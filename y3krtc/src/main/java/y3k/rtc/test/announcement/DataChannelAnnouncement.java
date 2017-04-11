package y3k.rtc.test.announcement;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import y3k.rtc.test.Y3kAppRtcRoom;
import y3k.rtc.test.channeldescription.DataChannelDescription;
import y3k.rtc.test.channeldescription.FileChannelDescription;
import y3k.rtc.test.channeldescription.FileStreamChannelDescription;

public class DataChannelAnnouncement {
    private final static String JSON_TAG_ANNOUNCEMENT_TYPE = "announcement_type";
    private final static String JSON_TAG_ANNOUNCEMENT_DESCRIPTION = "description";
    private final static String JSON_TAG_ANNOUNCEMENT_TIMESTAMP = "timestamp";
    private final DataChannelDescription channelDescription;
    private final AnnouncementType type;
    private final double timeStamp;
    private final Y3kAppRtcRoom room;

    public DataChannelAnnouncement(Y3kAppRtcRoom room, DataChannelDescription channelDescription) throws IllegalArgumentException {
        this.room = room;
        this.channelDescription = channelDescription;
        if (this.channelDescription instanceof FileChannelDescription) {
            this.type = AnnouncementType.File;
        } else if(this.channelDescription instanceof FileStreamChannelDescription){
            this.type = AnnouncementType.FileStream;
        } else{
            throw new IllegalArgumentException("Illegal Description Type!!!");
        }
        this.timeStamp = new Date().getTime();
    }

    public DataChannelAnnouncement(Y3kAppRtcRoom room,JSONObject jsonObject) throws JSONException, IllegalArgumentException {
        this.room = room;
        this.type = AnnouncementType.valueOf(jsonObject.getString(JSON_TAG_ANNOUNCEMENT_TYPE));
        this.timeStamp = jsonObject.getLong(JSON_TAG_ANNOUNCEMENT_TIMESTAMP);
        switch (this.type) {
            case File:
                this.channelDescription = new FileChannelDescription(jsonObject.getJSONObject(JSON_TAG_ANNOUNCEMENT_DESCRIPTION));
                break;
            case FileStream:
                this.channelDescription = new FileStreamChannelDescription(jsonObject.getJSONObject(JSON_TAG_ANNOUNCEMENT_DESCRIPTION));
                break;
            default:
                throw new IllegalArgumentException("Illegal Description Type!!!");
        }
    }

    public JSONObject toJSONObject() throws JSONException {
        return new JSONObject()
                .put(JSON_TAG_ANNOUNCEMENT_TYPE, this.type.name())
                .put(JSON_TAG_ANNOUNCEMENT_TIMESTAMP, this.timeStamp)
                .put(JSON_TAG_ANNOUNCEMENT_DESCRIPTION, this.channelDescription.toJSONObject());
    }

    public AnnouncementType getType() {
        return this.type;
    }

    public DataChannelDescription getChannelDescription() {
        return this.channelDescription;
    }

    public enum AnnouncementType {
        File, FileStream, Event
    }

    public double getTimeStamp() {
        return this.timeStamp;
    }

    public final void accept(){
        this.room.onAnnouncementAccepted(this);
    }

    public final void decline(){
        this.room.onAnnouncementDeclined(this);
    }
}
