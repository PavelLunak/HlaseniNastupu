package cz.stodva.hlaseninastupu.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.stodva.hlaseninastupu.utils.AppConstants;
import cz.stodva.hlaseninastupu.utils.AppUtils;
import cz.stodva.hlaseninastupu.utils.PrefsUtils;

public class MessageSentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(AppConstants.LOG_TAG_SMS, "MessageSentReceiver - onReceive");

        int messageType = intent.getIntExtra("message_type", -1);
        int reportType = intent.getIntExtra("report_type", -1);

        if (messageType > -1) {

            Log.d(AppConstants.LOG_TAG_SMS, "message type: " + AppUtils.messageTypeToString(messageType));
            Log.d(AppConstants.LOG_TAG_SMS, "report type: " + AppUtils.reportTypeToString(reportType));

            // Nastavení příznaku o úspěšném odeslání hlášení
            PrefsUtils.saveIsReportSent(context, true, messageType, reportType);
        }

        // Odeslání informace o úspěšném odeslání hlášení do MainActivity (pokud je aplikace spuštěna)
        Intent intentResult = new Intent(AppConstants.ACTION_SMS_SENT);
        intentResult.putExtra("message_type", messageType);
        intentResult.putExtra("report_type", reportType);
        context.sendBroadcast(intentResult);
    }
}
