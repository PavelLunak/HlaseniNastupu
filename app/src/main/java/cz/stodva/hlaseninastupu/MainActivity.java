package cz.stodva.hlaseninastupu;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.core.app.ActivityCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.util.Log;
import android.widget.Toast;

import com.facebook.stetho.Stetho;

import java.util.Calendar;

import cz.stodva.hlaseninastupu.customviews.DialogInfo;
import cz.stodva.hlaseninastupu.customviews.DialogYesNo;
import cz.stodva.hlaseninastupu.fragments.FragmentMain;
import cz.stodva.hlaseninastupu.fragments.FragmentSettings;
import cz.stodva.hlaseninastupu.fragments.FragmentTimer;
import cz.stodva.hlaseninastupu.listeners.YesNoSelectedListener;
import cz.stodva.hlaseninastupu.objects.AppSettings;
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
    AppSettings appSettings;

    private BroadcastReceiver smsSentBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "MainActivity - broadcastReceiver - onReceive");

            if (intent.getAction().equals(ACTION_SMS_SENT)){
                Log.d(LOG_TAG, "ACTION_SMS_SENT");
                Log.d(AppConstants.LOG_TAG, "intent hasExtra: " + intent.getIntExtra("message_type", -1));

                FragmentMain fm = (FragmentMain) fragmentManager.findFragmentByTag(FRAGMENT_MAIN_NAME);
                if (fm != null) fm.updateLastReportInfo();
            }
        }
    };

    private BroadcastReceiver smsDeliveredBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "MainActivity - broadcastReceiver - onReceive");

            if (intent.getAction().equals(ACTION_SMS_DELIVERED)) {
                Log.d(LOG_TAG, "ACTION_SMS_DELIVERED");
                Log.d(AppConstants.LOG_TAG, "intent hasExtra: " + intent.getIntExtra("message_type", -1));

                FragmentMain fm = (FragmentMain) fragmentManager.findFragmentByTag(FRAGMENT_MAIN_NAME);

                if (fm != null) {
                    int msgType = intent.getIntExtra("message_type", -1);

                    if (msgType == MESSAGE_TYPE_START) {
                        fm.getImgResult(R.id.imgDeliveredSart).setImageDrawable(AppCompatResources.getDrawable(MainActivity.this, R.drawable.ic_check_green));
                    } else if (msgType == MESSAGE_TYPE_END) {
                        fm.getImgResult(R.id.imgDeliveredEnd).setImageDrawable(AppCompatResources.getDrawable(MainActivity.this, R.drawable.ic_check_green));
                    } else {
                        Log.d(LOG_TAG, "bad message type");
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
            checkSettings();
        }

        Intent incommingIntent = getIntent();

        if (incommingIntent != null) {
            // Alarm neodeslaného (nedoručeného) hlášení
            if (incommingIntent.hasExtra("on_error")) {
                Log.d(LOG_TAG_SMS, "MainActivity - ON_ERROR");

                String errMsg = incommingIntent.getStringExtra("error_message");
                int msgType = incommingIntent.getIntExtra("messageType", -1);

                Log.d(LOG_TAG_SMS, "message type : " + messageTypeToString(msgType));
                Log.d(LOG_TAG_SMS, "error message : " + errMsg);

                if (errMsg != null) {
                    mediaPlayer = MediaPlayer.create(this, R.raw.ha);
                    mediaPlayer.start();

                    DialogInfo.createDialog(this)
                            .setTitle("POZOR")
                            .setMessage(errMsg)
                            .setListener(new DialogInfo.OnDialogClosedListener() {
                                @Override
                                public void onDialogClosed() {
                                    if (mediaPlayer != null) {
                                        mediaPlayer.stop();
                                        mediaPlayer.release();
                                        mediaPlayer = null;
                                    }
                                }
                            }).show();
                }
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

    // Nastavení časovače pro odeslání hlášení
    public void setTimer(int messageType) {

        Log.d(LOG_TAG_SMS, "MainActivity - setTimer(" + messageTypeToString(messageType) + ")");

        PrefsUtils.saveIsReportSent(this, false, messageType);
        PrefsUtils.saveIsReportDelivered(this, false, messageType);

        Intent intent = new Intent(this, TimerReceiver.class);
        intent.putExtra("message_type", messageType);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                messageType == MESSAGE_TYPE_START ? ALARM_REQUEST_CODE_START : ALARM_REQUEST_CODE_END,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        long time = getTimeInMillis(messageType);
        PrefsUtils.saveTimer(this, messageType, time);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);

        PrefsUtils.setIsTimer(this, messageType, true);
        PrefsUtils.setLastReportTime(this, getTimeInMillis(messageType), messageType);

        if (fragmentManager.findFragmentByTag(FRAGMENT_TIMER_NAME) != null) {
            ((FragmentTimer) fragmentManager.findFragmentByTag(FRAGMENT_TIMER_NAME)).updateLayoutsVisibility();
            ((FragmentTimer) fragmentManager.findFragmentByTag(FRAGMENT_TIMER_NAME)).updateLayoutsVisibility();
        }

        if (fragmentManager.findFragmentByTag(FRAGMENT_MAIN_NAME) != null) {
            ((FragmentMain) fragmentManager.findFragmentByTag(FRAGMENT_MAIN_NAME)).updateLastReportInfo();
        }
    }

    // Zrušení časovače pro odeslání hlášení
    public void cancelTimer(int messageType) {

        Log.d(LOG_TAG_SMS, "MainActivity - cancelTimer(" + messageTypeToString(messageType) + ")");

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

        // Vynulování příznaků o odeslání a doručení hlášení
        PrefsUtils.saveIsReportSent(this, false, messageType);
        PrefsUtils.saveIsReportDelivered(this, false, messageType);

        // Zrušení alarmů při neodeslání a nedoručení hlášení
        PrefsUtils.setNoSentAlarm(this, false, messageType);
        PrefsUtils.setNoDeliveredAlarm(this, false, messageType);

        if (fragmentManager.findFragmentByTag(FRAGMENT_TIMER_NAME) != null) {
            ((FragmentTimer) fragmentManager.findFragmentByTag(FRAGMENT_TIMER_NAME)).updateLayoutsVisibility();
            ((FragmentTimer) fragmentManager.findFragmentByTag(FRAGMENT_TIMER_NAME)).updateLayoutsVisibility();
        }

        if (fragmentManager.findFragmentByTag(FRAGMENT_MAIN_NAME) != null) {
            ((FragmentMain) fragmentManager.findFragmentByTag(FRAGMENT_MAIN_NAME)).updateLastReportInfo();
        }
    }

    // Při neodeslání nebo nedorušení hlášení
    public void setTimerForError(int messageType, int errorType) {

        Log.d(LOG_TAG_SMS, "MainActivity - setTimerForError(" + messageTypeToString(messageType) + ", " + errorTypeToString(errorType) + ")");

        Intent intent = new Intent(this, TimerReceiver.class);
        intent.putExtra("message_type", messageType);
        intent.putExtra("error_type", errorType);

        //Příznak pro Receiver, že jde o budík pro kontrolu odeslání nebo doručení hlášení
        intent.putExtra("alarm_check_error", 1);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                ALARM_REQUEST_ERROR,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, getTimeForErrorAlarm(messageType), pendingIntent);
    }

    public void cancelTimerForError(int messageType) {

        Log.d(LOG_TAG_SMS, "MainActivity - cancelTimerForError(" + messageTypeToString(messageType) + ")");

        Intent intent = new Intent(this, TimerReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                ALARM_REQUEST_ERROR,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(pendingIntent);

        PrefsUtils.setNoSentAlarm(this, false, messageType);
        PrefsUtils.setNoDeliveredAlarm(this, false, messageType);
    }

    public long getTimeForErrorAlarm(int messageType) {
        return PrefsUtils.getTimer(this, messageType) + 20000;//300000;
    }

    private void checkSettings() {
        boolean result = true;
        getAppSettings();

        if (appSettings.getStartMessage().equals("")) result = false;
        if (appSettings.getEndMessage().equals("")) result = false;

        if (!result) {
            DialogYesNo.createDialog(this)
                    .setTitle("Upozornění")
                    .setMessage("Aplikace není správně nastavena! Otevřít nastavení?")
                    .setListener(new YesNoSelectedListener() {
                        @Override
                        public void yesSelected() {
                            showFragment(FRAGMENT_SETTINGS_NAME);
                        }

                        @Override public void noSelected() {}
                    }).show();
        }
    }

    public String getPhoneNumber() {
        getAppSettings();

        if (appSettings.getPhoneNumber().equals("")) return PHONE_NUMBER;
        else return appSettings.getPhoneNumber();
    }

    public String getMessage(int messageType) {
        getAppSettings();

        if (messageType == MESSAGE_TYPE_START) {
            if (appSettings.getStartMessage() == null) return null;
            if (appSettings.getStartMessage().equals("")) return null;
            return appSettings.getStartMessage();
        } else if (messageType == MESSAGE_TYPE_END) {
            if (appSettings.getEndMessage() == null) return null;
            if (appSettings.getEndMessage().equals("")) return null;
            return appSettings.getEndMessage();
        }

        return null;
    }

    public boolean checkSmsPermissionGranted() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;

        // Bylo již udělení oprávnění definitivně odepřeno?
        if (PrefsUtils.isDefinitiveRejection(this)) {
            showDialogSettings();
            return false;
        }

        if (checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            // Udělení oprávnění uděleno již dříve
            Toast.makeText(this, R.string.permission_already_granted, Toast.LENGTH_SHORT).show();
            return true;
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
            // Udělení oprávnění již bylo odmítnuto (nezaškrtnuto políčko "Příště se neptat")
            showDialogExplanation();
            return false;
        } else {
            // Zobrazení informačního doalogu před zobrazením systémové žádosti
            showDialogInfo(Manifest.permission.SEND_SMS, "Pro odesílání SMS bude nutné této aplikaci udělit oprávnění. Pokračovat k udělení oprávnění?");
            return false;
        }
    }

    public void showDialogSettings() {
        DialogYesNo.createDialog(this)
                .setTitle(getResources().getString(R.string.additional_permission))
                .setMessage(getResources().getString(R.string.permissions_settings_info))
                .setListener(new YesNoSelectedListener() {
                    @Override
                    public void yesSelected() {
                        Intent intent = new Intent();
                        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                        Uri uri = Uri.fromParts("package", getPackageName(), null);
                        intent.setData(uri);
                        startActivity(intent);
                    }

                    @Override public void noSelected() {
                        return;
                    }
                }).show();
    }

    // Zobrazení dialogového okna s vysvětlením důvodu potřeby udělení oprávnění před zobrazením
    // systémového dialogového okna s žádostí a s možností zaškrtnutí políčka "Příště se neptat".
    public void showDialogExplanation() {
        DialogYesNo.createDialog(this)
                .setTitle("SMS")
                .setMessage(getResources().getString(R.string.permission_explanation))
                .setListener(new YesNoSelectedListener() {
                    @Override
                    public void yesSelected() {
                        ActivityCompat.requestPermissions(
                                MainActivity.this,
                                new String[]{Manifest.permission.SEND_SMS},
                                SMS_PERMISSION_REQUEST);
                    }

                    @Override public void noSelected() {
                        return;
                    }
                }).show();
    }

    public void showDialogInfo(final String permission, String message) {
        DialogYesNo.createDialog(this)
                .setTitle(getResources().getString(R.string.request_permission))
                .setMessage(message)
                .setListener(new YesNoSelectedListener() {
                    @Override
                    public void yesSelected() {
                        if (permission.equals(Manifest.permission.SEND_SMS)) {

                            ActivityCompat.requestPermissions(
                                    MainActivity.this,
                                    new String[]{Manifest.permission.SEND_SMS},
                                    SMS_PERMISSION_REQUEST);
                        } else {
                            // ... jiné postupy dle typu oprávnění
                        }
                    }

                    @Override public void noSelected() {
                        return;
                    }
                }).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode,String permissions[], int[] grantResults) {
        switch (requestCode) {
            case SMS_PERMISSION_REQUEST: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)) {
                    //Vždy když uživatel odmítne udělit oprávnění a NEZAŠKRTNE políčko "Příště se neptat"
                } else {
                    //Uživatel OPAKOVANĚ odmítl udělit oprávnění a zaškrtl políčko "Příště se neptat"
                    PrefsUtils.setDefinitiveRejection(MainActivity.this, true);
                }
            }
        }

    }

    public AppSettings getAppSettings() {
        appSettings = PrefsUtils.getAppSettings(this);
        return appSettings;
    }

    public String messageTypeToString(int messageType) {
        if (messageType == MESSAGE_TYPE_START) return "MESSAGE_TYPE_START";
        if (messageType == MESSAGE_TYPE_END) return "MESSAGE_TYPE_END";
        if (messageType == MESSAGE_TYPE_BOTH) return "MESSAGE_TYPE_BOTH";
        return "";
    }

    public String errorTypeToString(int errorType) {
        if (errorType == ERROR_TYPE_NO_SENT) return "ERROR_TYPE_NO_SENT";
        if (errorType == ERROR_TYPE_NO_DELIVERED) return "ERROR_TYPE_NO_DELIVERED";
        return "";
    }

    private void initStetho() {
        Stetho.InitializerBuilder initializerBuilder = Stetho.newInitializerBuilder(this);
        initializerBuilder.enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this));
        initializerBuilder.enableDumpapp(Stetho.defaultDumperPluginsProvider(this));
        Stetho.Initializer initializer = initializerBuilder.build();
        Stetho.initialize(initializer);
    }
}
