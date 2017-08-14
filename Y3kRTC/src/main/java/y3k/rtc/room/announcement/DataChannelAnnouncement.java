package y3k.rtc.room.announcement;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Date;

import y3k.rtc.room.channeldescription.DataChannelDescription;
import y3k.rtc.room.channeldescription.FileChannelDescription;
import y3k.rtc.room.channeldescription.FileStreamChannelDescription;

public class DataChannelAnnouncement {
    private final static String JSON_TAG_ANNOUNCEMENT_TYPE = "announcement_type";
    private final static String JSON_TAG_ANNOUNCEMENT_DESCRIPTION = "description";
    private final static String JSON_TAG_ANNOUNCEMENT_TIMESTAMP = "timestamp";
    private final DataChannelDescription channelDescription;
    private final AnnouncementType type;
    private final double timeStamp;
    private final Callback callback;

    public interface Callback{
        void onAnnouncementAccepted(DataChannelAnnouncement announcement);
        void onAnnouncementDeclined(DataChannelAnnouncement announcement);
    }

    public DataChannelAnnouncement(Callback callback, DataChannelDescription channelDescription) throws IllegalArgumentException {
        this.callback = callback;
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

    public DataChannelAnnouncement(Callback callback,JSONObject jsonObject) throws JSONException, IllegalArgumentException {
        this.callback = callback;
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
        this.callback.onAnnouncementAccepted(this);
    }

    public final void decline(){
        this.callback.onAnnouncementDeclined(this);
    }
}
