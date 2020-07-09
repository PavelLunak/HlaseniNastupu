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
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import cz.stodva.hlaseninastupu.customviews.DialogInfo;
import cz.stodva.hlaseninastupu.customviews.DialogYesNo;
import cz.stodva.hlaseninastupu.database.DataSource;
import cz.stodva.hlaseninastupu.database.DbHelper;
import cz.stodva.hlaseninastupu.fragments.FragmentItems;
import cz.stodva.hlaseninastupu.fragments.FragmentMain;
import cz.stodva.hlaseninastupu.fragments.FragmentSettings;
import cz.stodva.hlaseninastupu.fragments.FragmentTimer;
import cz.stodva.hlaseninastupu.listeners.OnDatabaseClearedListener;
import cz.stodva.hlaseninastupu.listeners.OnItemsLoadedListener;
import cz.stodva.hlaseninastupu.listeners.OnReportAddedListener;
import cz.stodva.hlaseninastupu.listeners.OnReportLoadedListener;
import cz.stodva.hlaseninastupu.listeners.OnReportUpdatedListener;
import cz.stodva.hlaseninastupu.listeners.YesNoSelectedListener;
import cz.stodva.hlaseninastupu.objects.AppSettings;
import cz.stodva.hlaseninastupu.objects.Report;
import cz.stodva.hlaseninastupu.objects.TimerRequestCodeGenerator;
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
    public FragmentItems fragmentItems;

    int day = -1;
    int month = -1;
    int year = -1;
    int hours = -1;
    int minutes = -1;

    public ArrayList<Report> items;
    public Report actualReport;

    MediaPlayer mediaPlayer;
    AppSettings appSettings;
    TimerRequestCodeGenerator timerRequestCodeGenerator;
    DataSource dataSource;

    TextView labelToolbar;
    ImageView imgToolbar, imgItems, imgClearDatabase;

    private BroadcastReceiver reportResultBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.d(LOG_TAG, "(101) MainActivity - reportResultBroadcastReceiver - onReceive");

            Log.d(LOG_TAG, "(102) ACTION_REPORT_RESULT");

            int reportId = intent.getIntExtra("report_id", -1);
            Log.d(AppConstants.LOG_TAG, "(103) report id: " + reportId);

            getDataSource().getAllItems(new OnItemsLoadedListener() {
                @Override
                public void onItemsLoaded(ArrayList<Report> loadedItems) {
                    items = new ArrayList<>(loadedItems);
                    if (fragmentItems != null) fragmentItems.getAdapter().notifyDataSetChanged();
                    if (fragmentMain != null) fragmentMain.updateLastReportInfo();
                }
            });
        }
    };


    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putParcelable("actualReport", actualReport);

        outState.putInt("dayStart", day);
        outState.putInt("monthStart", month);
        outState.putInt("yearStart", year);
        outState.putInt("hoursStart", hours);
        outState.putInt("minutesStart", minutes);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        actualReport = savedInstanceState.getParcelable("actualReport");

        day = savedInstanceState.getInt("dayStart");
        month = savedInstanceState.getInt("monthStart");
        year = savedInstanceState.getInt("yearStart");
        hours = savedInstanceState.getInt("hoursStart");
        minutes = savedInstanceState.getInt("minutesStart");
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        labelToolbar = findViewById(R.id.labelToolbar);
        imgToolbar = findViewById(R.id.imgToolbar);
        imgItems = findViewById(R.id.imgItems);
        imgClearDatabase = findViewById(R.id.imgClearDatabase);

        initStetho();

        fragmentManager = getSupportFragmentManager();
        fragmentManager.addOnBackStackChangedListener(this);

        fragmentMain = (FragmentMain) fragmentManager.findFragmentByTag(FRAGMENT_MAIN_NAME);
        fragmentTimer = (FragmentTimer) fragmentManager.findFragmentByTag(FRAGMENT_TIMER_NAME);
        fragmentSettings = (FragmentSettings) fragmentManager.findFragmentByTag(FRAGMENT_SETTINGS_NAME);
        fragmentItems = (FragmentItems) fragmentManager.findFragmentByTag(FRAGMENT_ITEMS_NAME);

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
                } else if (fragmentManager.getBackStackEntryAt(lastBeIndex).getName().equals(FRAGMENT_ITEMS_NAME)) {
                    onBackPressed();
                }
            }
        });

        imgItems.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animators.animateButtonClick(imgItems, true);
                showFragment(FRAGMENT_ITEMS_NAME);
            }
        });

        imgClearDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // TODO při odstraňování zrušit i nastavená hlášení!!!! DODĚLAT!!!!

                DialogYesNo.createDialog(MainActivity.this)
                        .setTitle("")
                        .setMessage("Odstraněním všech hlášení budou zrušena i neodeslaná hlášení. Opravdu vymazat všechna hlášení z databáze?")
                        .setListener(new YesNoSelectedListener() {
                            @Override
                            public void yesSelected() {
                                getDataSource().clearTable(new OnDatabaseClearedListener() {
                                    @Override
                                    public void onDatabaseCleared() {
                                        getDataSource().getAllItems(new OnItemsLoadedListener() {
                                            @Override
                                            public void onItemsLoaded(ArrayList<Report> loadedItems) {
                                                items = new ArrayList<>(loadedItems);
                                                if (fragmentItems != null) fragmentItems.getAdapter().notifyDataSetChanged();
                                                if (fragmentMain != null) fragmentMain.updateLastReportInfo();
                                            }
                                        });
                                    }
                                });
                            }

                            @Override public void noSelected() {}
                        })
                        .show();
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

                final int reportId = incommingIntent.getIntExtra("report_id", -1);

                Log.d(LOG_TAG_SMS, "(110) report id : " + reportId);

                getDataSource().getReportById(reportId, new OnReportLoadedListener() {
                    @Override
                    public void onReportLoaded(Report loadedReport) {
                        if (loadedReport == null) return;

                        if (loadedReport.getMessage() != null) {
                            getDataSource().getReportById(reportId, new OnReportLoadedListener() {
                                @Override
                                public void onReportLoaded(Report loadedReport) {

                                    mediaPlayer = MediaPlayer.create(MainActivity.this, R.raw.ha);
                                    mediaPlayer.start();

                                    DialogInfo.createDialog(MainActivity.this)
                                            .setTitle(AppUtils.timeToString(loadedReport.getTime()))
                                            .setMessage(loadedReport.getMessage())
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
                            });
                        }
                    }
                });
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

        getDataSource().getAllItems(new OnItemsLoadedListener() {
            @Override
            public void onItemsLoaded(ArrayList<Report> loadedItems) {
                items = new ArrayList<>(loadedItems);

                if (fragmentItems != null) {
                    fragmentItems.updateAdapter();
                }
            }
        });
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
            imgItems.setVisibility(View.VISIBLE);
        } else if (fragmentManager.getBackStackEntryAt(lastBeIndex).getName().equals(FRAGMENT_SETTINGS_NAME)) {
            updateToolbarText(FRAGMENT_SETTINGS);
            updateToolbarImage(R.drawable.ic_back);
            imgItems.setVisibility(View.GONE);
        } else if (fragmentManager.getBackStackEntryAt(lastBeIndex).getName().equals(FRAGMENT_TIMER_NAME)) {
            updateToolbarText(FRAGMENT_TIMER);
            updateToolbarImage(R.drawable.ic_back);
            imgItems.setVisibility(View.GONE);
        } else if (fragmentManager.getBackStackEntryAt(lastBeIndex).getName().equals(FRAGMENT_ITEMS_NAME)) {
            updateToolbarText(FRAGMENT_ITEMS);
            updateToolbarImage(R.drawable.ic_back);
            imgItems.setVisibility(View.GONE);
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
        } else if (name.equals(FRAGMENT_ITEMS_NAME)) {
            fragmentItems = new FragmentItems();
            return fragmentItems;
        }

        return null;
    }

    public void setTimeData(int hours, int minutes) {
        this.hours = hours;
        this.minutes = minutes;
    }

    public boolean isTimeSet() {
        return this.hours >= 0 && this.minutes >= 0;
    }

    public void setDateData(int day, int month, int year, int messageType) {
        this.day = day;
        this.month = month;
        this.year = year;
    }

    public boolean checkTimeInput() {
        Log.d(LOG_TAG, "MainActivity - checkTimeInput()" + LOG_UNDERLINED);
        if (hours < 0 || minutes < 0) {
            Toast.makeText(this, "Není nastaven čas odeslání!", Toast.LENGTH_LONG).show();
            Log.d(LOG_TAG, LOG_TAB + "FALSE");
            return false;
        }

        Log.d(LOG_TAG, LOG_TAB + "TRUE");
        return true;
    }

    public long getTimeInMillis() {
        Calendar calendar = Calendar.getInstance();
        calendar.set(year, month, day, hours, minutes, 0);
        return calendar.getTimeInMillis();
    }

    // Nastavení časovače pro odeslání hlášení
    public void setTimer(final long time, final int reportId) {

        Log.d(LOG_TAG_SMS, "(112) MainActivity - setTimer()");

        // Obnovení seznamu hlášení z databáze
        getDataSource().getAllItems(new OnItemsLoadedListener() {
            @Override
            public void onItemsLoaded(ArrayList<Report> loadedItems) {
                items = new ArrayList<>(loadedItems);

                if (fragmentTimer != null) fragmentTimer.updateViews();
                if (fragmentMain != null) fragmentMain.updateLastReportInfo();

                Intent intent = new Intent(MainActivity.this, TimerReceiver.class);
                intent.putExtra("report_id", reportId);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        MainActivity.this,
                        actualReport.getAlarmRequestCode(),
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

                AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
                am.set(AlarmManager.RTC_WAKEUP, time, pendingIntent);
            }
        });
    }

    // Zrušení časovače pro odeslání hlášení
    public void cancelTimer(final Report report) {

        Log.d(LOG_TAG_SMS, "(113) MainActivity - cancelTimer()");

        Intent intent = new Intent(this, TimerReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                report.getAlarmRequestCode(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(pendingIntent);

        if (report.getId() != NONE) {
            getDataSource().updateReportValue(
                    report.getId(),
                    DbHelper.COLUMN_REQUEST_CODE,
                    NONE,
                    new OnReportUpdatedListener() {
                        @Override
                        public void onReportUpdated(Report updatedReport) {
                            report.setAlarmRequestCode(NONE);
                            if (fragmentItems != null) fragmentItems.getAdapter().notifyDataSetChanged();
                        }
                    });

            getDataSource().updateReportValue(
                    report.getId(),
                    DbHelper.COLUMN_TIME,
                    CANCELED,
                    new OnReportUpdatedListener() {
                        @Override
                        public void onReportUpdated(Report updatedReport) {
                            report.setTime(CANCELED);
                            if (fragmentItems != null) fragmentItems.getAdapter().notifyDataSetChanged();
                        }
                    });

            getDataSource().updateReportValue(
                    report.getId(),
                    DbHelper.COLUMN_SENT,
                    NONE,
                    new OnReportUpdatedListener() {
                        @Override
                        public void onReportUpdated(Report updatedReport) {
                            report.setSentTime(NONE);
                            if (fragmentItems != null) fragmentItems.getAdapter().notifyDataSetChanged();
                        }
                    });

            getDataSource().updateReportValue(
                    report.getId(),
                    DbHelper.COLUMN_DELIVERED,
                    NONE,
                    new OnReportUpdatedListener() {
                        @Override
                        public void onReportUpdated(Report updatedReport) {
                            report.setDeliveryTime(NONE);
                            if (fragmentItems != null) fragmentItems.getAdapter().notifyDataSetChanged();
                        }
                    });
        }

        if (fragmentTimer != null) fragmentTimer.updateViews();
        if (fragmentMain != null) fragmentMain.updateLastReportInfo();
    }

    // Nastavení časovače pro kontrolu odeslání a doručení následujícího hlášení
    public void setTimerForError(final Report report) {

        Log.d(LOG_TAG_SMS, "(114) MainActivity - setTimerForError()");

        // Pokud má hlášení ID, je již uložen v databázi a je možné ho tam aktualizovat. V opačném
        // případě jde o nové hlášení, které bude do databáze teprve přidáno
        if (report.getId() != NONE) {
            getDataSource().updateReportValue(
                    report.getId(),
                    DbHelper.COLUMN_ERROR_REQUEST_CODE,
                    report.getRequestCodeForErrorAlarm(),
                    null);
        }

        Intent intent = new Intent(MainActivity.this, TimerReceiver.class);
        intent.putExtra("report_id", report.getId());

        //Příznak pro Receiver, že jde o budík pro kontrolu odeslání nebo doručení hlášení
        intent.putExtra("alarm_check_error", 1);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                MainActivity.this,
                report.getRequestCodeForErrorAlarm(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.set(
                AlarmManager.RTC_WAKEUP,
                report.getTime() + 20000,//getTimeForErrorAlarm(messageType, REPORT_TYPE_NEXT),
                pendingIntent);
    }

    public void cancelTimerForError(final Report report) {

        Log.d(LOG_TAG_SMS, "(115) MainActivity - cancelTimerForError()");

        Intent intent = new Intent(this, TimerReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                report.getRequestCodeForErrorAlarm(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(pendingIntent);

        // Pokud má hlášení ID, je již uložen v databázi a je možné ho tam aktualizovat. V opačném
        // případě jde o nové hlášení, které bude do databáze teprve přidáno. Aktualizace v databázi
        // (a změna requestCode pro alarm erroru na NONE) musí být až za zrušením alarmu.
        if (report.getId() != NONE) {
            getDataSource().updateReportValue(
                    report.getId(),
                    DbHelper.COLUMN_ERROR_REQUEST_CODE,
                    NONE,
                    new OnReportUpdatedListener() {
                        @Override
                        public void onReportUpdated(Report updatedReport) {
                            report.setRequestCodeForErrorAlarm(NONE);
                        }
                    });
        }
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
        if (appSettings == null) appSettings = PrefsUtils.getAppSettings(this);
        return appSettings;
    }

    public DataSource getDataSource() {
        if (dataSource == null) dataSource = new DataSource(this);
        return dataSource;
    }

    public TimerRequestCodeGenerator getTimerRequestCodeGenerator() {
        if (timerRequestCodeGenerator == null) timerRequestCodeGenerator = new TimerRequestCodeGenerator(this);
        return timerRequestCodeGenerator;
    }

    public int getTimerRequestCode() {
        Log.d(LOG_TAG, "getTimerRequestCode()");
        return getTimerRequestCodeGenerator().getNewRequestCode();
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public int getMonth() {
        return month;
    }

    public void setMonth(int month) {
        this.month = month;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public int getHours() {
        return hours;
    }

    public void setHours(int hours) {
        this.hours = hours;
    }

    public int getMinutes() {
        return minutes;
    }

    public void setMinutes(int minutes) {
        this.minutes = minutes;
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
        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            return pInfo.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            return null;
        }
    }

    public ArrayList<Report> getItems() {
        if (this.items == null) this.items = new ArrayList<>();
        return this.items;
    }

    // Test, zda jde o budoucí čas
    public boolean isInFuture(long time) {
        Log.d(LOG_TAG, "Main - btnOk click" + LOG_UNDERLINED);
        Log.d(LOG_TAG, LOG_TAB + (time > new Date().getTime()));
        return time > new Date().getTime();
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