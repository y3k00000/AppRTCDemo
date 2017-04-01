package y3k.rtc.test.channeldescription;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class FileChannelDescription extends DataChannelDescription {
    private final static String JSON_TAG_FILENAME = "file_name";
    private final static String JSON_TAG_FILELENGTH = "file_length";

    private final String fileName;
    private final long fileLength;

    public FileChannelDescription(UUID uuid, String fileName, long fileLength) {
        super(uuid);
        this.fileName = fileName;
        this.fileLength = fileLength;
    }

    public FileChannelDescription(JSONObject jsonObject) throws JSONException {
        super(jsonObject);
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

    public long getFileLength() {
        return this.fileLength;
    }
}
