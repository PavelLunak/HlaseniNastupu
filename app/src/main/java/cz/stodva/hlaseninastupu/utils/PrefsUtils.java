package cz.stodva.hlaseninastupu.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


import java.text.SimpleDateFormat;

import cz.stodva.hlaseninastupu.objects.AppSettings;


public class PrefsUtils implements AppConstants {

    //Pouze kvůli testování a výpisu do logu
    public static SimpleDateFormat sdf = new SimpleDateFormat("d.MM. yyyy  k:mm");

    public static String timeToString(long time) {
        return sdf.format(time);
    }

    public static void saveTimer(Context context, int messageType, long time) {
        Log.d(LOG_TAG_SMS, "PrefsUtils - saveTimer(" + messageType + ", " + timeToString(time) + ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        if (messageType == MESSAGE_TYPE_START) {
            editor.putLong("timer_start", time);
        } else if (messageType == MESSAGE_TYPE_END) {
            editor.putLong("timer_end", time);
        }

        editor.commit();
    }

    public static long getTimer(Context context, int messageType) {
        Log.d(LOG_TAG_SMS, "PrefsUtils - getTimer()");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);

        if (messageType == MESSAGE_TYPE_START) {
            Log.d(LOG_TAG_SMS, "time: " + timeToString(sp.getLong("timer_start", -1)) + ")");
            return sp.getLong("timer_start", -1);
        } else if (messageType == MESSAGE_TYPE_END) {
            Log.d(LOG_TAG_SMS, "time: " + timeToString(sp.getLong("timer_start", -1)) + ")");
            return sp.getLong("timer_end", -1);
        }

        return 0;
    }

    public static void setIsTimer(Context context, int messageType, boolean isSet) {
        Log.d(LOG_TAG_SMS, "PrefsUtils - setIsTimer(" + messageType + ", " + isSet + ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        if (messageType == MESSAGE_TYPE_START) {
            editor.putBoolean("is_timer_start", isSet);
        } else {
            editor.putBoolean("is_timer_end", isSet);
        }

        editor.commit();
    }

    public static boolean isTimerSet(Context context,  int messageType) {
        Log.d(LOG_TAG_SMS, "PrefsUtils - isTimerSet(" + messageType + ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);

        if (messageType == MESSAGE_TYPE_START) {
            Log.d(LOG_TAG_SMS, "is set: " + sp.getBoolean("is_timer_start", false));
            return sp.getBoolean("is_timer_start", false);
        } else {
            Log.d(LOG_TAG_SMS, "is set: " + sp.getBoolean("is_timer_start", false));
            return sp.getBoolean("is_timer_end", false);
        }
    }

    public static void setLastReportTime(Context context, long time, int messageType) {
        Log.d(LOG_TAG_SMS, "PrefsUtils - setLastReportTime(" + messageType + ", " + timeToString(time) + ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        if (messageType == MESSAGE_TYPE_START) {
            editor.putLong("last_report_start", time);
        } else {
            editor.putLong("last_report_end", time);
        }

        editor.commit();
    }

    public static long getLastTimer(Context context,  int messageType) {
        Log.d(LOG_TAG_SMS, "PrefsUtils - getLastTimer(" + messageType + ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);

        if (messageType == MESSAGE_TYPE_START) {
            Log.d(LOG_TAG_SMS, "last timer " + timeToString(sp.getLong("last_report_start", -1)));
            return sp.getLong("last_report_start", -1);
        } else {
            Log.d(LOG_TAG_SMS, "last timer " + timeToString(sp.getLong("last_report_start", -1)));
            return sp.getLong("last_report_end", -1);
        }
    }

    public static void saveAppSettings(Context context, String sap, String phone, String startString, String endString) {
        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        editor.putString("settings_sap", sap);
        editor.putString("settings_phone", phone);
        editor.putString("settings_start_text", startString);
        editor.putString("settings_end_text", endString);

        editor.commit();
    }

    public static AppSettings getAppSettings(Context context) {
        AppSettings appSettings = new AppSettings();

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);

        appSettings.setSap(sp.getString("settings_sap", ""));
        appSettings.setPhoneNumber(sp.getString("settings_phone", PHONE_NUMBER));
        appSettings.setStartMessage(sp.getString("settings_start_text", ""));
        appSettings.setEndMessage(sp.getString("settings_end_text", ""));

        return appSettings;
    }

    // ---------- PŘÍZNAK ODESLÁNÍ HLÁŠENÍ ---------------------------------------------------------

    public static void saveIsReportSent(Context context, boolean sent, int messageType) {
        Log.d(LOG_TAG_SMS, "PrefsUtils - saveIsReportSent(" + messageType + ", " + sent + ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        if (messageType == MESSAGE_TYPE_START) {
            editor.putBoolean("msg_start_sent", sent);
        } else if (messageType == MESSAGE_TYPE_END) {
            editor.putBoolean("msg_end_sent", sent);
        } else {
            editor.putBoolean("msg_start_sent", sent);
            editor.putBoolean("msg_end_sent", sent);
        }

        editor.commit();
    }

    public static boolean isReportSent(Context context, int messageType) {
        Log.d(LOG_TAG_SMS, "PrefsUtils - isReportSent(" + messageType + ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);

        if (messageType == MESSAGE_TYPE_START) {
            Log.d(LOG_TAG_SMS, "is sent : " + sp.getBoolean("msg_start_sent", false));
            return sp.getBoolean("msg_start_sent", false);
        } else if (messageType == MESSAGE_TYPE_END) {
            Log.d(LOG_TAG_SMS, "is sent : " + sp.getBoolean("msg_start_sent", false));
            return sp.getBoolean("msg_end_sent", false);
        }

        return true;
    }

    // ---------- PŘÍZNAK DORUČENÍ HLÁŠENÍ ---------------------------------------------------------

    public static void saveIsReportDelivered(Context context, boolean delivered, int messageType) {
        Log.d(LOG_TAG_SMS, "PrefsUtils - saveIsReportDelivered(" + messageType + ", " + delivered + ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        if (messageType == MESSAGE_TYPE_START) {
            editor.putBoolean("msg_start_delivered", delivered);
        } else if (messageType == MESSAGE_TYPE_END){
            editor.putBoolean("msg_end_delivered", delivered);
        } else {
            editor.putBoolean("msg_start_delivered", delivered);
            editor.putBoolean("msg_end_delivered", delivered);
        }

        editor.commit();
    }

    public static boolean isReportDelivered(Context context, int messageType) {
        Log.d(LOG_TAG_SMS, "PrefsUtils - isReportDelivered(" + messageType + ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);

        if (messageType == MESSAGE_TYPE_START) {
            Log.d(LOG_TAG_SMS, "is delivered : " + sp.getBoolean("msg_start_delivered", false));
            return sp.getBoolean("msg_start_delivered", false);
        } else if (messageType == MESSAGE_TYPE_END) {
            Log.d(LOG_TAG_SMS, "is delivered : " + sp.getBoolean("msg_start_delivered", false));
            return sp.getBoolean("msg_end_delivered", false);
        }

        return true;
    }

    // ---------- PŘÍZNAK DEFINITIVNÍHO ODMÍTNUTÍ OPRÁVNĚNÍ ----------------------------------------

    public static void setDefinitiveRejection(Context context, boolean isDenied) {
        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("definitive_rejection", isDenied);
        editor.commit();
    }

    public static boolean isDefinitiveRejection(Context context) {
        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        return sp.getBoolean("definitive_rejection", false);
    }

    // ---------- PŘÍZNAK NASTAVENÉHO ALARMU PŘI NEODESLÁNÍ HLÁŠENÍ --------------------------------

    public static void setNoSentAlarm(Context context, boolean isAlarm, int messageType) {
        Log.d(LOG_TAG_SMS, "PrefsUtils - setNoSentAlarm(" + messageType + ", " + isAlarm + ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        if (messageType == MESSAGE_TYPE_START) {
            editor.putBoolean("no_sent_start_alarm", isAlarm);
        } else if (messageType == MESSAGE_TYPE_END){
            editor.putBoolean("no_sent_end_alarm", isAlarm);
        } else {
            editor.putBoolean("no_sent_start_alarm", isAlarm);
            editor.putBoolean("no_sent_end_alarm", isAlarm);
        }

        editor.commit();
    }

    public static boolean isNoSentAlarm(Context context, int messageType) {
        Log.d(LOG_TAG_SMS, "PrefsUtils - isNoSentAlarm(" + messageType + ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);

        if (messageType == MESSAGE_TYPE_START) {
            Log.d(LOG_TAG_SMS, "is no sent alarm : " + sp.getBoolean("no_sent_start_alarm", false));
            return sp.getBoolean("no_sent_start_alarm", false);
        } else if (messageType == MESSAGE_TYPE_END) {
            Log.d(LOG_TAG_SMS, "is no sent alarm : " + sp.getBoolean("no_sent_start_alarm", false));
            return sp.getBoolean("no_sent_end_alarm", false);
        }

        return false;
    }

    // ---------- PŘÍZNAK NASTAVENÉHO ALARMU PŘI NEDORUČENÍ HLÁŠENÍ --------------------------------

    public static void setNoDeliveredAlarm(Context context, boolean isAlarm, int messageType) {
        Log.d(LOG_TAG_SMS, "PrefsUtils - setNoDeliveredAlarm(" + messageType + ", " + isAlarm + ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        if (messageType == MESSAGE_TYPE_START) {
            editor.putBoolean("no_delivered_start_alarm", isAlarm);
        } else if (messageType == MESSAGE_TYPE_END){
            editor.putBoolean("no_delivered_end_alarm", isAlarm);
        } else {
            editor.putBoolean("no_delivered_start_alarm", isAlarm);
            editor.putBoolean("no_delivered_end_alarm", isAlarm);
        }

        editor.commit();
    }

    public static boolean isNoDeliveredAlarm(Context context, int messageType) {
        Log.d(LOG_TAG_SMS, "PrefsUtils - isNoDeliveredAlarm(" + messageType + ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);

        if (messageType == MESSAGE_TYPE_START) {
            Log.d(LOG_TAG_SMS, "is no delivered alarm : " + sp.getBoolean("no_delivered_start_alarm", false));
            return sp.getBoolean("no_delivered_start_alarm", false);
        } else if (messageType == MESSAGE_TYPE_END) {
            Log.d(LOG_TAG_SMS, "is no delivered alarm : " + sp.getBoolean("no_delivered_start_alarm", false));
            return sp.getBoolean("no_delivered_end_alarm", false);
        }

        return false;

    }
}
