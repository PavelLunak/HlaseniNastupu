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
import cz.stodva.hlaseninastupu.database.DataSource;
import cz.stodva.hlaseninastupu.database.DbHelper;
import cz.stodva.hlaseninastupu.listeners.OnReportLoadedListener;
import cz.stodva.hlaseninastupu.listeners.OnReportUpdatedListener;
import cz.stodva.hlaseninastupu.objects.AppSettings;
import cz.stodva.hlaseninastupu.objects.Report;
import cz.stodva.hlaseninastupu.utils.AppConstants;
import cz.stodva.hlaseninastupu.utils.AppUtils;
import cz.stodva.hlaseninastupu.utils.PrefsUtils;
import cz.stodva.hlaseninastupu.utils.WakeLocker;

public class TimerReceiver extends BroadcastReceiver implements AppConstants {

    TelephonyManager telephonyManager;
    PhoneStateListener phoneStateListener;

    DataSource dataSource;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        Log.d(LOG_TAG_SMS, "(01) TimerReceiver - onReceive()" + LOG_UNDERLINED);

        dataSource = new DataSource(context);

        WakeLocker.acquire(context);

        final int reportId = intent.getIntExtra("report_id", -1);

        if (reportId < 0) return;

        dataSource.getReportById(reportId, new OnReportLoadedListener() {
            @Override
            public void onReportLoaded(Report report) {
                if (report != null) process(context, intent, report);
                else Log.d(LOG_TAG_SMS, LOG_TAB + "NENALEZENO HLÁŠENÍ PODLE ID");
            }
        });

        Log.d(AppConstants.LOG_TAG_SMS, "(02) report id: " + reportId);
    }

    private void process(final Context context, final Intent intent, final Report report) {
        //Jde o budík, po kterém má být zkontrolováno odeslání (doručení) hlášení
        if (intent.hasExtra("alarm_check_error")) {

            Log.d(LOG_TAG_SMS, LOG_TAB + "(04) TimerReceiver - - - - - - - CHECK ERROR - - - - - - -");

            String errMsg = null;
            int errorType = 0;

            // NEBYLO HLÁŠENÍ ODESLÁNO?
            if (report.getSentTime() == WAITING) {
                Log.d(LOG_TAG_SMS, LOG_TAB + "(07) TimerReceiver - Nepodařilo se odeslat hlášení");
                errMsg = "Nepodařilo se v nastaveném časovém limitu odeslat hlášení!";
                errorType = ERROR_TYPE_NO_SENT;
            }

            // BYLO HLÁŠENÍ ODESLÁNO? POKUD ANO, ZKONTROLUJEME JEHO DORUČENÍ
            if (report.getSentTime() > NONE) {
                if (report.getDeliveryTime() == WAITING) {
                    Log.d(LOG_TAG_SMS, "(09) TimerReceiver - Hlášení nebylo doručeno");
                    errMsg = "Hlášení nebylo v nastaveném časovém limitu doručeno!";
                    errorType = ERROR_TYPE_NO_DELIVERED;
                }
            }

            if (errMsg != null) {
                requestShowNoSentError(context, report, errMsg, errorType);
            }
        } else {
            Log.d(LOG_TAG_SMS, "(15) TimerReceiver - - - - - - - SEND REPORT - - - - - - -");

            final String text = getMessage(context, report.getMessageType());

            if (text == null) {
                WakeLocker.release();
                return;
            }

            // Nastavení doručení na neúspěšné pro případ nedoručení hlášení
            dataSource.updateReportValue(
                    report.getId(),
                    new String[] {DbHelper.COLUMN_DELIVERED},
                    new long[] {UNSUCCESFUL},
                    null);

            telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

            phoneStateListener = new PhoneStateListener() {
                @Override
                public void onServiceStateChanged(ServiceState serviceState) {
                    super.onServiceStateChanged(serviceState);

                    switch (serviceState.getState()) {
                        case ServiceState.STATE_IN_SERVICE:
                            Log.d(LOG_TAG, "(17) TimerReceiver - onServiceStateChanged: STATE_IN_SERVICE");
                            sendReport(context, report, text);
                            break;
                        case ServiceState.STATE_OUT_OF_SERVICE:
                            Log.d(LOG_TAG, "(18) TimerReceiver - onServiceStateChanged: STATE_OUT_OF_SERVICE: ");
                            requestShowNoSentError(context, report, "Hlášení nebylo možné odeslat - není dostupná síť", ERROR_TYPE_NO_SENT);
                            break;
                        case ServiceState.STATE_EMERGENCY_ONLY:
                            Log.d(LOG_TAG, "(19) TimerReceiver - onServiceStateChanged: STATE_EMERGENCY_ONLY");
                            requestShowNoSentError(context, report, "Hlášení nebylo možné odeslat - je povoleno pouze tísňové volání", ERROR_TYPE_NO_SENT);
                            break;
                        case ServiceState.STATE_POWER_OFF:
                            Log.d(LOG_TAG, "(20) TimerReceiver - onServiceStateChanged: STATE_POWER_OFF");
                            requestShowNoSentError(context, report, "Hlášení nebylo možné odeslat - je zapnut režim letadlo", ERROR_TYPE_NO_SENT);
                            break;
                        default:
                            Log.d(LOG_TAG, "(21) TimerReceiver - onServiceStateChanged: UNKNOWN_STATE: ");
                            requestShowNoSentError(context, report, "Hlášení nebylo možné odeslat - neznámý důvod nedostupnosti sítě", ERROR_TYPE_NO_SENT);
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

    private void sendReport(Context context, Report report, String text) {
        Log.d(LOG_TAG, "(22) TimerReceiver - sendReport()");

        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);

        AppUtils.vibrate(context);

        Intent sentIntent = new Intent(context, MessageSentReceiver.class);
        sentIntent.putExtra("report_id", report.getId());
        PendingIntent pi1 = PendingIntent.getBroadcast(
                context,
                1,
                sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Intent deliveredIntent = new Intent(context, MessageDeliveredReceiver.class);
        deliveredIntent.putExtra("report_id", report.getId());
        PendingIntent pi2 = PendingIntent.getBroadcast(
                context,
                2,
                deliveredIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        SmsManager sm = SmsManager.getDefault();
        sm.sendTextMessage(
                getPhoneNumber(context),
                null,
                text,
                pi1,
                pi2);
    }

    private void requestShowNoSentError(Context context, final Report report, String errMsg, int errorType) {

        Log.d(LOG_TAG, "(23) TimerReceiver - sendError(errorType: " + AppUtils.errorTypeToString(errorType) + ")");

        if (dataSource == null) dataSource = new DataSource(context);

        if (errorType == ERROR_TYPE_NO_SENT) {
            dataSource.updateReportMessage(report.getId(), errMsg, null);

            // Pokud nelze hlášení odeslat, bude zrušen alarm pro kontrolu odeslání a doručení
            dataSource.updateReportValue(
                    report.getId(),
                    new String[] {DbHelper.COLUMN_SENT, DbHelper.COLUMN_DELIVERED, DbHelper.COLUMN_IS_FAILED},
                    new long[] {UNSUCCESFUL, UNSUCCESFUL, 1},
                    null);

            cancelTimerForError(context, report);
        } else {
            dataSource.updateReportMessage(report.getId(), errMsg, null);
            dataSource.updateReportValue(
                    report.getId(),
                    new String[] {DbHelper.COLUMN_DELIVERED, DbHelper.COLUMN_IS_FAILED},
                    new long[] {UNSUCCESFUL, 1},
                    null);
        }

        telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);

        // Je nastaven příznak o potřebě alarmu při neodeslání i nedoručení hlášení
        if (report.isErrorAlert()) {
            Intent intentToMain = new Intent(context, MainActivity.class);
            intentToMain.putExtra("report_id", report.getId());
            intentToMain.putExtra("on_error", 1);
            intentToMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intentToMain);
        } else {
            // Odeslání informace o neodeslání do aktivity, pokud je otevřena, pro aktualizaci zobrazených dat
            Intent intentResult = new Intent(ACTION_REPORT_RESULT);
            intentResult.putExtra("report_id", report.getId());
            context.sendBroadcast(intentResult);
        }
    }

    public void cancelTimerForError(final Context context, final Report report) {

        Log.d(LOG_TAG, "(24) TimerReceiver - cancelTimerForError()");

        Intent intent = new Intent(context, TimerReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                report.getRequestCodeForErrorAlarm(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        am.cancel(pendingIntent);

        dataSource.updateReportValue(
                report.getId(),
                new String[] {DbHelper.COLUMN_ERROR_REQUEST_CODE},
                new long[] {NONE},
                new OnReportUpdatedListener() {
                    @Override
                    public void onReportUpdated(Report updatedReport) {
                        report.setRequestCodeForErrorAlarm(NONE);
                    }
                });
    }

    private String getPhoneNumber(Context context) {
        AppSettings appSettings = PrefsUtils.getAppSettings(context);

        if (appSettings.getPhoneNumber().equals("")) return PHONE_NUMBER;
        else return appSettings.getPhoneNumber();
    }
}
