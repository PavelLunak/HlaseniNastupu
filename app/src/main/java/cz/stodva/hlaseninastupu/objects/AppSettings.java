package cz.stodva.hlaseninastupu.objects;

public class AppSettings {
    private String sap;
    private String phoneNumber;
    private String startMessage;
    private String endMessage;

    public String getSap() {
        return sap;
    }

    public void setSap(String sap) {
        this.sap = sap;
    }

    public String getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(String phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

    public String getStartMessage() {
        return startMessage;
    }

    public void setStartMessage(String startMessage) {
        this.startMessage = startMessage;
    }

    public String getEndMessage() {
        return endMessage;
    }

    public void setEndMessage(String endMessage) {
        this.endMessage = endMessage;
    }
}
