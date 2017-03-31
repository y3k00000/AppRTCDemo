package y3k.rtc.test;

import android.util.Log;

import org.webrtc.DataChannel;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;

public abstract class ChannelReader<T> {
    private final DataChannel rtcDataChannel;
    private final ArrayList<byte[]> bufferedMessages = new ArrayList<>();
    private OutputStream targetOutputStream;
    private Callback<T> callback;

    private final DataChannel.Observer channelObserver = new DataChannel.Observer() {
        @Override
        public void onBufferedAmountChange(long l) {

        }

        @Override
        public void onStateChange() {
            Log.d(this.getClass().getSimpleName(), "onStateChange(" + ChannelReader.this.rtcDataChannel.state() + ")");
            if (ChannelReader.this.rtcDataChannel.state() == DataChannel.State.CLOSED) {
                ChannelReader.this.rtcDataChannel.unregisterObserver();
                T result = ChannelReader.this.onChannelClosed();
                if (ChannelReader.this.callback != null) {
                    ChannelReader.this.callback.onFinished(null, result);
                }
            }
        }

        @Override
        public void onMessage(DataChannel.Buffer buffer) {
            Log.d(this.getClass().getSimpleName(), "onMessage(" + buffer.data.remaining() + ")");
            byte[] receivedBytes = new byte[buffer.data.remaining()];
            buffer.data.get(receivedBytes);
            if (ChannelReader.this.targetOutputStream == null && (ChannelReader.this.targetOutputStream = ChannelReader.this.onCreateOutStream()) == null) {
                bufferedMessages.add(receivedBytes);
                return;
            } else if (bufferedMessages.size() != 0) {
                for (byte[] bufferedBytes : new ArrayList<>(bufferedMessages)) {
                    try {
                        ChannelReader.this.targetOutputStream.write(bufferedBytes);
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    ChannelReader.this.bufferedMessages.remove(bufferedBytes);
                }
            }
            try {
                ChannelReader.this.targetOutputStream.write(receivedBytes);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    };

    public ChannelReader(DataChannel dataChannel) {
        this.rtcDataChannel = dataChannel;
    }

    public ChannelReader withCallback(Callback<T> callback) {
        this.callback = callback;
        return this;
    }

    public void start() {
        this.rtcDataChannel.registerObserver(this.channelObserver);
    }

    protected abstract OutputStream onCreateOutStream();

    protected abstract T onChannelClosed();

    public interface Callback<T> {
        void onFinished(Exception e, T result);
    }
}
