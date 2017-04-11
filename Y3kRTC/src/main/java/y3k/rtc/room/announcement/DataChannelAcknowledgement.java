package y3k.rtc.room.announcement;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.UUID;

public class DataChannelAcknowledgement {
    private final static String JSON_TAG_ACKNOWLEDGEMENT_UUID = "channel_uuid";
    private final static String JSON_TAG_ACKNOWLEDGEMENT_REPLY = "acknowledgement_reply";
    private final UUID channelUUID;
    private final Reply reply;

    public DataChannelAcknowledgement(DataChannelAnnouncement announcementToReply, Reply reply) {
        this.channelUUID = announcementToReply.getChannelDescription().getUUID();
        this.reply = reply;
    }

    public DataChannelAcknowledgement(JSONObject jsonObject) throws JSONException, IllegalArgumentException {
        this.channelUUID = UUID.fromString(jsonObject.getString(JSON_TAG_ACKNOWLEDGEMENT_UUID));
        this.reply = Reply.valueOf(jsonObject.getString(JSON_TAG_ACKNOWLEDGEMENT_REPLY));
    }

    public JSONObject toJSONObject() throws JSONException {
        return new JSONObject()
                .put(JSON_TAG_ACKNOWLEDGEMENT_REPLY, this.reply)
                .put(JSON_TAG_ACKNOWLEDGEMENT_UUID, this.channelUUID.toString());
    }

    public Reply getReply() {
        return this.reply;
    }

    public UUID getChannelUUID() {
        return this.channelUUID;
    }

    public enum Reply {
        NOTED, REFUSE
    }
}
