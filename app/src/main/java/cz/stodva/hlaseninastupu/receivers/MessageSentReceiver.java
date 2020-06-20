package cz.stodva.hlaseninastupu.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import cz.stodva.hlaseninastupu.MainActivity;
import cz.stodva.hlaseninastupu.utils.AppConstants;
import cz.stodva.hlaseninastupu.utils.PrefsUtils;

public class MessageSentReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(AppConstants.LOG_TAG, "MessageSentReceiver - onReceive");

        if (intent.hasExtra("message_type")) {
            Log.d(AppConstants.LOG_TAG, "intent hasExtra: " + intent.getIntExtra("message_type", -1));

            if (intent.getIntExtra("message_type", -1) == AppConstants.MESSAGE_TYPE_START) {
                PrefsUtils.saveMsgStartSent(context, true);
            } else {
                PrefsUtils.saveMsgEndSent(context, true);
            }
        }

        Intent intentResult = new Intent(AppConstants.ACTION_SMS_SENT);
        intentResult.putExtra("message_type", intent.getIntExtra("message_type", -1));
        context.sendBroadcast(intentResult);
    }
}
