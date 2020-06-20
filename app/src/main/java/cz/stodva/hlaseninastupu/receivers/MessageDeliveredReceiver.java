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
        Log.d(AppConstants.LOG_TAG, "MessageDeliveredReceiver - onReceive");

        if (intent.hasExtra("message_type")) {
            Log.d(AppConstants.LOG_TAG, "intent hasExtra: " + intent.getIntExtra("message_type", -1));

            if (intent.getIntExtra("message_type", -1) == AppConstants.MESSAGE_TYPE_START) {
                PrefsUtils.saveMsgStartDelivered(context, true);
            } else {
                PrefsUtils.saveMsgEndDelivered(context, true);
            }
        }

        Intent intentResult = new Intent(AppConstants.ACTION_SMS_DELIVERED);
        intentResult.putExtra("message_type", intent.getIntExtra("message_type", -1));
        context.sendBroadcast(intentResult);
    }
}
