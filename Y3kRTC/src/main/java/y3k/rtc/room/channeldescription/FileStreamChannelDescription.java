package y3k.rtc.room.channeldescription;

import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.InputStream;
import java.util.UUID;

public class FileStreamChannelDescription extends DataChannelDescription {
    private final static String JSON_TAG_FILENAME = "stream_file_name";
    private final static String JSON_TAG_FILEPATH = "stream_file_path";
    private final static String JSON_TAG_FILELENGTH = "stream_file_length";

    private final String fileName, filePath;
    @Nullable
    private final InputStream fileStream;
    private final long fileLength;
    @Nullable
    private final SendProgressCallback progressCallback;

    public interface SendProgressCallback {
        void onStart();

        void onSentBytes(long sentBytes);

        void onFinished(Exception exception);
    }

    public FileStreamChannelDescription(UUID uuid, String fileName, String filePath, @Nullable InputStream fileStream, long fileLength, @Nullable SendProgressCallback progressCallback) {
        super(uuid);
        this.progressCallback = progressCallback;
        this.fileStream = fileStream;
        this.fileName = fileName;
        this.filePath = filePath;
        this.fileLength = fileLength;
    }

    public FileStreamChannelDescription(JSONObject jsonObject) throws JSONException {
        super(jsonObject);
        this.fileStream = null;
        this.progressCallback = null;
        this.fileName = jsonObject.getString(JSON_TAG_FILENAME);
        this.filePath = jsonObject.getString(JSON_TAG_FILEPATH);
        this.fileLength = jsonObject.getLong(JSON_TAG_FILELENGTH);
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        return super.toJSONObject()
                .put(JSON_TAG_FILENAME, this.fileName)
                .put(JSON_TAG_FILEPATH, this.filePath)
                .put(JSON_TAG_FILELENGTH, this.fileLength);
    }

    public String getFilePath() {
        return this.filePath;
    }

    public String getFileName() {
        return this.fileName;
    }

    @Nullable
    public InputStream getFileStream() {
        return this.fileStream;
    }

    @Nullable
    public SendProgressCallback getProgressCallback() {
        return this.progressCallback;
    }

    public long getFileLength() {
        return this.fileLength;
    }
}
