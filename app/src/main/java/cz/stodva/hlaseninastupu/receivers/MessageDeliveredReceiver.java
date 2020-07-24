package cz.stodva.hlaseninastupu.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Date;

import cz.stodva.hlaseninastupu.database.DataSource;
import cz.stodva.hlaseninastupu.database.DbHelper;
import cz.stodva.hlaseninastupu.utils.AppConstants;


public class MessageDeliveredReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d(AppConstants.LOG_TAG_SMS, "MessageDeliveredReceiver - onReceive" + AppConstants.LOG_UNDERLINED);

        final int reportId = intent.getIntExtra("report_id", -1);
        Log.d(AppConstants.LOG_TAG_SMS, AppConstants.LOG_TAB + "report id: " + reportId);

        if (reportId > -1) {
            final DataSource dataSource = new DataSource(context);

            dataSource.updateReportValue(
                    reportId,
                    new String[] {DbHelper.COLUMN_DELIVERED, /*DbHelper.COLUMN_REQUEST_CODE, */DbHelper.COLUMN_IS_FAILED, DbHelper.COLUMN_IS_DELIVERED},
                    new long[] {new Date().getTime(), /*AppConstants.NONE, */0, 1},
                    null);

            // Odeslání informace o úspěšném odeslání hlášení do MainActivity (pokud je aplikace spuštěna)
            Intent intentResult = new Intent(AppConstants.ACTION_REPORT_RESULT);
            intentResult.putExtra("report_id", reportId);
            context.sendBroadcast(intentResult);
        } else {
            Log.d(AppConstants.LOG_TAG_SMS, AppConstants.LOG_TAB + "Nenalezeno hlášení podle ID");
        }
    }
}
