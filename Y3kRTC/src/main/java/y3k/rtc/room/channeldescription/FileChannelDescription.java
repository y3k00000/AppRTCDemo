package y3k.rtc.room.channeldescription;

import android.support.annotation.Nullable;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.UUID;

public class FileChannelDescription extends DataChannelDescription {
    private final static String JSON_TAG_FILENAME = "file_name";
    private final static String JSON_TAG_FILELENGTH = "file_length";

    private final String fileName;
    @Nullable
    private final File localFile;
    private final long fileLength;

    public FileChannelDescription(UUID uuid, String fileName, @Nullable File localFile, long fileLength) {
        super(uuid);
        this.localFile = localFile;
        this.fileName = fileName;
        this.fileLength = fileLength;
    }

    public FileChannelDescription(JSONObject jsonObject) throws JSONException {
        super(jsonObject);
        this.localFile = null;
        this.fileName = jsonObject.getString(JSON_TAG_FILENAME);
        this.fileLength = jsonObject.getLong(JSON_TAG_FILELENGTH);
    }

    @Override
    public JSONObject toJSONObject() throws JSONException {
        return super.toJSONObject()
                .put(JSON_TAG_FILENAME, this.fileName)
                .put(JSON_TAG_FILELENGTH, this.fileLength);
    }

    public String getFileName() {
        return this.fileName;
    }

    @Nullable
    public File getLocalFile() {
        return this.localFile;
    }

    public long getFileLength() {
        return this.fileLength;
    }
}
