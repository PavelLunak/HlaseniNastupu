package cz.stodva.hlaseninastupu.utils;

import android.content.Context;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;

import java.text.SimpleDateFormat;

public class AppUtils implements AppConstants {

    public static SimpleDateFormat sdf = new SimpleDateFormat("d.MM.yyyy  k:mm");

    public static String timeToString(long time, int reportPhase) {
        if (time == NONE) return "NONE";

        if (time == WAITING) {
            if (reportPhase == REPORT_PHASE_SEND) return "Čekání na odeslání hlášení...";
            if (reportPhase == REPORT_PHASE_DELIVERY) return "Čekání na potvrzení doručení hlášení...";
        }

        if (time == UNSUCCESFUL) return "Chyba...";
        if (time == CANCELED) return "Zrušeno...";

        return sdf.format(time);
    }

    public static void vibrate(Context context) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            v.vibrate(VibrationEffect.createOneShot(150, VibrationEffect.DEFAULT_AMPLITUDE));
        } else {
            v.vibrate(150);
        }
    }

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
