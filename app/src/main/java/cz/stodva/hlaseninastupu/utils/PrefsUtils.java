package cz.stodva.hlaseninastupu.utils;

import android.content.Context;
import android.content.SharedPreferences;

import cz.stodva.hlaseninastupu.objects.Settings;

public class PrefsUtils {

    public static void setIsTimer(Context context, int messageType, boolean isSet) {
        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        if (messageType == AppConstants.MESSAGE_TYPE_START) {
            editor.putBoolean("timer_start", isSet);
        } else {
            editor.putBoolean("timer_end", isSet);
        }

        editor.commit();
    }

    public static boolean isTimerSet(Context context,  int messageType) {
        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);

        if (messageType == AppConstants.MESSAGE_TYPE_START) {
            return sp.getBoolean("timer_start", false);
        } else {
            return sp.getBoolean("timer_end", false);
        }
    }

    public static void setLastReportTime(Context context, long time, int messageType) {
        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        if (messageType == AppConstants.MESSAGE_TYPE_START) {
            editor.putLong("last_report_start", time);
        } else {
            editor.putLong("last_report_end", time);
        }

        editor.commit();
    }

    public static long getLastTimer(Context context,  int messageType) {
        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);

        if (messageType == AppConstants.MESSAGE_TYPE_START) {
            return sp.getLong("last_report_start", -1);
        } else {
            return sp.getLong("last_report_end", -1);
        }
    }

    public static void saveSettings(Context context, String sap, String phone, String startString, String endString) {
        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        editor.putString("settings_sap", sap);
        editor.putString("settings_phone", phone);
        editor.putString("settings_start_text", startString);
        editor.putString("settings_end_text", endString);

        editor.commit();
    }

    public static Settings getSettings(Context context) {
        Settings settings = new Settings();

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);

        settings.setSap(sp.getString("settings_sap", ""));
        settings.setPhoneNumber(sp.getString("settings_phone", AppConstants.PHONE_NUMBER));
        settings.setStartMessage(sp.getString("settings_start_text", ""));
        settings.setEndMessage(sp.getString("settings_end_text", ""));

        return settings;
    }

    public static void saveMsgStartSent(Context context, boolean sent) {
        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("msg_start_sent", sent);
        editor.commit();
    }

    public static boolean isMsgStartSent(Context context) {
        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        return sp.getBoolean("msg_start_sent", false);
    }

    public static void saveMsgEndSent(Context context, boolean sent) {
        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("msg_end_sent", sent);
        editor.commit();
    }

    public static boolean isMsgEndSent(Context context) {
        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        return sp.getBoolean("msg_end_sent", false);
    }

    public static void saveMsgStartDelivered(Context context, boolean delivered) {
        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("msg_start_delivered", delivered);
        editor.commit();
    }

    public static boolean isMsgStartDelivered(Context context) {
        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        return sp.getBoolean("msg_start_delivered", false);
    }

    public static void saveMsgEndDelivered(Context context, boolean delivered) {
        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();
        editor.putBoolean("msg_end_delivered", delivered);
        editor.commit();
    }

    public static boolean isMsgEndDeliveredt(Context context) {
        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        return sp.getBoolean("msg_end_delivered", false);
    }
}
