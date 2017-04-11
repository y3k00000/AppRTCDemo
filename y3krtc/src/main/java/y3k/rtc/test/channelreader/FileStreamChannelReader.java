package y3k.rtc.test.channelreader;

import org.webrtc.DataChannel;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class FileStreamChannelReader extends ChannelReader<OutputStream> {
    final OutputStream outputStream;

    public FileStreamChannelReader(DataChannel dataChannel, OutputStream outputStream) {
        super(dataChannel);
        this.outputStream = outputStream;
    }

    @Override
    protected OutputStream onCreateOutStream() {
        return this.outputStream;
    }

    @Override
    protected OutputStream onChannelClosed() {
        return this.outputStream;
    }
}
