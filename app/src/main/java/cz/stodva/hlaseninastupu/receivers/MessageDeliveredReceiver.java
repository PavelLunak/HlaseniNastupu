package cz.stodva.hlaseninastupu.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.stodva.hlaseninastupu.utils.AppConstants;
import cz.stodva.hlaseninastupu.utils.AppUtils;
import cz.stodva.hlaseninastupu.utils.PrefsUtils;

public class MessageDeliveredReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(AppConstants.LOG_TAG_SMS, "MessageDeliveredReceiver - onReceive");

        int messageType = intent.getIntExtra("message_type", -1);
        int reportType = intent.getIntExtra("report_type", -1);

        if (messageType > -1) {

            Log.d(AppConstants.LOG_TAG_SMS, "message type: " + AppUtils.messageTypeToString(messageType));
            Log.d(AppConstants.LOG_TAG_SMS, "report type: " + AppUtils.reportTypeToString(reportType));

            // Nastavení příznaku o úspěšném doručení hlášení
            PrefsUtils.saveIsReportDelivered(context, true, messageType, AppConstants.REPORT_TYPE_LAST);
        }

        // Odeslání informace o úspěšném odeslání hlášení do MainActivity (pokud je aplikace spuštěna)
        Intent intentResult = new Intent(AppConstants.ACTION_SMS_DELIVERED);
        intentResult.putExtra("message_type", messageType);
        intentResult.putExtra("report_type", reportType);
        context.sendBroadcast(intentResult);
    }
}
