package cz.stodva.hlaseninastupu.receivers;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;

import cz.stodva.hlaseninastupu.MainActivity;
import cz.stodva.hlaseninastupu.objects.AppSettings;
import cz.stodva.hlaseninastupu.utils.AppConstants;
import cz.stodva.hlaseninastupu.utils.PrefsUtils;
import cz.stodva.hlaseninastupu.utils.WakeLocker;

public class TimerReceiver extends BroadcastReceiver implements AppConstants {

    TelephonyManager telephonyManager;
    PhoneStateListener phoneStateListener;

    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d(LOG_TAG_SMS, "TimerReceiver - onReceive()");

        WakeLocker.acquire(context);

        final int messageType = intent.getIntExtra("message_type", 0);

        //Jde o budík, po kterém má být zkontrolováno odeslání (doručení) hlášení
        if (intent.hasExtra("alarm_check_error")) {

            Log.d(LOG_TAG_SMS, "TimerReceiver - CHECK ERROR");

            String errMsg = null;

            // NÁSTUP
            if (messageType == MESSAGE_TYPE_START) {

                Log.d(LOG_TAG_SMS, "TimerReceiver - NÁSTUP");

                // JE NASTAVEN ALARM NEODESLÁNÍ NÁSTUPU?
                if (PrefsUtils.isNoSentAlarm(context, MESSAGE_TYPE_START)) {
                    Log.d(LOG_TAG_SMS, "TimerReceiver - JE NASTAVEN ALARM NEODESLÁNÍ NÁSTUPU?");
                    //Bylo hlášení nástupu odesláno?
                    if (!PrefsUtils.isReportSent(context, MESSAGE_TYPE_START)) {
                        Log.d(LOG_TAG_SMS, "TimerReceiver - Nepodařilo se odeslat hlášení nástupu");
                        errMsg = "Nepodařilo se v nastaveném časovém limitu odeslat hlášení nástupu!";
                    }
                }

                //BYL REPORT ODESLÁN? POKUD ANO, ZKONTROLUJEME JEHO DORUČENÍ
                if (PrefsUtils.isReportSent(context, MESSAGE_TYPE_START)) {
                    // JE NASTAVEN ALARM NEDORUČENÍ NÁSTUPU?
                    if (PrefsUtils.isNoDeliveredAlarm(context, MESSAGE_TYPE_START)) {
                        Log.d(LOG_TAG_SMS, "TimerReceiver - JE NASTAVEN ALARM NEDORUČENÍ NÁSTUPU");
                        // Bylo hlášení nástupu doručeno?
                        if (!PrefsUtils.isReportDelivered(context, MESSAGE_TYPE_START)) {
                            Log.d(LOG_TAG_SMS, "TimerReceiver - Hlášení nástupu nebylo doručeno");
                            errMsg = "Hlášení nástupu nebylo v nastaveném časovém limitu doručeno!";
                        }
                    }
                }
            }
            // KONEC
            else if (messageType == MESSAGE_TYPE_END) {

                Log.d(LOG_TAG_SMS, "TimerReceiver - KONEC");

                // JE NASTAVEN ALARM NEODESLÁNÍ KONCE?
                if (PrefsUtils.isNoSentAlarm(context, MESSAGE_TYPE_END)) {
                    Log.d(LOG_TAG_SMS, "TimerReceiver - JE NASTAVEN ALARM NEODESLÁNÍ KONCE");
                    // Bylo hlášení konce odesláno?
                    if (!PrefsUtils.isReportSent(context, MESSAGE_TYPE_END)) {
                        Log.d(LOG_TAG_SMS, "TimerReceiver - Nepodařilo se odeslat hlášení konce");
                        errMsg = "Nepodařilo se v nastaveném časovém limitu odeslat hlášení konce!";
                    }
                }

                //BYL REPORT ODESLÁN? POKUD ANO, ZKONTROLUJEME JEHO DORUČENÍ
                if (PrefsUtils.isReportSent(context, MESSAGE_TYPE_END)) {
                    // JE NASTAVEN ALARM NEDORUČENÍ KONCE?
                    if (PrefsUtils.isNoDeliveredAlarm(context, MESSAGE_TYPE_END)) {
                        Log.d(LOG_TAG_SMS, "TimerReceiver - JE NASTAVEN ALARM NEDORUČENÍ KONCE");
                        // Bylo hlášení konce doručeno?
                        if (!PrefsUtils.isReportDelivered(context, MESSAGE_TYPE_END)) {
                            Log.d(LOG_TAG_SMS, "TimerReceiver - Hlášení konce nebylo doručeno");
                            errMsg = "Hlášení konce nebylo v nastaveném časovém limitu doručeno!";
                        }
                    }
                }
            } else {
                WakeLocker.release();
                return;
            }

            if (errMsg != null) {
                sendError(context, messageType, errMsg);
            }
        } else {
            Log.d(LOG_TAG_SMS, "TimerReceiver - SEND REPORT");

            PrefsUtils.setIsTimer(context, messageType, false);

            final String text = getMessage(context, messageType);

            if (text == null) {
                WakeLocker.release();
                return;
            }

            Log.d(LOG_TAG_SMS, "- - - ODESÍLÁNÍ HLÁŠENÍ - - -");

            telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            phoneStateListener = new PhoneStateListener() {
                @Override
                public void onServiceStateChanged(ServiceState serviceState) {
                    super.onServiceStateChanged(serviceState);

                    switch (serviceState.getState()) {
                        case ServiceState.STATE_IN_SERVICE:
                            Log.d(LOG_TAG, "TimerReceiver - onServiceStateChanged: STATE_IN_SERVICE");
                            sendReport(context, messageType, text);
                            break;
                        case ServiceState.STATE_OUT_OF_SERVICE:
                            Log.d(LOG_TAG, "TimerReceiver - onServiceStateChanged: STATE_OUT_OF_SERVICE: ");
                            sendError(context, messageType, "HLÁŠENÍ NELZE ODESLAT - není dostupná síť");
                            break;
                        case ServiceState.STATE_EMERGENCY_ONLY:
                            Log.d(LOG_TAG, "TimerReceiver - onServiceStateChanged: STATE_EMERGENCY_ONLY");
                            sendError(context, messageType, "HLÁŠENÍ NELZE ODESLAT - je povoleno pouze tísňové volání");
                            break;
                        case ServiceState.STATE_POWER_OFF:
                            Log.d(LOG_TAG, "TimerReceiver - onServiceStateChanged: STATE_POWER_OFF");
                            sendError(context, messageType, "HLÁŠENÍ NELZE ODESLAT - je zapnut režim letadlo");
                            break;
                        default:
                            Log.d(LOG_TAG, "TimerReceiver - onServiceStateChanged: UNKNOWN_STATE: ");
                            sendError(context, messageType, "HLÁŠENÍ NELZE ODESLAT - neznámý důvod nedostupnosti sítě");
                            break;
                    }
                }
            };

            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        }

        WakeLocker.release();
    }

    private String getMessage(Context context, int messageType) {
        AppSettings appSettings = PrefsUtils.getAppSettings(context);

        if (messageType == MESSAGE_TYPE_START) {
            if (appSettings.getStartMessage() == null) return null;
            if (appSettings.getStartMessage().equals("")) return null;
            return appSettings.getStartMessage();
        } else if (messageType == MESSAGE_TYPE_END) {
            if (appSettings.getEndMessage() == null) return null;
            if (appSettings.getEndMessage().equals("")) return null;
            return appSettings.getEndMessage();
        }

        return null;
    }

    private void sendReport(Context context, int messageType, String text) {
        Log.d(LOG_TAG, "TimerReceiver - sendReport()");

        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);

        Intent sentIntent = new Intent(context, MessageSentReceiver.class);
        sentIntent.putExtra("message_type", messageType);
        PendingIntent pi1 = PendingIntent.getBroadcast(
                context,
                messageType == MESSAGE_TYPE_START ? SENT_REQUEST_START : SENT_REQUEST_END,
                sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Intent deliveredIntent = new Intent(context, MessageDeliveredReceiver.class);
        deliveredIntent.putExtra("message_type", messageType);
        PendingIntent pi2 = PendingIntent.getBroadcast(
                context,
                messageType == MESSAGE_TYPE_START ? DELIVERED_REQUEST_START : DELIVERED_REQUEST_END,
                deliveredIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        SmsManager sm = SmsManager.getDefault();
        sm.sendTextMessage(
                getPhoneNumber(context),
                null,
                text,
                pi1,
                pi2);

        PrefsUtils.saveIsReportSent(context, true, messageType);
        PrefsUtils.setNoSentAlarm(context, false, messageType);
    }

    private void sendError(Context context, int messageType, String errMessage) {

        Log.d(LOG_TAG, "TimerReceiver - sendError()");

        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);

        if (!PrefsUtils.isNoSentAlarm(context, messageType) && !PrefsUtils.isNoDeliveredAlarm(context, messageType)) return;

        cancelTimerForError(context, messageType);
        PrefsUtils.saveIsReportSent(context, false, messageType);
        PrefsUtils.saveIsReportDelivered(context, false, messageType);

        Intent intentToMain = new Intent(context, MainActivity.class);
        intentToMain.putExtra("messageType", messageType);
        intentToMain.putExtra("error_message", errMessage);
        intentToMain.putExtra("on_error", 1);
        intentToMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intentToMain);
    }

    public void cancelTimerForError(Context context, int messageType) {

        Log.d(LOG_TAG, "TimerReceiver - cancelTimerForError()");

        Intent intent = new Intent(context, TimerReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                ALARM_REQUEST_ERROR,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pendingIntent);

        PrefsUtils.setNoSentAlarm(context, false, messageType);
        PrefsUtils.setNoDeliveredAlarm(context, false, messageType);
    }

    private String getPhoneNumber(Context context) {
        AppSettings appSettings = PrefsUtils.getAppSettings(context);

        if (appSettings.getPhoneNumber().equals("")) return PHONE_NUMBER;
        else return appSettings.getPhoneNumber();
    }
}