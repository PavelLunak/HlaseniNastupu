package cz.stodva.hlaseninastupu.objects;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import cz.stodva.hlaseninastupu.utils.AppConstants;

public class TimerRequestCodeGenerator {

    Context context;
    SharedPreferences sp;
    SharedPreferences.Editor editor;

    int lastRequestCode;


    public TimerRequestCodeGenerator(Context context) {
        this.context = context;
        load();
    }

    public int getNewRequestCode() {
        Log.d(AppConstants.LOG_TAG, "TimerRequestCodeGenerator - getTimerRequestCode()");
        load();
        lastRequestCode += 1;
        save();
        Log.d(AppConstants.LOG_TAG, "new code: " + lastRequestCode);
        return lastRequestCode;
    }

    private void save() {
        if (sp == null) sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        if (editor == null) editor = sp.edit();
        editor.putInt("last_request_code", lastRequestCode);
        editor.commit();
    }

    private void load() {
        if (sp == null) sp = context.getSharedPreferences("hlaseni_nastupu_app", context.MODE_PRIVATE);
        lastRequestCode = sp.getInt("last_request_code", 0);
        if (lastRequestCode < 10) lastRequestCode = 10;
    }
}
