package cz.stodva.hlaseninastupu;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentTransaction;

import android.Manifest;
import android.app.AlarmManager;
import android.app.DownloadManager;
import android.app.PendingIntent;
import android.content.ActivityNotFoundException;
import android.content.BroadcastReceiver;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.ContactsContract;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.facebook.stetho.Stetho;

import java.io.File;
import java.util.Calendar;

import cz.stodva.hlaseninastupu.customviews.DialogInfo;
import cz.stodva.hlaseninastupu.customviews.DialogYesNo;
import cz.stodva.hlaseninastupu.fragments.FragmentMain;
import cz.stodva.hlaseninastupu.fragments.FragmentSettings;
import cz.stodva.hlaseninastupu.fragments.FragmentTimer;
import cz.stodva.hlaseninastupu.listeners.YesNoSelectedListener;
import cz.stodva.hlaseninastupu.objects.AppSettings;
import cz.stodva.hlaseninastupu.receivers.TimerReceiver;
import cz.stodva.hlaseninastupu.utils.Animators;
import cz.stodva.hlaseninastupu.utils.AppConstants;
import cz.stodva.hlaseninastupu.utils.AppUtils;
import cz.stodva.hlaseninastupu.utils.PrefsUtils;


public class MainActivity extends AppCompatActivity implements
        AppConstants,
        FragmentManager.OnBackStackChangedListener {

    int animShowFragment = R.anim.anim_fragment_show;
    int animHideFragment = R.anim.anim_fragment_hide;

    RequestQueue queue;

    public FragmentManager fragmentManager;
    public FragmentTimer fragmentTimer;
    public FragmentMain fragmentMain;
    public FragmentSettings fragmentSettings;

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

    TextView labelToolbar;
    ImageView imgToolbar;

    private BroadcastReceiver reportResultBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "(101) MainActivity - reportResultBroadcastReceiver - onReceive");

            Log.d(LOG_TAG, "(102) ACTION_SMS_SENT");

            int messageType = intent.getIntExtra("message_type", -1);
            int reportType = intent.getIntExtra("report_type", -1);

            Log.d(AppConstants.LOG_TAG, "(103) message type: " + AppUtils.messageTypeToString(messageType));
            Log.d(AppConstants.LOG_TAG, "(104) report type: " + AppUtils.reportTypeToString(reportType));

            if (fragmentMain != null) fragmentMain.updateReportInfo();
        }
    };


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putInt("dayStart", dayStart);
        outState.putInt("monthStart", monthStart);
        outState.putInt("yearStart", yearStart);
        outState.putInt("hoursStart", hoursStart);
        outState.putInt("minutesStart", minutesStart);

        outState.putInt("dayEnd", dayEnd);
        outState.putInt("monthEnd", monthEnd);
        outState.putInt("yearEnd", yearEnd);
        outState.putInt("hoursEnd", hoursEnd);
        outState.putInt("minutesEnd", minutesEnd);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        dayStart = savedInstanceState.getInt("dayStart");
        monthStart = savedInstanceState.getInt("monthStart");
        yearStart = savedInstanceState.getInt("yearStart");
        hoursStart = savedInstanceState.getInt("hoursStart");
        minutesStart = savedInstanceState.getInt("minutesStart");

        dayEnd = savedInstanceState.getInt("dayEnd");
        monthEnd = savedInstanceState.getInt("monthEnd");
        yearEnd = savedInstanceState.getInt("yearEnd");
        hoursEnd = savedInstanceState.getInt("hoursEnd");
        minutesEnd = savedInstanceState.getInt("minutesEnd");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        labelToolbar = findViewById(R.id.labelToolbar);
        imgToolbar = findViewById(R.id.imgToolbar);

        initStetho();

        fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);

        fragmentMain = (FragmentMain) fragmentManager.findFragmentByTag(FRAGMENT_MAIN_NAME);
        fragmentTimer = (FragmentTimer) fragmentManager.findFragmentByTag(FRAGMENT_TIMER_NAME);
        fragmentSettings = (FragmentSettings) fragmentManager.findFragmentByTag(FRAGMENT_SETTINGS_NAME);

        imgToolbar.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animators.animateButtonClick(imgToolbar, true);

                int lastBeIndex = fragmentManager.getBackStackEntryCount() - 1;
                if (fragmentManager.getBackStackEntryAt(lastBeIndex).getName().equals(FRAGMENT_MAIN_NAME)) {
                    showFragment(FRAGMENT_SETTINGS_NAME);
                } else if (fragmentManager.getBackStackEntryAt(lastBeIndex).getName().equals(FRAGMENT_SETTINGS_NAME)) {
                    onBackPressed();
                } else if (fragmentManager.getBackStackEntryAt(lastBeIndex).getName().equals(FRAGMENT_TIMER_NAME)) {
                    onBackPressed();
                }
            }
        });

        if (savedInstanceState == null) {
            showFragment(FRAGMENT_MAIN_NAME);
            checkSettings();
        }

        Intent incommingIntent = getIntent();

        if (incommingIntent != null) {
            // Alarm neodeslaného (nedoručeného) hlášení
            if (incommingIntent.hasExtra("on_error")) {
                Log.d(LOG_TAG_SMS, "(109) MainActivity - ON_ERROR");

                String errMsg = incommingIntent.getStringExtra("error_message");
                int msgType = incommingIntent.getIntExtra("messageType", -1);

                Log.d(LOG_TAG_SMS, "(110) message type : " + AppUtils.messageTypeToString(msgType));
                Log.d(LOG_TAG_SMS, "(111) error message : " + errMsg);

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

        registerReceiver(reportResultBroadcastReceiver, new IntentFilter(ACTION_REPORT_RESULT));

        if (savedInstanceState == null) {
            if (checkWriteStoragePermissionGranted()) {
                checkVersion();
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        onBackStackChanged();
        if (fragmentTimer != null) fragmentTimer.updateViews();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        unregisterReceiver(reportResultBroadcastReceiver);

        if (queue != null) {
            queue.cancelAll(REQUEST_QUEUE_TAG);
        }

    }

    @Override
    public void onBackPressed() {
        if (fragmentManager.getBackStackEntryAt(fragmentManager.getBackStackEntryCount() - 1).getName().equals(FRAGMENT_SETTINGS_NAME)) {
            fragmentSettings = (FragmentSettings) fragmentManager.findFragmentByTag(FRAGMENT_SETTINGS_NAME);

            if (fragmentSettings != null) {
                fragmentSettings.saveData();
                super.onBackPressed();
                return;
            }
        }

        if (fragmentManager.getBackStackEntryCount() == 1) finish();
        else super.onBackPressed();
    }

    @Override
    public void onBackStackChanged() {
        int lastBeIndex = fragmentManager.getBackStackEntryCount() - 1;
        if (fragmentManager.getBackStackEntryAt(lastBeIndex).getName().equals(FRAGMENT_MAIN_NAME)) {
            updateToolbarText(getString(R.string.app_name));
            updateToolbarImage(R.drawable.ic_settings_white);
        } else if (fragmentManager.getBackStackEntryAt(lastBeIndex).getName().equals(FRAGMENT_SETTINGS_NAME)) {
            updateToolbarText(FRAGMENT_SETTINGS);
            updateToolbarImage(R.drawable.ic_back);
        } else if (fragmentManager.getBackStackEntryAt(lastBeIndex).getName().equals(FRAGMENT_TIMER_NAME)) {
            updateToolbarText(FRAGMENT_TIMER);
            updateToolbarImage(R.drawable.ic_back);
        }
    }

    public void updateToolbarText(String text) {
        labelToolbar.setText(text);
    }

    public void updateToolbarImage(@DrawableRes int resId) {
        imgToolbar.setImageResource(resId);
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
            fragmentMain = new FragmentMain();
            return fragmentMain;
        } else if (name.equals(FRAGMENT_TIMER_NAME)) {
            fragmentTimer = new FragmentTimer();
            return fragmentTimer;
        } else if (name.equals(FRAGMENT_SETTINGS_NAME)) {
            fragmentSettings = new FragmentSettings();
            return fragmentSettings;
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

    public boolean isStartTimeSet() {
        return this.hoursStart > 0 && this.minutesStart > 0;
    }

    public boolean isEndTimeSet() {
        return this.hoursEnd > 0 && this.minutesEnd > 0;
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
    public void setTimer(int messageType, int reportType) {

        Log.d(LOG_TAG_SMS, "(112) MainActivity - setTimer(" + AppUtils.messageTypeToString(messageType) + ")");

        Intent intent = new Intent(this, TimerReceiver.class);
        intent.putExtra("message_type", messageType);
        intent.putExtra("report_type", reportType);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                messageType == MESSAGE_TYPE_START ? ALARM_REQUEST_CODE_START : ALARM_REQUEST_CODE_END,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        long time = getTimeInMillis(messageType);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);

        // Nastavení příznaku o nastavení časovače
        PrefsUtils.setIsTimer(this, messageType, true);

        // Uložení času automatického odeslání hlášení
        PrefsUtils.setReportTime(this, time, messageType, REPORT_TYPE_NEXT);

        // Vynulování příznaku o úspěšném odeslání hlášení
        PrefsUtils.saveIsReportSent(this, false, messageType, REPORT_TYPE_NEXT);

        // Vynulování příznaku o úspěšném doručení hlášení
        PrefsUtils.saveIsReportDelivered(this, false, messageType, REPORT_TYPE_NEXT);

        if (fragmentTimer != null) fragmentTimer.updateViews();
        if (fragmentMain != null) fragmentMain.updateReportInfo();
    }

    // Zrušení časovače pro odeslání hlášení
    public void cancelTimer(int messageType) {

        Log.d(LOG_TAG_SMS, "(113) MainActivity - cancelTimer(" + AppUtils.messageTypeToString(messageType) + ")");

        Intent intent = new Intent(this, TimerReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                messageType == MESSAGE_TYPE_START ? ALARM_REQUEST_CODE_START : ALARM_REQUEST_CODE_END,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(pendingIntent);

        PrefsUtils.setIsTimer(this, messageType, false);
        PrefsUtils.setReportTime(this, PREFS_DELETE_KEY, messageType, REPORT_TYPE_NEXT);

        // Vynulování příznaků o odeslání a doručení hlášení
        PrefsUtils.saveIsReportSent(this, false, messageType, REPORT_TYPE_NEXT);
        PrefsUtils.saveIsReportDelivered(this, false, messageType, REPORT_TYPE_NEXT);

        // Zrušení alarmů při neodeslání a nedoručení hlášení
        PrefsUtils.setAlarm(this, false, messageType, ALARM_TYPE_NO_SENT);
        PrefsUtils.setAlarm(this, false, messageType, ALARM_TYPE_NO_DELIVERED);

        if (fragmentTimer != null) fragmentTimer.updateViews();
        if (fragmentMain != null) fragmentMain.updateReportInfo();
    }

    // Nastavení časovače pro kontrolu odeslání a doručení následujícího hlášení
    public void setTimerForError(int messageType, int errorType) {

        Log.d(LOG_TAG_SMS, "(114) MainActivity - setTimerForError(" + AppUtils.messageTypeToString(messageType) + ", " + AppUtils.errorTypeToString(errorType) + ")");

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
        am.set(AlarmManager.RTC_WAKEUP, getTimeForErrorAlarm(messageType, REPORT_TYPE_NEXT), pendingIntent);
    }

    public void cancelTimerForError(int messageType) {

        Log.d(LOG_TAG_SMS, "(115) MainActivity - cancelTimerForError(" + AppUtils.messageTypeToString(messageType) + ")");

        Intent intent = new Intent(this, TimerReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                ALARM_REQUEST_ERROR,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(pendingIntent);

        PrefsUtils.setAlarm(this, false, messageType, ALARM_TYPE_NO_SENT);
        PrefsUtils.setAlarm(this, false, messageType, ALARM_TYPE_NO_DELIVERED);
    }

    public long getTimeForErrorAlarm(int messageType, int reportType) {
        return PrefsUtils.getReportTime(this, messageType, reportType) + 20000;//300000;
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

                        @Override
                        public void noSelected() {
                        }
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
        if (PrefsUtils.isDefinitiveRejection(this, PERMISSION_SMS)) {
            showDialogSettings();
            return false;
        }

        if (checkSelfPermission(Manifest.permission.SEND_SMS) == PackageManager.PERMISSION_GRANTED) {
            // Udělení oprávnění uděleno již dříve
            //Toast.makeText(this, R.string.permission_already_granted, Toast.LENGTH_SHORT).show();
            return true;
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
            // Udělení oprávnění již bylo odmítnuto (nezaškrtnuto políčko "Příště se neptat")
            showDialogExplanation(PERMISSION_SMS);
            return false;
        } else {
            // Zobrazení informačního doalogu před zobrazením systémové žádosti
            showDialogInfo(Manifest.permission.SEND_SMS, getString(R.string.permission_sms_info));
            return false;
        }
    }

    public boolean checkWriteStoragePermissionGranted() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;

        // Bylo již udělení oprávnění definitivně odepřeno?
        if (PrefsUtils.isDefinitiveRejection(this, PERMISSION_WRITE_EXTERNAL_STORAGE)) {
            showDialogSettings();
            return false;
        }

        if (checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {
            // Udělení oprávnění uděleno již dříve
            //Toast.makeText(this, R.string.permission_already_granted, Toast.LENGTH_SHORT).show();
            return true;
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            // Udělení oprávnění již bylo odmítnuto (nezaškrtnuto políčko "Příště se neptat")
            showDialogExplanation(PERMISSION_WRITE_EXTERNAL_STORAGE);
            return false;
        } else {
            // Zobrazení informačního doalogu před zobrazením systémové žádosti
            showDialogInfo(Manifest.permission.WRITE_EXTERNAL_STORAGE, getString(R.string.permission_storage_info));
            return false;
        }
    }

    public boolean checkReadContactsPermissionGranted() {

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) return true;

        // Bylo již udělení oprávnění definitivně odepřeno?
        if (PrefsUtils.isDefinitiveRejection(this, PERMISSION_READ_CONTACTS)) {
            showDialogSettings();
            return false;
        }

        if (checkSelfPermission(Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED) {
            // Udělení oprávnění uděleno již dříve
            //Toast.makeText(this, R.string.permission_already_granted, Toast.LENGTH_SHORT).show();
            return true;
        } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
            // Udělení oprávnění již bylo odmítnuto (nezaškrtnuto políčko "Příště se neptat")
            showDialogExplanation(PERMISSION_READ_CONTACTS);
            return false;
        } else {
            // Zobrazení informačního doalogu před zobrazením systémové žádosti
            showDialogInfo(Manifest.permission.READ_CONTACTS, getString(R.string.permission_read_contacts_info));
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

                    @Override
                    public void noSelected() {
                        return;
                    }
                }).show();
    }

    // Zobrazení dialogového okna s vysvětlením důvodu potřeby udělení oprávnění před zobrazením
    // systémového dialogového okna s žádostí a s možností zaškrtnutí políčka "Příště se neptat".
    public void showDialogExplanation(final int permissionType) {
        String message = "";

        if (permissionType == PERMISSION_SMS) message = getResources().getString(R.string.permission_explanation_sms);
        if (permissionType == PERMISSION_WRITE_EXTERNAL_STORAGE) message = getResources().getString(R.string.permission_explanation_write_external_storage);
        if (permissionType == PERMISSION_READ_CONTACTS) message = getResources().getString(R.string.permission_explanation_read_contacts_storage);

        DialogYesNo.createDialog(this)
                .setTitle("Vysvětlení")
                .setMessage(message)
                .setListener(new YesNoSelectedListener() {
                    @Override
                    public void yesSelected() {
                        if (permissionType == PERMISSION_SMS) {
                            ActivityCompat.requestPermissions(
                                    MainActivity.this,
                                    new String[]{Manifest.permission.SEND_SMS},
                                    SMS_PERMISSION_REQUEST);
                        }

                        if (permissionType == PERMISSION_WRITE_EXTERNAL_STORAGE) {
                            ActivityCompat.requestPermissions(
                                    MainActivity.this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST);
                        }

                        if (permissionType == PERMISSION_READ_CONTACTS) {
                            ActivityCompat.requestPermissions(
                                    MainActivity.this,
                                    new String[]{Manifest.permission.READ_CONTACTS},
                                    READ_CONTACTS_PERMISSION_REQUEST);
                        }
                    }

                    @Override
                    public void noSelected() {
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
                        }

                        if (permission.equals(Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                            ActivityCompat.requestPermissions(
                                    MainActivity.this,
                                    new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
                                    WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST);
                        }

                        if (permission.equals(Manifest.permission.READ_CONTACTS)) {
                            ActivityCompat.requestPermissions(
                                    MainActivity.this,
                                    new String[]{Manifest.permission.READ_CONTACTS},
                                    READ_CONTACTS_PERMISSION_REQUEST);
                        }
                    }

                    @Override
                    public void noSelected() {
                        return;
                    }
                }).show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        switch (requestCode) {
            case SMS_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.SEND_SMS)) {
                    //Vždy když uživatel odmítne udělit oprávnění a NEZAŠKRTNE políčko "Příště se neptat"
                } else {
                    //Uživatel OPAKOVANĚ odmítl udělit oprávnění a zaškrtl políčko "Příště se neptat"
                    PrefsUtils.setDefinitiveRejection(MainActivity.this, PERMISSION_SMS, true);
                }
                break;
            case WRITE_EXTERNAL_STORAGE_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    checkVersion();
                } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
                    //Vždy když uživatel odmítne udělit oprávnění a NEZAŠKRTNE políčko "Příště se neptat"
                } else {
                    //Uživatel OPAKOVANĚ odmítl udělit oprávnění a zaškrtl políčko "Příště se neptat"
                    PrefsUtils.setDefinitiveRejection(MainActivity.this, PERMISSION_WRITE_EXTERNAL_STORAGE, true);
                }
                break;
            case READ_CONTACTS_PERMISSION_REQUEST:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                } else if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_CONTACTS)) {
                    //Vždy když uživatel odmítne udělit oprávnění a NEZAŠKRTNE políčko "Příště se neptat"
                } else {
                    //Uživatel OPAKOVANĚ odmítl udělit oprávnění a zaškrtl políčko "Příště se neptat"
                    PrefsUtils.setDefinitiveRejection(MainActivity.this, PERMISSION_READ_CONTACTS, true);
                }
                break;
        }

    }

    public AppSettings getAppSettings() {
        appSettings = PrefsUtils.getAppSettings(this);
        return appSettings;
    }

    public int getDayStart() {
        return dayStart;
    }

    public int getMonthStart() {
        return monthStart;
    }

    public int getYearStart() {
        return yearStart;
    }

    public int getHoursStart() {
        return hoursStart;
    }

    public int getMinutesStart() {
        return minutesStart;
    }

    public int getDayEnd() {
        return dayEnd;
    }

    public int getMonthEnd() {
        return monthEnd;
    }

    public int getYearEnd() {
        return yearEnd;
    }

    public int getHoursEnd() {
        return hoursEnd;
    }

    public int getMinutesEnd() {
        return minutesEnd;
    }

    private void initStetho() {
        Stetho.InitializerBuilder initializerBuilder = Stetho.newInitializerBuilder(this);
        initializerBuilder.enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this));
        initializerBuilder.enableDumpapp(Stetho.defaultDumperPluginsProvider(this));
        Stetho.Initializer initializer = initializerBuilder.build();
        Stetho.initialize(initializer);
    }

    public void checkVersion() {
        Log.d("SGSGSGS", "checkVersion()");

        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://stodva.cz/Hlaseni/index.php?version_check=1.01";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.d("SGSGSGS", "onResponse()");
                        Log.d("SGSGSGS", "response: " + response);

                        if (!response.equals(getAppVersion())) {
                            Log.d("SGSGSGS", "K dispozici je novější verze aplikace.");

                            DialogYesNo.createDialog(MainActivity.this)
                                    .setTitle("Aktualizace")
                                    .setMessage("K dispozici je aktualiuace aplikace. Stáhnout a nainstalovat novou verzi?")
                                    .setListener(new YesNoSelectedListener() {
                                        @Override
                                        public void yesSelected() {
                                            downloadApp();
                                        }

                                        @Override public void noSelected() {}
                                    })
                                    .show();
                        } else {
                            Log.d("SGSGSGS", "Je nainstalovaná poslední verze.");
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("SGSGSGS", "onErrorResponse()");
                Log.d("SGSGSGS", "error: " + error.getMessage());
            }
        });

        stringRequest.setTag(REQUEST_QUEUE_TAG);
        queue.add(stringRequest);
    }

    public static void installApplication(Context context, String filePath) {
        Log.d("SGSGSGS", "installApplication()");

        Intent intent = new Intent(Intent.ACTION_VIEW);
        //intent.setDataAndType(Uri.fromFile(new File(filePath)), "application/vnd.android.package-archive");
        intent.setDataAndType(uriFromFile(context, new File(filePath)), "application/vnd.android.package-archive");
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

        try {
            context.startActivity(intent);
        } catch (ActivityNotFoundException e) {
            e.printStackTrace();
            Log.e("TAG", "Error in opening the file!");
        }
    }

    private static Uri uriFromFile(Context context, File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            return FileProvider.getUriForFile(context, "cz.stodva.hlaseninastupu.fileprovider", file);
        } else {
            return Uri.fromFile(file);
        }
    }

    public void downloadApp() {
        Log.d("SGSGSGS", "downloadApp()");

        String fileName = "hlaseni_app.apk";
        final String destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + fileName;
        final Uri uri = Uri.parse("file://" + destination);

        File file = new File(destination);
        if (file.exists()) file.delete();

        String url = "http://stodva.cz/Hlaseni/app.apk";

        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setDescription("description");
        request.setTitle("title");
        request.setDestinationUri(uri);

        final DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        final long downloadId = manager.enqueue(request);

        BroadcastReceiver onComplete = new BroadcastReceiver() {
            public void onReceive(Context ctxt, Intent intent) {
                installApplication(MainActivity.this, destination);
                unregisterReceiver(this);
                finish();
            }
        };

        registerReceiver(onComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
    }

    public String getAppVersion() {
        Log.d("SGSGSGS", "getAppVersion()");

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            Log.d("SGSGSGS", "version: " + pInfo.versionName);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public void selectContact() {
        Intent pickContact = new Intent(Intent.ACTION_PICK, ContactsContract.Contacts.CONTENT_URI);
        pickContact.setType(ContactsContract.CommonDataKinds.Phone.CONTENT_TYPE);
        startActivityForResult(pickContact, REQUEST_SELECT_CONTACT);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK) {
            Uri uri = data.getData();
            Cursor cursor;
            ContentResolver cr = getContentResolver();

            try {
                Cursor cur = cr.query(ContactsContract.Contacts.CONTENT_URI, null, null, null, null);
                if (null != cur && cur.getCount() > 0) {
                    cur.moveToFirst();
                }

                if (cur.getCount() > 0) {
                    cursor = getContentResolver().query(uri, null, null, null, null);
                    if (null != cursor && cursor.getCount() > 0) {
                        cursor.moveToFirst();
                        String name = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));
                        String phoneNo = cursor.getString(cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));

                        if (fragmentSettings != null) {
                            fragmentSettings.setContact(phoneNo, name);
                        }
                    }
                }
            } catch (IndexOutOfBoundsException e) {
                e.printStackTrace();
            }
        }
    }
}