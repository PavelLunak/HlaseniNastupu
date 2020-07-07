package cz.stodva.hlaseninastupu.objects;

public class Report {

    private int id;
    private int messageType;
    private long time;
    private long sendingTime;
    private long deliveryTime;
    private boolean isAlarm;


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

    public long getSendingTime() {
        return sendingTime;
    }

    public void setSentTime(long sendingTime) {
        this.sendingTime = sendingTime;
    }

    public long getDeliveryTime() {
        return deliveryTime;
    }

    public void setDeliveryTime(long deliveryTime) {
        this.deliveryTime = deliveryTime;
    }

    public boolean isAlarm() {
        return isAlarm;
    }

    public void setAlarm(boolean alarm) {
        isAlarm = alarm;
    }
}
