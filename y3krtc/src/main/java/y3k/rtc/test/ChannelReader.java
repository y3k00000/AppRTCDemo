package y3k.rtc.test;

import android.util.Log;

import org.webrtc.DataChannel;

import java.io.IOException;
import java.io.OutputStream;

public abstract class ChannelReader<T> implements DataChannel.Observer {
    private final DataChannel rtcDataChannel;
    private final OutputStream targetOutputStream;
    private Callback<T> callback;

    public ChannelReader(DataChannel dataChannel) {
        this.rtcDataChannel = dataChannel;
        this.rtcDataChannel.registerObserver(this);
        this.targetOutputStream = this.onCreateOutStream();
    }

    public ChannelReader withCallback(Callback<T> callback) {
        this.callback = callback;
        return this;
    }

    @Override
    public void onBufferedAmountChange(long l) {

    }

    @Override
    public void onStateChange() {
        Log.d(this.getClass().getSimpleName(), "onStateChange(" + this.rtcDataChannel.state() + ")");
        if (this.rtcDataChannel.state() == DataChannel.State.CLOSED) {
            T result = this.onChannelClosed();
            if (this.callback != null) {
                this.callback.onFinished(null, result);
            }
        }
    }

    @Override
    public void onMessage(DataChannel.Buffer buffer) {
        try {
            byte[] receivedBytes = new byte[buffer.data.remaining()];
            buffer.data.get(receivedBytes);
            this.targetOutputStream.write(receivedBytes);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    abstract OutputStream onCreateOutStream();
    abstract T onChannelClosed();

    public interface Callback<T> {
        void onFinished(Exception e, T result);
    }
}
