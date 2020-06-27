package cz.stodva.hlaseninastupu.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.stodva.hlaseninastupu.utils.AppConstants;
import cz.stodva.hlaseninastupu.utils.PrefsUtils;

public class MessageDeliveredReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(AppConstants.LOG_TAG_SMS, "MessageDeliveredReceiver - onReceive");

        if (intent.hasExtra("message_type")) {
            Log.d(AppConstants.LOG_TAG_SMS, "intent hasExtra : " + intent.getIntExtra("message_type", -1));
            PrefsUtils.saveIsReportDelivered(context, true, intent.getIntExtra("message_type", -1));
        }

        Intent intentResult = new Intent(AppConstants.ACTION_SMS_DELIVERED);
        intentResult.putExtra("message_type", intent.getIntExtra("message_type", -1));
        context.sendBroadcast(intentResult);
    }
}
