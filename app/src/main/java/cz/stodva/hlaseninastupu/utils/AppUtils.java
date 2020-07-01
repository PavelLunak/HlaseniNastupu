package cz.stodva.hlaseninastupu.utils;

public class AppUtils implements AppConstants {

    public static String messageTypeToString(int messageType) {
        switch (messageType) {
            case MESSAGE_TYPE_START:
                return "MESSAGE_TYPE_START";
            case MESSAGE_TYPE_END:
                return "MESSAGE_TYPE_END";
            case MESSAGE_TYPE_BOTH:
                return "MESSAGE_TYPE_BOTH";
            default:
                return "UNKNOWN";
        }
    }

    public static String reportTypeToString(int messageType) {
        switch (messageType) {
            case REPORT_TYPE_LAST:
                return "REPORT_TYPE_LAST";
            case REPORT_TYPE_NEXT:
                return "REPORT_TYPE_NEXT";
            default:
                return "UNKNOWN";
        }
    }

    public static String errorTypeToString(int errorType) {
        switch (errorType) {
            case ERROR_TYPE_NO_SENT:
                return "ERROR_TYPE_NO_SENT";
            case ERROR_TYPE_NO_DELIVERED:
                return "ERROR_TYPE_NO_DELIVERED";
            default:
                return "UNKNOWN";
        }
    }

    public static String alarmTypeToString(int alarmType) {
        switch (alarmType) {
            case ALARM_TYPE_NO_SENT:
                return "ALARM_TYPE_NO_SENT";
            case ALARM_TYPE_NO_DELIVERED:
                return "ALARM_TYPE_NO_DELIVERED";
            default:
                return "UNKNOWN";
        }
    }
}
