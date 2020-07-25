package cz.stodva.hlaseninastupu.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Date;

import cz.stodva.hlaseninastupu.database.DataSource;
import cz.stodva.hlaseninastupu.database.DbHelper;
import cz.stodva.hlaseninastupu.listeners.OnReportLoadedListener;
import cz.stodva.hlaseninastupu.listeners.OnReportUpdatedListener;
import cz.stodva.hlaseninastupu.objects.Report;
import cz.stodva.hlaseninastupu.utils.AppConstants;


public class MessageDeliveredReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(final Context context, Intent intent) {
        Log.d(AppConstants.LOG_TAG_SMS, "MessageDeliveredReceiver - onReceive" + AppConstants.LOG_UNDERLINED);

        final int reportId = intent.getIntExtra("report_id", -1);
        Log.d(AppConstants.LOG_TAG_SMS, AppConstants.LOG_TAB + "report id: " + reportId);

        if (reportId > -1) {
            final DataSource dataSource = new DataSource(context);

            // Získání právě doručeného hlášení, u kterého zkontrolujeme, jestli není nastaven na FAILED, což by
            // znamenalo, že hlášení bylo doručeno až po kontrole doručení a má nastavenu zprávu pro uživatele
            // o jeho nedoručení. Pokud bylo později hlášení přesto doručeno, změníme tuto zprávu, aby balo jasné,
            // že bylo hlášení pouze doručeno později.
            dataSource.getReportById(reportId, new OnReportLoadedListener() {
                @Override
                public void onReportLoaded(Report loadedReport) {
                    if (loadedReport != null) {
                        if (loadedReport.isFailed()) {
                            // Aktualizace zprávy
                            dataSource.updateReportMessage(reportId, "Hlášení bylo doručeno později...", new OnReportUpdatedListener() {
                                @Override
                                public void onReportUpdated(Report updatedReport) {
                                    // Aktualizace dat hlášení po jeho doručení
                                    dataSource.updateReportValue(
                                            reportId,
                                            new String[] {DbHelper.COLUMN_DELIVERED, DbHelper.COLUMN_IS_FAILED, DbHelper.COLUMN_IS_DELIVERED},
                                            new long[] {new Date().getTime(), 0, 1},
                                            null);

                                    // Odeslání informace o úspěšném odeslání hlášení do MainActivity (pokud je aplikace spuštěna)
                                    Intent intentResult = new Intent(AppConstants.ACTION_REPORT_RESULT);
                                    intentResult.putExtra("report_id", reportId);
                                    context.sendBroadcast(intentResult);
                                }
                            });
                        }
                    }
                }
            });
        } else {
            Log.d(AppConstants.LOG_TAG_SMS, AppConstants.LOG_TAB + "Nenalezeno hlášení podle ID");
        }
    }
}
