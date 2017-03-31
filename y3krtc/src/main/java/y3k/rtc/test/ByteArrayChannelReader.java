package y3k.rtc.test;

import org.webrtc.DataChannel;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;

public class ByteArrayChannelReader extends ChannelReader<byte[]> {
    private ByteArrayOutputStream byteArrayOutputStream;

    public ByteArrayChannelReader(DataChannel dataChannel) {
        super(dataChannel);
    }

    @Override
    OutputStream onCreateOutStream() {
        return this.byteArrayOutputStream = new ByteArrayOutputStream();
    }

    @Override
    byte[] onChannelClosed() {
        return this.byteArrayOutputStream.toByteArray();
    }
}