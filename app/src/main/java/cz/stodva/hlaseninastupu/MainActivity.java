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
import com.google.gson.Gson;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import cz.stodva.hlaseninastupu.customviews.DialogInfo;
import cz.stodva.hlaseninastupu.customviews.DialogSelect;
import cz.stodva.hlaseninastupu.customviews.DialogYesNo;
import cz.stodva.hlaseninastupu.database.DataSource;
import cz.stodva.hlaseninastupu.database.DbHelper;
import cz.stodva.hlaseninastupu.fragments.FragmentItems;
import cz.stodva.hlaseninastupu.fragments.FragmentMain;
import cz.stodva.hlaseninastupu.fragments.FragmentSettings;
import cz.stodva.hlaseninastupu.fragments.FragmentTimer;
import cz.stodva.hlaseninastupu.listeners.OnDatabaseClearedListener;
import cz.stodva.hlaseninastupu.listeners.OnItemDeletedListener;
import cz.stodva.hlaseninastupu.listeners.OnItemsCountCheckedListener;
import cz.stodva.hlaseninastupu.listeners.OnItemsLoadedListener;
import cz.stodva.hlaseninastupu.listeners.OnNewPageLoadedListener;
import cz.stodva.hlaseninastupu.listeners.OnReportAddedListener;
import cz.stodva.hlaseninastupu.listeners.OnReportLoadedListener;
import cz.stodva.hlaseninastupu.listeners.OnReportUpdatedListener;
import cz.stodva.hlaseninastupu.listeners.YesNoSelectedListener;
import cz.stodva.hlaseninastupu.objects.AppSettings;
import cz.stodva.hlaseninastupu.objects.Report;
import cz.stodva.hlaseninastupu.objects.TimerRequestCodeGenerator;
import cz.stodva.hlaseninastupu.objects.VersionResponse;
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

    int pagesCount;
    int itemsCount;
    int page = 1;

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

            updateItems(null);
        }
    };

    public View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
        @Override
        public boolean onLongClick(final View v) {

            // Pokud je tag typu Integer, byla vybrána položka ve FragmentItems
            // Pokud je tag typu Report, byla vybrána položka ve FragmentMain
            final Report reportToUpdate = v.getTag() instanceof Report ? (Report) v.getTag() : items.get((int) v.getTag());
            final String fromFragment = v.getTag() instanceof Report ? FRAGMENT_MAIN_NAME : FRAGMENT_ITEMS_NAME;

            AppUtils.vibrate(MainActivity.this);

            ArrayList<String> items = new ArrayList();
            items.add("Odstranit hlášení");
            items.add("Použít hlášení");

            // Automatické hlášení lze obnovit (čas hlášení je v budoucnu)
            if (reportToUpdate.getSentTime() == CANCELED && reportToUpdate.getTime() > new Date().getTime()) {
                items.add("Obnovit automatické hlášení");
            }

            // Automatické hlášení lze deaktivovat
            if (reportToUpdate.getSentTime() == WAITING) {
                items.add("Zrušit hlášení");
                items.add("Upravit hlášení");
            }

            String[] itemsToArray = items.toArray(new String[items.size()]);

            DialogSelect.createDialog(MainActivity.this)
                    .setTitle("Úprava hlášení...")
                    .setItems(itemsToArray)
                    .setListener(new DialogSelect.OnDialogSelectItemSelectedListener() {
                        @Override
                        public void onDialogSelectItemSelected(String selectedItem) {
                            if (selectedItem.equals("Odstranit hlášení")) {
                                String msg = "Opravdu odstranit tohoto hlášení?";
                                if (reportToUpdate.getSentTime() == WAITING)
                                    msg = "Po odstranění nebude toto hlášení odesláno. Opravdu odstranit tohoto hlášení?";

                                DialogYesNo.createDialog(MainActivity.this)
                                        .setTitle("Odstranění hlášení")
                                        .setMessage(msg)
                                        .setListener(new YesNoSelectedListener() {
                                            @Override
                                            public void yesSelected() {
                                                cancelTimer(reportToUpdate, false);

                                                getDataSource().removeItem(reportToUpdate.getId(), new OnItemDeletedListener() {
                                                    @Override
                                                    public void onItemDeleted() {
                                                        updateItems(null);
                                                    }
                                                });
                                            }

                                            @Override
                                            public void noSelected() {
                                            }
                                        }).show();
                            } else if (selectedItem.equals("Obnovit automatické hlášení")) {
                                setTimerWithErrorCheck(reportToUpdate);
                            } else if (selectedItem.equals("Zrušit hlášení")) {
                                DialogYesNo.createDialog(MainActivity.this)
                                        .setTitle("Zrušení hlášení")
                                        .setMessage("Opravdu zrušit odeslání tohoto hlášení?")
                                        .setListener(new YesNoSelectedListener() {
                                            @Override
                                            public void yesSelected() {
                                                cancelTimer(reportToUpdate, true);
                                            }

                                            @Override
                                            public void noSelected() {
                                            }
                                        }).show();
                            } else if (selectedItem.equals("Upravit hlášení")) {
                                actualReport = reportToUpdate;
                                Bundle args = new Bundle();
                                args.putBoolean("edit", true);
                                showFragment(v.getTag() instanceof Report ? FRAGMENT_MAIN_NAME : FRAGMENT_ITEMS_NAME, args);
                            } else if (selectedItem.equals("Použít hlášení")) {
                                actualReport = reportToUpdate;
                                Bundle args = new Bundle();
                                args.putBoolean("use", true);
                                showFragment(FRAGMENT_TIMER_NAME, args);
                            }
                        }
                    }).show();

            return true;
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

        outState.putInt("pagesCount", pagesCount);
        outState.putInt("itemsCount", itemsCount);
        outState.putInt("page", page);

        //outState.putBoolean("showOnlyWaitingReports", showOnlyWaitingReports);
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

        pagesCount = savedInstanceState.getInt("pagesCount");
        itemsCount = savedInstanceState.getInt("itemsCount");
        page = savedInstanceState.getInt("page");

        //showOnlyWaitingReports = savedInstanceState.getBoolean("showOnlyWaitingReports", false);
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
                    showFragment(FRAGMENT_SETTINGS_NAME, null);
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
                showFragment(FRAGMENT_ITEMS_NAME, null);
            }
        });

        imgClearDatabase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getDataSource().getWaitingItemsCount(new OnItemsCountCheckedListener() {
                    @Override
                    public void onItemsCountChecked(int count) {
                        String message = "Opravdu vymazat historii hlášení z databáze?";

                        if (count > 0) {
                            message += "\n\n";
                            message += "Odstraněním všech hlášení budou zrušena i neodeslaná hlášení (čekajících hlášení: " + count + ").";
                        }

                        DialogYesNo.createDialog(MainActivity.this)
                                .setTitle("")
                                .setMessage(message)
                                .setListener(new YesNoSelectedListener() {
                                    @Override
                                    public void yesSelected() {
                                        getDataSource().getWaitingItems(new OnItemsLoadedListener() {
                                            @Override
                                            public void onItemsLoaded(ArrayList<Report> loadedItems) {
                                                Log.d(LOG_TAG, "ID čekajících hlášení, která budou před smazáním deaktivována:");

                                                for (Report report : loadedItems) {
                                                    Log.d(LOG_TAG, "" + report.getId());
                                                    cancelTimer(report, false);
                                                }

                                                getDataSource().clearTable(new OnDatabaseClearedListener() {
                                                    @Override
                                                    public void onDatabaseCleared() {
                                                        updateItems(null);
                                                    }
                                                });
                                            }
                                        });
                                    }

                                    @Override
                                    public void noSelected() {
                                    }
                                })
                                .show();
                    }
                });
            }
        });

        if (savedInstanceState == null) {
            showFragment(FRAGMENT_MAIN_NAME, null);
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
                                            .setTitle(AppUtils.timeToString(loadedReport.getTime(), REPORT_PHASE_NONE))
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
        updateFragments();
        updateItems(null);
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
                if (fragmentSettings.isBackPressed()) {
                    super.onBackPressed();
                    return;
                }

                fragmentSettings.onBackPressed();
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
            fragmentItems.updatePageInfo();
        }
    }

    public void updateToolbarText(String text) {
        labelToolbar.setText(text);
    }

    public void updateToolbarImage(@DrawableRes int resId) {
        imgToolbar.setImageResource(resId);
    }

    public void showFragment(String name, Bundle args) {
        Fragment fragment = fragmentManager.findFragmentByTag(name);

        if (fragment == null) addFragment(fragment, name, args);
        else restoreFragment(name);
    }

    public void addFragment(Fragment fragment, String name, Bundle args) {
        if (fragment == null) {
            fragment = createFragment(name);
        }

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

    // Přidá do databáze hlášení a aktualizuje seznam stažených hlášení
    public void addReportToDatabase(final Report report, final OnReportAddedListener addListener) {
        getDataSource().addReport(report, new OnReportAddedListener() {
            @Override
            public void onReportAdded(final Report addedReport) {
                updateItems(new OnItemsLoadedListener() {
                    @Override
                    public void onItemsLoaded(ArrayList<Report> loadedItems) {
                        if (addListener != null) addListener.onReportAdded(addedReport);
                    }
                });
            }
        });
    }

    public void updateItems(final OnItemsLoadedListener onItemsLoadedListener) {
        // Získání celkového počtu hlášení v databázi
        getDataSource().getCount(getAppSettings().isShowOnlyActiveReports(), new OnItemsCountCheckedListener() {
            @Override
            public void onItemsCountChecked(int count) {
                itemsCount = count;
                pagesCount = calculatePagesCount();

                if (getAppSettings().isShowOnlyActiveReports()) {
                    getDataSource().getPageWaitingItems(getOffset(), ITEMS_PER_PAGE, new OnItemsLoadedListener() {
                        @Override
                        public void onItemsLoaded(ArrayList<Report> waitingItems) {
                            items = new ArrayList<>(waitingItems);
                            updateFragments();
                            if (onItemsLoadedListener != null)
                                onItemsLoadedListener.onItemsLoaded(items);
                        }
                    });
                } else {
                    getDataSource().getPage(getOffset(), ITEMS_PER_PAGE, new OnItemsLoadedListener() {
                        @Override
                        public void onItemsLoaded(ArrayList<Report> loadedItems) {
                            items = new ArrayList<>(loadedItems);
                            updateFragments();
                            if (onItemsLoadedListener != null)
                                onItemsLoadedListener.onItemsLoaded(items);
                        }
                    });
                }
            }
        });
    }

    public void updateFragments() {
        if (fragmentItems != null) fragmentItems.updateFragment();
        if (fragmentMain != null) fragmentMain.updateInfo();
        if (fragmentTimer != null) fragmentTimer.updateViews();
    }

    public int calculatePagesCount() {
        if (itemsCount <= 0) return 0;

        if (itemsCount % ITEMS_PER_PAGE == 0) return itemsCount / ITEMS_PER_PAGE;
        else return (itemsCount / ITEMS_PER_PAGE) + 1;
    }

    public int getOffset() {
        if (page <= 1) return 0;
        else return (page * ITEMS_PER_PAGE) - ITEMS_PER_PAGE;
    }

    public void pageUp(OnNewPageLoadedListener listener) {
        if (page < pagesCount) {
            page += 1;
            updateItems(null);
            if (listener != null) listener.onNewPageLoaded();
        }
    }

    public void pageDown(OnNewPageLoadedListener listener) {
        if (page > 1) {
            page -= 1;
            updateItems(null);
            if (listener != null) listener.onNewPageLoaded();
        }
    }

    public void setTimeData(int hours, int minutes) {
        this.hours = hours;
        this.minutes = minutes;
    }

    public boolean isTimeSet() {
        return this.hours >= 0 && this.minutes >= 0;
    }

    public void setDateData(int day, int month, int year) {
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
        calendar.set(Calendar.MILLISECOND, 0);
        return calendar.getTimeInMillis();
    }

    // Nastavení časovače pro odeslání hlášení.
    // Včetně časovače pro kontrolu odeslání a doručení
    // Bude použito pouze u hlášení s automatem
    public void setTimerWithErrorCheck(final Report report) {

        Log.d(LOG_TAG_SMS, "(112) MainActivity - setTimer()");

        getDataSource().updateReportValue(
                report.getId(),
                new String[]{
                        DbHelper.COLUMN_SENT,
                        DbHelper.COLUMN_DELIVERED,
                        DbHelper.COLUMN_REQUEST_CODE,
                        DbHelper.COLUMN_ERROR_REQUEST_CODE,
                        DbHelper.COLUMN_IS_AUTOMAT},
                new long[]{
                        WAITING,
                        WAITING,
                        getTimerRequestCode(),
                        getTimerRequestCode(),
                        1},
                new OnReportUpdatedListener() {
                    @Override
                    public void onReportUpdated(final Report updatedReport) {
                        updateItems(new OnItemsLoadedListener() {
                            @Override
                            public void onItemsLoaded(ArrayList<Report> loadedItems) {
                                updateFragments();

                                // NASTAVENÍ ČASOVAČŮ
                                // Časovač pro odeslání hlášení ------------------------------------------------------------
                                Intent intentTimer = new Intent(MainActivity.this, TimerReceiver.class);
                                intentTimer.putExtra("report_id", updatedReport.getId());

                                PendingIntent pendingIntentTimer = PendingIntent.getBroadcast(
                                        MainActivity.this,
                                        updatedReport.getAlarmRequestCode(),
                                        intentTimer,
                                        PendingIntent.FLAG_UPDATE_CURRENT);

                                AlarmManager amTimer = (AlarmManager) getSystemService(ALARM_SERVICE);
                                amTimer.set(
                                        AlarmManager.RTC_WAKEUP,
                                        updatedReport.getTime(),
                                        pendingIntentTimer);

                                // Časovač pro kontrolu doručení hlášení ---------------------------------------------------
                                Intent intentTimerCheckError = new Intent(MainActivity.this, TimerReceiver.class);
                                intentTimerCheckError.putExtra("report_id", updatedReport.getId());

                                //Příznak pro Receiver, že jde o budík pro kontrolu odeslání nebo doručení hlášení
                                intentTimerCheckError.putExtra("alarm_check_error", 1);

                                PendingIntent pendingIntentTimerCheckError = PendingIntent.getBroadcast(
                                        MainActivity.this,
                                        updatedReport.getRequestCodeForErrorAlarm(),
                                        intentTimerCheckError,
                                        PendingIntent.FLAG_UPDATE_CURRENT);

                                AlarmManager amCheckError = (AlarmManager) getSystemService(ALARM_SERVICE);
                                amCheckError.set(
                                        AlarmManager.RTC_WAKEUP,
                                        updatedReport.getTime() + TIME_FOR_CONTROL,
                                        pendingIntentTimerCheckError);
                            }
                        });
                    }
                });
    }

    // Nastavení časovače pro kontrolu odeslání a doručení
    // Bude použito pouze u okamžitých hlášení bez automatu
    public void setOnlyErrorCheckTimer(final Report report) {

        Log.d(LOG_TAG_SMS, "(112_2) MainActivity - setOnlyErrorCheckTimer()");

        // Časovač pro kontrolu doručení hlášení ---------------------------------------------------
        Intent intentTimerCheckError = new Intent(MainActivity.this, TimerReceiver.class);
        intentTimerCheckError.putExtra("report_id", report.getId());

        //Příznak pro Receiver, že jde o budík pro kontrolu odeslání nebo doručení hlášení
        intentTimerCheckError.putExtra("alarm_check_error", 1);

        PendingIntent pendingIntentTimerCheckError = PendingIntent.getBroadcast(
                MainActivity.this,
                report.getRequestCodeForErrorAlarm(),
                intentTimerCheckError,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager amCheckError = (AlarmManager) getSystemService(ALARM_SERVICE);
        amCheckError.set(
                AlarmManager.RTC_WAKEUP,
                report.getTime() + TIME_FOR_CONTROL,
                pendingIntentTimerCheckError);
    }

    // Zrušení časovače pro odeslání hlášení
    // updateDatabase: aktualizace není potřeba, když bude hlášení následně smazáno z databáze
    public void cancelTimer(final Report report, boolean updateDatabase) {

        Log.d(LOG_TAG_SMS, "(113) MainActivity - cancelTimer()");

        // ZRUŠENÍ ČASOVAČŮ
        // Časovač pro odeslání hlášení ------------------------------------------------------------
        Intent intentTimer = new Intent(this, TimerReceiver.class);
        PendingIntent pendingIntentTimer = PendingIntent.getBroadcast(
                this,
                report.getAlarmRequestCode(),
                intentTimer,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager amTimer = (AlarmManager) getSystemService(ALARM_SERVICE);
        amTimer.cancel(pendingIntentTimer);

        // Časovač pro kontrolu doručení hlášení ---------------------------------------------------
        Intent intentTimerCheckError = new Intent(this, TimerReceiver.class);
        PendingIntent pendingIntentTimerCheckError = PendingIntent.getBroadcast(
                this,
                report.getRequestCodeForErrorAlarm(),
                intentTimerCheckError,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
        am.cancel(pendingIntentTimerCheckError);

        if (!updateDatabase) return;

        // Aktualizace hlášení v databázi
        getDataSource().updateReportValue(
                report.getId(),
                new String[]{
                        DbHelper.COLUMN_REQUEST_CODE,
                        DbHelper.COLUMN_ERROR_REQUEST_CODE,
                        DbHelper.COLUMN_SENT,
                        DbHelper.COLUMN_DELIVERED},
                new long[]{
                        NONE,
                        NONE,
                        CANCELED,
                        CANCELED},
                new OnReportUpdatedListener() {
                    @Override
                    public void onReportUpdated(Report updatedReport) {
                        updateItems(new OnItemsLoadedListener() {
                            @Override
                            public void onItemsLoaded(ArrayList<Report> loadedItems) {
                                updateFragments();
                            }
                        });
                    }
                });
    }

    /*
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

        getDataSource().updateReportValue(
                report.getId(),
                new String[]{DbHelper.COLUMN_ERROR_REQUEST_CODE},
                new long[]{NONE},
                new OnReportUpdatedListener() {
                    @Override
                    public void onReportUpdated(Report updatedReport) {
                        updateItems(new OnItemsLoadedListener() {
                            @Override
                            public void onItemsLoaded(ArrayList<Report> loadedItems) {
                                updateFragments();
                            }
                        });
                    }
                });
    }
    */

    public void updateErrorAlert(Report report, final boolean isErrorAlert) {
        report.setErrorAlert(isErrorAlert);

        getDataSource().updateReportValue(
                report.getId(),
                new String[]{
                        DbHelper.COLUMN_IS_ERROR_ALERT},
                new long[]{
                        isErrorAlert ? 1 : 0},
                new OnReportUpdatedListener() {
                    @Override
                    public void onReportUpdated(Report updatedReport) {
                        updateItems(new OnItemsLoadedListener() {
                            @Override
                            public void onItemsLoaded(ArrayList<Report> loadedItems) {
                                updateFragments();

                                if (isErrorAlert)
                                    Toast.makeText(MainActivity.this, "Buzení při neúspěšném odeslání hlášení ZAPNUTO...", Toast.LENGTH_LONG).show();
                                else
                                    Toast.makeText(MainActivity.this, "Buzení při neúspěšném odeslání hlášení VYPNUTO...", Toast.LENGTH_LONG).show();
                            }
                        });
                    }
                });
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
                            showFragment(FRAGMENT_SETTINGS_NAME, null);
                        }

                        @Override
                        public void noSelected() {
                        }
                    }).show();
        }
    }

    public Report getReportById(int id) {
        if (items == null) return null;
        if (items.isEmpty()) return null;

        for (Report r : items) {
            if (r.getId() == id) return r;
        }

        return null;
    }

    public String getSap() {
        getAppSettings();

        if (appSettings.getSap().equals("")) return null;
        else return appSettings.getSap();
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

        if (permissionType == PERMISSION_SMS)
            message = getResources().getString(R.string.permission_explanation_sms);
        if (permissionType == PERMISSION_WRITE_EXTERNAL_STORAGE)
            message = getResources().getString(R.string.permission_explanation_write_external_storage);
        if (permissionType == PERMISSION_READ_CONTACTS)
            message = getResources().getString(R.string.permission_explanation_read_contacts_storage);

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

    public void updateAppSettings(Context context) {
        appSettings = PrefsUtils.getAppSettings(context);
    }

    public DataSource getDataSource() {
        if (dataSource == null) dataSource = new DataSource(this);
        return dataSource;
    }

    public TimerRequestCodeGenerator getTimerRequestCodeGenerator() {
        if (timerRequestCodeGenerator == null)
            timerRequestCodeGenerator = new TimerRequestCodeGenerator(this);
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

    public int getPagesCount() {
        return pagesCount;
    }

    public void setPagesCount(int pagesCount) {
        this.pagesCount = pagesCount;
    }

    public int getItemsCount() {
        return itemsCount;
    }

    public void setItemsCount(int itemsCount) {
        this.itemsCount = itemsCount;
    }

    public int getPage() {
        return page;
    }

    public void setPage(int page) {
        this.page = page;
    }

    /*
    public boolean isShowOnlyWaitingReports() {
        return showOnlyWaitingReports;
    }

    public void setShowOnlyWaitingReports(boolean showOnlyWaitingReports) {
        this.showOnlyWaitingReports = showOnlyWaitingReports;
    }
    */

    private void initStetho() {
        Stetho.InitializerBuilder initializerBuilder = Stetho.newInitializerBuilder(this);
        initializerBuilder.enableWebKitInspector(Stetho.defaultInspectorModulesProvider(this));
        initializerBuilder.enableDumpapp(Stetho.defaultDumperPluginsProvider(this));
        Stetho.Initializer initializer = initializerBuilder.build();
        Stetho.initialize(initializer);
    }

    public void checkVersion() {
        RequestQueue queue = Volley.newRequestQueue(this);
        String url = "http://stodva.cz/Hlaseni/index.php?version_check=1.01";

        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {

                        Log.d("VTKVG", response);

                        Gson gson = new Gson();
                        VersionResponse versionResponse = gson.fromJson(response, VersionResponse.class);

                        if (versionResponse == null) {
                            Log.d("VTKVG", "versionResponse == null");
                            return;
                        }

                        if (versionResponse.getVersion() == 0) {
                            Log.d("VTKVG", "BAD REQUEST");
                            return;
                        }

                        float currentVersion = Float.parseFloat(getAppVersion());

                        Log.d("VTKVG", "VERSION: " + versionResponse.getVersion());
                        Log.d("VTKVG", "MESSAGE:" + versionResponse.getMessage());

                        if (versionResponse.getVersion() > currentVersion) {
                            DialogYesNo.createDialog(MainActivity.this)
                                    .setTitle("Aktualizace")
                                    .setMessage("K dispozici je aktualiuace aplikace. Stáhnout a nainstalovat novou verzi?\n\n" + versionResponse.getMessage())
                                    .setListener(new YesNoSelectedListener() {
                                        @Override
                                        public void yesSelected() {
                                            downloadApp();
                                        }

                                        @Override
                                        public void noSelected() {
                                        }
                                    })
                                    .show();
                        }

                        /*
                        if (!response.equals(getAppVersion())) {
                            DialogYesNo.createDialog(MainActivity.this)
                                    .setTitle("Aktualizace")
                                    .setMessage("K dispozici je aktualiuace aplikace. Stáhnout a nainstalovat novou verzi?")
                                    .setListener(new YesNoSelectedListener() {
                                        @Override
                                        public void yesSelected() {
                                            downloadApp();
                                        }

                                        @Override
                                        public void noSelected() {
                                        }
                                    })
                                    .show();
                        }
                        */
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {

            }
        });

        stringRequest.setTag(REQUEST_QUEUE_TAG);
        queue.add(stringRequest);
    }

    public static void installApplication(Context context, String filePath) {
        Intent intent = new Intent(Intent.ACTION_VIEW);
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

        String fileName = "hlaseni.apk";
        final String destination = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS).toString() + File.separator + fileName;
        final Uri uri = Uri.parse("file://" + destination);

        File file = new File(destination);
        if (file.exists()) file.delete();

        String url = "http://stodva.cz/Hlaseni/hlaseni.apk";

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

            if (requestCode == REQUEST_SELECT_CONTACT) {
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
}