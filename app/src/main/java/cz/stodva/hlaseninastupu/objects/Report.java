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
    private boolean isFailed;
    private boolean isDelivered;
    private boolean isAutomat;
    private String desc;


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
                ", isFailed=" + isFailed +
                ", isDelivered=" + isDelivered +
                ", isAutomat=" + isAutomat +
                ", desc='" + desc + '\'' +
                '}';
    }

    public void copyTo(Report targetReport) {
        if (targetReport == null) return;

        targetReport.setId(this.id);
        targetReport.setMessageType(this.messageType);
        targetReport.setTime(this.time);
        targetReport.setSentTime(this.sentTime);
        targetReport.setDeliveryTime(this.deliveryTime);
        targetReport.setAlarmRequestCode(this.alarmRequestCode);
        targetReport.setRequestCodeForErrorAlarm(this.requestCodeForErrorAlarm);
        targetReport.setErrorAlert(this.isErrorAlert);
        targetReport.setMessage(this.message);
        targetReport.setFailed(this.isFailed);
        targetReport.setDelivered(this.isDelivered);
        targetReport.setAutomat(this.isAutomat);
        targetReport.setDesc(this.desc);
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

    public boolean isFailed() {
        return isFailed;
    }

    public void setFailed(boolean failed) {
        isFailed = failed;
    }

    public boolean isDelivered() {
        return isDelivered;
    }

    public void setDelivered(boolean delivered) {
        isDelivered = delivered;
    }

    public boolean isAutomat() {
        return isAutomat;
    }

    public void setAutomat(boolean automat) {
        isAutomat = automat;
    }

    public String getDesc() {
        return desc;
    }

    public void setDesc(String desc) {
        this.desc = desc;
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
        dest.writeByte(this.isFailed ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isDelivered ? (byte) 1 : (byte) 0);
        dest.writeByte(this.isAutomat ? (byte) 1 : (byte) 0);
        dest.writeString(this.desc);
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
        this.isFailed = in.readByte() != 0;
        this.isDelivered = in.readByte() != 0;
        this.isAutomat = in.readByte() != 0;
        this.desc = in.readString();
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
