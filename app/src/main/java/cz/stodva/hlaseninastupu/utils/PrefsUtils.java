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

    public static void setIsTimer(Context context, int messageType, boolean isSet) {
        Log.d(LOG_TAG_SMS, "(901) PrefsUtils - setIsTimer(" + AppUtils.messageTypeToString(messageType) + ", is set: " + isSet + ")");

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
        Log.d(LOG_TAG_SMS, "(902) PrefsUtils - isTimerSet(" + AppUtils.messageTypeToString(messageType) + ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);

        if (messageType == MESSAGE_TYPE_START) {
            Log.d(LOG_TAG_SMS, "(903) is set: " + sp.getBoolean("is_timer_start", false));
            return sp.getBoolean("is_timer_start", false);
        } else {
            Log.d(LOG_TAG_SMS, "(904) is set: " + sp.getBoolean("is_timer_start", false));
            return sp.getBoolean("is_timer_end", false);
        }
    }

    public static void setReportTime(Context context, long time, int messageType, int reportType) {
        Log.d(LOG_TAG_SMS, "(905) PrefsUtils - setReportTime(" +
                AppUtils.messageTypeToString(messageType) +
                ", time: " +
                timeToString(time) + ", " +
                AppUtils.reportTypeToString(reportType) +
                ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        if (messageType == MESSAGE_TYPE_START) {
            if (reportType == REPORT_TYPE_NEXT) {
                if (time == PREFS_DELETE_KEY) editor.remove("next_report_start");
                else editor.putLong("next_report_start", time);
            } else if (reportType == REPORT_TYPE_LAST) {
                if (time == PREFS_DELETE_KEY) editor.remove("last_report_start");
                else editor.putLong("last_report_start", time);
            }
        } else {
            if (reportType == REPORT_TYPE_NEXT) {
                if (time == PREFS_DELETE_KEY) editor.remove("next_report_end");
                else editor.putLong("next_report_end", time);
            } else if (reportType == REPORT_TYPE_LAST) {
                if (time == PREFS_DELETE_KEY) editor.remove("last_report_end");
                else editor.putLong("last_report_end", time);
            }
        }

        editor.commit();
    }

    public static long getReportTime(Context context,  int messageType, int reportType) {
        Log.d(LOG_TAG_SMS, "(906) PrefsUtils - getReportTime(" + AppUtils.messageTypeToString(messageType) + ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);

        if (messageType == MESSAGE_TYPE_START) {
            Log.d(LOG_TAG_SMS, "(907) messageType == MESSAGE_TYPE_START");

            if (reportType == REPORT_TYPE_NEXT) {
                Log.d(LOG_TAG_SMS, "(908) reportType == REPORT_TYPE_NEXT: " + sdf.format(sp.getLong("next_report_start", -1)));
                return sp.getLong("next_report_start", -1);
            } else {
                Log.d(LOG_TAG_SMS, "(909) reportType == REPORT_TYPE_LAST :" + sdf.format(sp.getLong("last_report_start", -1)));
                return sp.getLong("last_report_start", -1);
            }
        } else {
            Log.d(LOG_TAG_SMS, "(910) messageType == MESSAGE_TYPE_END");

            if (reportType == REPORT_TYPE_NEXT) {
                Log.d(LOG_TAG_SMS, "(911) reportType == REPORT_TYPE_NEXT: " + sdf.format(sp.getLong("next_report_end", -1)));
                return sp.getLong("next_report_end", -1);
            } else {
                Log.d(LOG_TAG_SMS, "(912) reportType == REPORT_TYPE_LAST :" + sdf.format(sp.getLong("last_report_end", -1)));
                return sp.getLong("last_report_end", -1);
            }
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

    public static void saveIsReportSent(Context context, boolean sent, int messageType, int reportType) {
        Log.d(LOG_TAG_SMS, "(913) PrefsUtils - saveIsReportSent(" + AppUtils.messageTypeToString(messageType) + ", is sent: " + sent + ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        if (messageType == MESSAGE_TYPE_START) {
            if (reportType == REPORT_TYPE_NEXT) editor.putBoolean("next_report_start_sent", sent);
            else if (reportType == REPORT_TYPE_LAST) editor.putBoolean("last_report_start_sent", sent);
        } else if (messageType == MESSAGE_TYPE_END) {
            if (reportType == REPORT_TYPE_NEXT) editor.putBoolean("next_report_end_sent", sent);
            else if (reportType == REPORT_TYPE_LAST) editor.putBoolean("last_report_end_sent", sent);
        } else {
            if (reportType == REPORT_TYPE_NEXT) {
                editor.putBoolean("next_report_start_sent", sent);
                editor.putBoolean("next_report_end_sent", sent);
            } else if (reportType == REPORT_TYPE_LAST) {
                editor.putBoolean("last_report_start_sent", sent);
                editor.putBoolean("last_report_end_sent", sent);
            }
        }

        editor.commit();
    }

    public static boolean isReportSent(Context context, int messageType, int reportType) {
        Log.d(LOG_TAG_SMS,
                "(914) PrefsUtils - isReportSent(" +
                        AppUtils.messageTypeToString(messageType) +
                        ", " +
                        AppUtils.reportTypeToString(reportType) +
                        ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);

        if (messageType == MESSAGE_TYPE_START) {
            Log.d(LOG_TAG_SMS, "(915) messageType == MESSAGE_TYPE_START");

            if (reportType == REPORT_TYPE_NEXT) {
                Log.d(LOG_TAG_SMS, "(916) reportType == REPORT_TYPE_NEXT");
                Log.d(LOG_TAG_SMS, "(917) is sent: " + sp.getBoolean("next_report_start_sent", false));
                return sp.getBoolean("next_report_start_sent", false);
            } else if (reportType == REPORT_TYPE_LAST) {
                Log.d(LOG_TAG_SMS, "(918) reportType == REPORT_TYPE_LAST");
                Log.d(LOG_TAG_SMS, "(919) is sent: " + sp.getBoolean("last_report_start_sent", false));
                return sp.getBoolean("last_report_start_sent", false);
            } else {
                Log.d(LOG_TAG_SMS, "(920) reportType == UNKNOWN");
            }
        } else if (messageType == MESSAGE_TYPE_END) {
            Log.d(LOG_TAG_SMS, "(921) messageType == MESSAGE_TYPE_END");

            if (reportType == REPORT_TYPE_NEXT) {
                Log.d(LOG_TAG_SMS, "(922) reportType == REPORT_TYPE_NEXT");
                Log.d(LOG_TAG_SMS, "(923) is sent: " + sp.getBoolean("next_report_end_sent", false));
                return sp.getBoolean("next_report_end_sent", false);
            } else if (reportType == REPORT_TYPE_LAST) {
                Log.d(LOG_TAG_SMS, "(924) reportType == REPORT_TYPE_LAST");
                Log.d(LOG_TAG_SMS, "(925) is sent: " + sp.getBoolean("last_report_end_sent", false));
                return sp.getBoolean("last_report_end_sent", false);
            }
        }

        return true;
    }

    // ---------- PŘÍZNAK DORUČENÍ HLÁŠENÍ ---------------------------------------------------------

    public static void saveIsReportDelivered(Context context, boolean delivered, int messageType, int reportType) {
        Log.d(LOG_TAG_SMS, "(926) PrefsUtils - saveIsReportDelivered(" + AppUtils.messageTypeToString(messageType) + ", delivered: " + delivered + ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        if (messageType == MESSAGE_TYPE_START) {
            if (reportType == REPORT_TYPE_NEXT) editor.putBoolean("next_report_start_delivered", delivered);
            else if (reportType == REPORT_TYPE_LAST) editor.putBoolean("last_report_start_delivered", delivered);
        } else if (messageType == MESSAGE_TYPE_END){
            if (reportType == REPORT_TYPE_NEXT) editor.putBoolean("next_report_end_delivered", delivered);
            else if (reportType == REPORT_TYPE_LAST) editor.putBoolean("last_report_end_delivered", delivered);
        } else {
            if (reportType == REPORT_TYPE_NEXT) {
                editor.putBoolean("next_report_start_delivered", delivered);
                editor.putBoolean("next_report_end_delivered", delivered);
            } else if (reportType == REPORT_TYPE_LAST) {
                editor.putBoolean("last_report_start_delivered", delivered);
                editor.putBoolean("last_report_end_delivered", delivered);
            }
        }

        editor.commit();
    }

    public static boolean isReportDelivered(Context context, int messageType, int reportType) {
        Log.d(LOG_TAG_SMS, "(927) PrefsUtils - isReportDelivered(" + AppUtils.messageTypeToString(messageType) + ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);

        if (messageType == MESSAGE_TYPE_START) {
            Log.d(LOG_TAG_SMS, "(928) messageType == MESSAGE_TYPE_START");

            if (reportType == REPORT_TYPE_NEXT) {
                Log.d(LOG_TAG_SMS, "(929) reportType == REPORT_TYPE_NEXT");
                Log.d(LOG_TAG_SMS, "(930) is delivered: " + sp.getBoolean("next_report_start_delivered", false));
                return sp.getBoolean("next_report_start_delivered", false);
            } else if (reportType == REPORT_TYPE_LAST) {
                Log.d(LOG_TAG_SMS, "(931) reportType == REPORT_TYPE_LAST");
                Log.d(LOG_TAG_SMS, "(932) is delivered: " + sp.getBoolean("last_report_start_delivered", false));
                return sp.getBoolean("last_report_start_delivered", false);
            }
        } else if (messageType == MESSAGE_TYPE_END) {
            Log.d(LOG_TAG_SMS, "(933) messageType == MESSAGE_TYPE_END");

            if (reportType == REPORT_TYPE_NEXT) {
                Log.d(LOG_TAG_SMS, "(934) reportType == REPORT_TYPE_NEXT");
                Log.d(LOG_TAG_SMS, "(935) is delivered: " + sp.getBoolean("next_report_end_delivered", false));
                return sp.getBoolean("next_report_end_delivered", false);
            } else if (reportType == REPORT_TYPE_LAST) {
                Log.d(LOG_TAG_SMS, "(936) reportType == REPORT_TYPE_LAST");
                Log.d(LOG_TAG_SMS, "(937) is delivered: " + sp.getBoolean("last_report_end_delivered", false));
                return sp.getBoolean("last_report_end_delivered", false);
            }
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

    // ---------- PŘÍZNAK O NASTAVENÍ ALARMU NEODESLÁNÍ NEBO NEDORUČENÍ HLÁŠENÍ --------------------

    public static void setAlarm(Context context, boolean isAlarm, int messageType, int alarmType) {
        Log.d(LOG_TAG_SMS,
                "(938) PrefsUtils - setAlarm(" +
                        AppUtils.messageTypeToString(messageType) +
                        ", " +
                        AppUtils.alarmTypeToString(alarmType) +
                        ", is alarm: " +
                        isAlarm + ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        SharedPreferences.Editor editor = sp.edit();

        if (messageType == MESSAGE_TYPE_START) {
            if (alarmType == ALARM_TYPE_NO_SENT) editor.putBoolean("no_sent_start_alarm", isAlarm);
            else if (alarmType == ALARM_TYPE_NO_DELIVERED) editor.putBoolean("no_delivered_start_alarm", isAlarm);
        } else if (messageType == MESSAGE_TYPE_END){
            if (alarmType == ALARM_TYPE_NO_SENT) editor.putBoolean("no_sent_end_alarm", isAlarm);
            else if (alarmType == ALARM_TYPE_NO_DELIVERED) editor.putBoolean("no_delivered_end_alarm", isAlarm);
        } else if (alarmType == ALARM_TYPE_BOTH){
            if (alarmType == ALARM_TYPE_NO_SENT) {
                editor.putBoolean("no_sent_start_alarm", isAlarm);
                editor.putBoolean("no_sent_end_alarm", isAlarm);
            } else {
                editor.putBoolean("no_delivered_start_alarm", isAlarm);
                editor.putBoolean("no_delivered_end_alarm", isAlarm);
            }
        }

        editor.commit();
    }

    public static boolean isAlarm(Context context, int messageType, int alarmType) {
        Log.d(LOG_TAG_SMS,
                "(939) PrefsUtils - isAlarm(" +
                        AppUtils.messageTypeToString(messageType) +
                        ", " +
                        AppUtils.alarmTypeToString(alarmType) +
                        ")");

        SharedPreferences sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);

        if (messageType == MESSAGE_TYPE_START) {
            Log.d(LOG_TAG_SMS, "(940) messageType == MESSAGE_TYPE_START");

            if (alarmType == ALARM_TYPE_NO_SENT) {
                Log.d(LOG_TAG_SMS, "(941) alarmType == ALARM_TYPE_NO_SENT");
                Log.d(LOG_TAG_SMS, "(942) is alarm: " + sp.getBoolean("no_sent_start_alarm", false));
                return sp.getBoolean("no_sent_start_alarm", false);
            } else if (alarmType == ALARM_TYPE_NO_DELIVERED) {
                Log.d(LOG_TAG_SMS, "(943) alarmType == ALARM_TYPE_NO_DELIVERED");
                Log.d(LOG_TAG_SMS, "(944) is alarm: " + sp.getBoolean("no_delivered_start_alarm", false));
                return sp.getBoolean("no_delivered_start_alarm", false);
            } else if (alarmType == ALARM_TYPE_BOTH) {
                Log.d(LOG_TAG_SMS, "(943_1) alarmType == ALARM_TYPE_NO_BOTH");
                return sp.getBoolean("no_sent_start_alarm", false) || sp.getBoolean("no_delivered_start_alarm", false);
            }
        } else if (messageType == MESSAGE_TYPE_END) {
            Log.d(LOG_TAG_SMS, "(945) messageType == MESSAGE_TYPE_END");

            if (alarmType == ALARM_TYPE_NO_SENT) {
                Log.d(LOG_TAG_SMS, "(946) alarmType == ALARM_TYPE_NO_SENT");
                Log.d(LOG_TAG_SMS, "(947) is alarm: " + sp.getBoolean("no_sent_end_alarm", false));
                return sp.getBoolean("no_sent_end_alarm", false);
            } else if (alarmType == ALARM_TYPE_NO_DELIVERED) {
                Log.d(LOG_TAG_SMS, "(948) alarmType == ALARM_TYPE_NO_DELIVERED");
                Log.d(LOG_TAG_SMS, "(949) is alarm: " + sp.getBoolean("no_delivered_end_alarm", false));
                return sp.getBoolean("no_delivered_end_alarm", false);
            } else if (alarmType == ALARM_TYPE_BOTH) {
                Log.d(LOG_TAG_SMS, "(943_2) alarmType == ALARM_TYPE_NO_BOTH");
                return sp.getBoolean("no_sent_end_alarm", false) || sp.getBoolean("no_delivered_end_alarm", false);
            }
        }

        return false;
    }
}
