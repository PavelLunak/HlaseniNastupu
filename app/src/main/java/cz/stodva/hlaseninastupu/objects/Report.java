package cz.stodva.hlaseninastupu.objects;

import android.os.Parcel;
import android.os.Parcelable;


public class Report implements Parcelable {

    private int id;
    private int messageType;
    private long time;
    private long sentTime;
    private long deliveryTime;
    private int alarmRequestCode;
    private int requestCodeForErrorAlarm;
    private boolean isErrorAlert;
    private String message;


    public Report() {}

    @Override
    public String toString() {
        return "Report{" +
                "id=" + id +
                ", messageType=" + messageType +
                ", time=" + time +
                ", sentTime=" + sentTime +
                ", deliveryTime=" + deliveryTime +
                ", alarmRequestCode=" + alarmRequestCode +
                ", requestCodeForErrorAlarm=" + requestCodeForErrorAlarm +
                ", isErrorAlert=" + isErrorAlert +
                ", message='" + message + '\'' +
                '}';
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMessageType() {
        return messageType;
    }

    public void setMessageType(int messageType) {
        this.messageType = messageType;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    public long getSentTime() {
        return sentTime;
    }

    public void setSentTime(long sendingTime) {
        this.sentTime = sendingTime;
    }

    public long getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(long deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public int getAlarmRequestCode() {
        return alarmRequestCode;
    }

    public void setAlarmRequestCode(int alarmRequestCode) {
        this.alarmRequestCode = alarmRequestCode;
    }

    public int getRequestCodeForErrorAlarm() {
        return requestCodeForErrorAlarm;
    }

    public void setRequestCodeForErrorAlarm(int requestCodeForErrorAlarm) {
        this.requestCodeForErrorAlarm = requestCodeForErrorAlarm;
    }

    public boolean isErrorAlert() {
        return isErrorAlert;
    }

    public void setErrorAlert(boolean errorAlert) {
        isErrorAlert = errorAlert;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(this.id);
        dest.writeInt(this.messageType);
        dest.writeLong(this.time);
        dest.writeLong(this.sentTime);
        dest.writeLong(this.deliveryTime);
        dest.writeInt(this.alarmRequestCode);
        dest.writeInt(this.requestCodeForErrorAlarm);
        dest.writeByte(this.isErrorAlert ? (byte) 1 : (byte) 0);
        dest.writeString(this.message);
    }

    protected Report(Parcel in) {
        this.id = in.readInt();
        this.messageType = in.readInt();
        this.time = in.readLong();
        this.sentTime = in.readLong();
        this.deliveryTime = in.readLong();
        this.alarmRequestCode = in.readInt();
        this.requestCodeForErrorAlarm = in.readInt();
        this.isErrorAlert = in.readByte() != 0;
        this.message = in.readString();
    }

    public static final Parcelable.Creator<Report> CREATOR = new Parcelable.Creator<Report>() {
        @Override
        public Report createFromParcel(Parcel source) {
            return new Report(source);
        }

        @Override
        public Report[] newArray(int size) {
            return new Report[size];
        }
    };
}
