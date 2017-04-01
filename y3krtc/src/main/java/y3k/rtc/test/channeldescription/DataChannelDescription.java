package y3k.rtc.test.channeldescription;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class DataChannelDescription {
    private final static String JSON_TAG_UUID = "uuid";
    private final UUID uuid;

    public DataChannelDescription(UUID uuid) {
        this.uuid = uuid;
    }

    public DataChannelDescription(JSONObject jsonObject) throws JSONException {
        this.uuid = UUID.fromString(jsonObject.getString(JSON_TAG_UUID));
    }

    public JSONObject toJSONObject() throws JSONException {
        return new JSONObject().put(JSON_TAG_UUID, this.uuid.toString());
    }

    public UUID getUUID() {
        return this.uuid;
    }
}
