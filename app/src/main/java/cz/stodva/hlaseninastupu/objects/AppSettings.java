package cz.stodva.hlaseninastupu.objects;

public class AppSettings {
    private String sap;
    private String phoneNumber;
    private String contactName;
    private String startMessage;
    private String endMessage;

    private boolean showOnlyActiveReports;
    private boolean showItemDetails;

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

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
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

    public boolean isShowOnlyActiveReports() {
        return showOnlyActiveReports;
    }

    public void setShowOnlyActiveReports(boolean showOnlyActiveReports) {
        this.showOnlyActiveReports = showOnlyActiveReports;
    }

    public boolean isShowItemDetails() {
        return showItemDetails;
    }

    public void setShowItemDetails(boolean showItemDetails) {
        this.showItemDetails = showItemDetails;
    }
}
