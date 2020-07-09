package cz.stodva.hlaseninastupu.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Date;

import cz.stodva.hlaseninastupu.database.DataSource;
import cz.stodva.hlaseninastupu.database.DbHelper;
import cz.stodva.hlaseninastupu.listeners.OnReportUpdatedListener;
import cz.stodva.hlaseninastupu.objects.Report;
import cz.stodva.hlaseninastupu.utils.AppConstants;


public class MessageDeliveredReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d(AppConstants.LOG_TAG_SMS, "MessageDeliveredReceiver - onReceive");

        final int reportId = intent.getIntExtra("report_id", -1);
        Log.d(AppConstants.LOG_TAG_SMS, "report id: " + reportId);

        if (reportId > -1) {
            DataSource dataSource = new DataSource(context);
            dataSource.updateReportValue(reportId, DbHelper.COLUMN_DELIVERED, new Date().getTime(), new OnReportUpdatedListener() {
                @Override
                public void onReportUpdated(Report updatedReport) {
                    // Odeslání informace o úspěšném odeslání hlášení do MainActivity (pokud je aplikace spuštěna)
                    Intent intentResult = new Intent(AppConstants.ACTION_REPORT_RESULT);
                    intentResult.putExtra("report_id", reportId);
                    context.sendBroadcast(intentResult);
                }
            });
        }
    }
}
