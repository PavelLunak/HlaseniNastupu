package cz.stodva.hlaseninastupu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.util.Log;
import android.widget.Toast;

import com.facebook.stetho.Stetho;

import java.util.Calendar;

import cz.stodva.hlaseninastupu.fragments.FragmentMain;
import cz.stodva.hlaseninastupu.fragments.FragmentSettings;
import cz.stodva.hlaseninastupu.fragments.FragmentTimer;
import cz.stodva.hlaseninastupu.objects.Settings;
import cz.stodva.hlaseninastupu.receivers.TimerReceiver;
import cz.stodva.hlaseninastupu.utils.AppConstants;
import cz.stodva.hlaseninastupu.utils.PrefsUtils;


public class MainActivity extends AppCompatActivity implements
        AppConstants,
        FragmentManager.OnBackStackChangedListener {

    int animShowFragment = R.anim.anim_fragment_show;
    int animHideFragment = R.anim.anim_fragment_hide;

    public FragmentManager fragmentManager;

    int dayStart = -1;
    int monthStart = -1;
    int yearStart = -1;
    int hoursStart = -1;
    int minutesStart = -1;

    int dayEnd = -1;
    int monthEnd = -1;
    int yearEnd = -1;
    int hoursEnd = -1;
    int minutesEnd = -1;

    MediaPlayer mediaPlayer;
    Settings settings;

    private BroadcastReceiver smsSentBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(LOG_TAG, "MainActivity - broadcastReceiver - onReceive");

            if (intent.getAction().equals(ACTION_SMS_SENT)){
                Log.i(LOG_TAG, "ACTION_SMS_SENT");
                Log.d(AppConstants.LOG_TAG, "intent hasExtra: " + intent.getIntExtra("message_type", -1));

                FragmentMain fm = (FragmentMain) fragmentManager.findFragmentByTag(FRAGMENT_MAIN_NAME);
                if (fm != null) fm.updateLastReportInfo();
            }
        }
    };

    private BroadcastReceiver smsDeliveredBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(LOG_TAG, "MainActivity - broadcastReceiver - onReceive");

            if (intent.getAction().equals(ACTION_SMS_DELIVERED)) {
                Log.i(LOG_TAG, "ACTION_SMS_DELIVERED");
                Log.d(AppConstants.LOG_TAG, "intent hasExtra: " + intent.getIntExtra("message_type", -1));

                FragmentMain fm = (FragmentMain) fragmentManager.findFragmentByTag(FRAGMENT_MAIN_NAME);

                if (fm != null) {
                    int msgType = intent.getIntExtra("message_type", -1);

                    if (msgType == MESSAGE_TYPE_START) {
                        fm.getImgResult(R.id.imgDeliveredSart).setImageDrawable(AppCompatResources.getDrawable(MainActivity.this, R.drawable.ic_check_green));
                    } else if (msgType == MESSAGE_TYPE_END) {
                        fm.getImgResult(R.id.imgDeliveredEnd).setImageDrawable(AppCompatResources.getDrawable(MainActivity.this, R.drawable.ic_check_green));
                    } else {
                        Log.i(LOG_TAG, "bad message type");
                    }
                }
            }
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initStetho();

        fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);

        if (savedInstanceState == null) {
            showFragment(FRAGMENT_MAIN_NAME);
            //checkSettings();
        }

        Intent incommingIntent = getIntent();

        if (incommingIntent != null) {
            if (incommingIntent.hasExtra("on_timer")) {

                int messageType = incommingIntent.getIntExtra("messageType", -1);

                if (messageType > -1) {
                    PrefsUtils.setIsTimer(this, messageType, false);

                    String text = getMessage(messageType);
                    if (text == null) return;

                    SmsManager sm = SmsManager.getDefault();
                    sm.sendTextMessage(
                            getPhoneNumber(),
                            null,
                            text,
                            null,
                            null);
                } else {
                    mediaPlayer = MediaPlayer.create(this, R.raw.ha);
                    mediaPlayer.start();

                    AlertDialog.Builder dlgAlert = new AlertDialog.Builder(MainActivity.this);
                    dlgAlert.setTitle("CHYBA");
                    dlgAlert.setMessage("Chyba při pokusu odeslat hlášení! Chyba: neznámý typ hlášení.");

                    dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int whichButton) {
                            if (mediaPlayer != null) {
                                mediaPlayer.stop();
                                dialog.cancel();
                                //cancelTimer();
                            }
                        }
                    });

                    dlgAlert.show();
                }
            } else if (incommingIntent.hasExtra("key_message_sent")) {
                Log.d(AppConstants.LOG_TAG, "incommingIntent - hasExtra(key_message_sent)");
            } else if (incommingIntent.hasExtra("key_message_delivered")) {
                Log.d(AppConstants.LOG_TAG, "incommingIntent - hasExtra(key_message_delivered)");
            }
        }

        registerReceiver(smsSentBroadcastReceiver, new IntentFilter(ACTION_SMS_SENT));
        registerReceiver(smsDeliveredBroadcastReceiver, new IntentFilter(ACTION_SMS_DELIVERED));
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        unregisterReceiver(smsSentBroadcastReceiver);
        unregisterReceiver(smsDeliveredBroadcastReceiver);
    }

    @Override
    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount() - 1).getName().equals(FRAGMENT_SETTINGS_NAME)) {
            FragmentSettings frSettings = (FragmentSettings) fragmentManager.findFragmentByTag(FRAGMENT_SETTINGS_NAME);

            if (frSettings != null) {
                frSettings.saveData();
                super.onBackPressed();
                return;
            }
        }

        if (fragmentManager.getBackStackEntryCount() == 1) finish();
        else super.onBackPressed();
    }

    @Override
    public void onBackStackChanged() {

    }

    public void showFragment(String name) {
        Fragment fragment = fragmentManager.findFragmentByTag(name);

        if (fragment == null) addFragment(fragment, name);
        else restoreFragment(name);
    }

    public void addFragment(Fragment fragment, String name) {
        if (fragment == null) {
            fragment = createFragment(name);
        }

        Bundle args = new Bundle();
        fragment.setArguments(args);

        FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
        fragmentTransaction.setCustomAnimations(animShowFragment, animHideFragment, animShowFragment, animHideFragment);
        fragmentTransaction.add(R.id.container, fragment, name);
        fragmentTransaction.addToBackStack(name);
        fragmentTransaction.commit();
    }

    public void restoreFragment(String name) {
        int beCount = fragmentManager.getBackStackEntryCount();
        if (beCount == 0) return;
        fragmentManager.popBackStack(name, 0);
    }

    public Fragment createFragment(String name) {
        if (name.equals(FRAGMENT_MAIN_NAME)) {
            FragmentMain newFragment = new FragmentMain();
            return newFragment;
        } else if (name.equals(FRAGMENT_TIMER_NAME)) {
            FragmentTimer newFragment = new FragmentTimer();
            return newFragment;
        } else if (name.equals(FRAGMENT_SETTINGS_NAME)) {
            FragmentSettings newFragment = new FragmentSettings();
            return newFragment;
        }

        return null;
    }

    public void setTimeData(int hours, int minutes, int messageType) {
        if (messageType == MESSAGE_TYPE_START) {
            this.hoursStart = hours;
            this.minutesStart = minutes;
        } else {
            this.hoursEnd = hours;
            this.minutesEnd = minutes;
        }
    }

    public void setDateData(int day, int month, int year, int messageType) {
        if (messageType == MESSAGE_TYPE_START) {
            this.dayStart = day;
            this.monthStart = month;
            this.yearStart = year;
        } else {
            this.dayEnd = day;
            this.monthEnd = month;
            this.yearEnd = year;
        }
    }

    public boolean checkTimeInput(int messageType) {
        if (messageType == MESSAGE_TYPE_START) {
            if (hoursStart < 0 || minutesStart < 0) {
                Toast.makeText(this, "Není nastaven čas odeslání!", Toast.LENGTH_LONG).show();
                return false;
            }
        } else {
            if (hoursEnd < 0 || minutesEnd < 0) {
                Toast.makeText(this, "Není nastaven čas odeslání!", Toast.LENGTH_LONG).show();
                return false;
            }
        }

        return true;
    }

    public long getTimeInMillis(int messageType) {
        Calendar calendar = Calendar.getInstance();

        if (messageType == MESSAGE_TYPE_START) {
            calendar.set(yearStart, monthStart, dayStart, hoursStart, minutesStart, 0);
        } else {
            calendar.set(yearEnd, monthEnd, dayEnd, hoursEnd, minutesEnd, 0);
        }

        return calendar.getTimeInMillis();
    }

    public void setTimer(int messageType) {

        if (messageType == MESSAGE_TYPE_START) {
            PrefsUtils.saveMsgStartSent(this, false);
            PrefsUtils.saveMsgStartDelivered(this, false);
        } else if (messageType == MESSAGE_TYPE_END) {
            PrefsUtils.saveMsgEndSent(this, false);
            PrefsUtils.saveMsgEndDelivered(this, false);
        }

        Intent intent = new Intent(this, TimerReceiver.class);
        intent.putExtra("message_type", messageType);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                messageType == MESSAGE_TYPE_START ? ALARM_REQUEST_CODE_START : ALARM_REQUEST_CODE_END,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, getTimeInMillis(messageType), pendingIntent);
        PrefsUtils.setIsTimer(this, messageType, true);
        PrefsUtils.setLastReportTime(this, getTimeInMillis(messageType), messageType);

        if (fragmentManager.findFragmentByTag(FRAGMENT_TIMER_NAME) != null) {
            ((FragmentTimer) fragmentManager.findFragmentByTag(FRAGMENT_TIMER_NAME)).updateBtnCancelTimer(messageType);
            ((FragmentTimer) fragmentManager.findFragmentByTag(FRAGMENT_TIMER_NAME)).updateNextTimerInfo(messageType);
        }

        if (fragmentManager.findFragmentByTag(FRAGMENT_MAIN_NAME) != null) {
            ((FragmentMain) fragmentManager.findFragmentByTag(FRAGMENT_MAIN_NAME)).updateLastReportInfo();
        }
    }

    public void cancelTimer(int messageType) {
        Intent intent = new Intent(this, TimerReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                messageType == MESSAGE_TYPE_START ? ALARM_REQUEST_CODE_START : ALARM_REQUEST_CODE_END,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(pendingIntent);
        PrefsUtils.setIsTimer(this, messageType, false);
        PrefsUtils.setLastReportTime(this, -1, messageType);

        if (fragmentManager.findFragmentByTag(FRAGMENT_TIMER_NAME) != null) {
            ((FragmentTimer) fragmentManager.findFragmentByTag(FRAGMENT_TIMER_NAME)).updateBtnCancelTimer(messageType);
            ((FragmentTimer) fragmentManager.findFragmentByTag(FRAGMENT_TIMER_NAME)).updateNextTimerInfo(messageType);
        }

        if (fragmentManager.findFragmentByTag(FRAGMENT_MAIN_NAME) != null) {
            ((FragmentMain) fragmentManager.findFragmentByTag(FRAGMENT_MAIN_NAME)).updateLastReportInfo();
        }
    }

    private void checkSettings() {
        boolean result = true;
        loadSettings();

        if (settings.getStartMessage().equals("")) result = false;
        if (settings.getEndMessage().equals("")) result = false;

        if (!result) {
            AlertDialog.Builder dlgAlert = new AlertDialog.Builder(MainActivity.this);
            dlgAlert.setTitle("Upozornění");
            dlgAlert.setMessage("Aplikace není správně nastaven! Otevřít nastavení?");

            dlgAlert.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int whichButton) {
                    showFragment(FRAGMENT_SETTINGS_NAME);
                }
            }).setNegativeButton("Zavřít", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

            dlgAlert.show();
        }
    }

    public String getPhoneNumber() {
        loadSettings();

        if (settings.getPhoneNumber().equals("")) return PHONE_NUMBER;
        else return settings.getPhoneNumber();
    }

    public String getMessage(int messageType) {
        loadSettings();

        if (messageType == MESSAGE_TYPE_START) {
            if (settings.getStartMessage() == null) return null;
            if (settings.getStartMessage().equals("")) return null;
            return settings.getStartMessage();
        } else if (messageType == MESSAGE_TYPE_END) {
            if (settings.getEndMessage() == null) return null;
            if (settings.getEndMessage().equals("")) return null;
            return settings.getEndMessage();
        }

        return null;
    }

    public boolean checkSmsPermission() {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.SEND_SMS) != PackageManager.PERMISSION_GRANTED) {
                return true;
            } else {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.SEND_SMS},
                        SMS_PERMISSION_REQUEST);

                return false;
            }
        } else {
            return true;
        }

    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case SMS_PERMISSION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else {

                    return;
                }
            }
        }

    }

    public Settings loadSettings() {
        settings = PrefsUtils.getSettings(this);
        return settings;
    }

    public Settings getSettings() {
        return settings;
    }

    private void initStetho() {
        Stetho.InitializerBuilder initializerBuilder = Stetho.newInitializerBuilder(this);
        initializerBuilder.enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this));
        initializerBuilder.enableDumpapp(Stetho.defaultDumperPluginsProvider(this));
        Stetho.Initializer initializer = initializerBuilder.build();
        Stetho.initialize(initializer);
    }
}
