package cz.stodva.hlaseninastupu.receivers;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import cz.stodva.hlaseninastupu.MainActivity;

public class TimerReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        int messageType = intent.getIntExtra("message_type", 0);

        Intent intentToMain = new Intent(context, MainActivity.class);
        intentToMain.putExtra("on_timer", 1);
        intentToMain.putExtra("messageType", messageType);
        intentToMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intentToMain);
    }
}
