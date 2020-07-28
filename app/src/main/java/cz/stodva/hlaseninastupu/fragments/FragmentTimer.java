package cz.stodva.hlaseninastupu.fragments;

import android.content.Context;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;

import cz.stodva.hlaseninastupu.MainActivity;
import cz.stodva.hlaseninastupu.R;
import cz.stodva.hlaseninastupu.customviews.DialogInfo;
import cz.stodva.hlaseninastupu.database.DbHelper;
import cz.stodva.hlaseninastupu.listeners.OnItemsLoadedListener;
import cz.stodva.hlaseninastupu.listeners.OnReportAddedListener;
import cz.stodva.hlaseninastupu.listeners.OnReportLoadedListener;
import cz.stodva.hlaseninastupu.listeners.OnReportUpdatedListener;
import cz.stodva.hlaseninastupu.objects.Report;
import cz.stodva.hlaseninastupu.pickers.DatePicker;
import cz.stodva.hlaseninastupu.pickers.TimePicker;
import cz.stodva.hlaseninastupu.utils.Animators;
import cz.stodva.hlaseninastupu.utils.AppConstants;

public class FragmentTimer extends Fragment implements AppConstants {

    RadioGroup rgStartEnd;
    RadioButton rbStart, rbEnd;

    TextView labelDate, labelTime;
    TextView btnOk;
    CheckBox chbErrorAlarm;

    MainActivity activity;
    boolean isEdit;
    boolean isUsed;


    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) activity = (MainActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        Bundle args = getArguments();

        if (args != null) isEdit = args.containsKey("edit");
        if (args != null) isUsed = args.containsKey("use");

        View view = inflater.inflate(R.layout.fragment_timer_new, container, false);

        rgStartEnd = view.findViewById(R.id.rgStartEnd);
        rbStart = view.findViewById(R.id.rbStart);
        rbEnd = view.findViewById(R.id.rbEnd);
        labelDate = view.findViewById(R.id.labelDate);
        labelTime = view.findViewById(R.id.labelTime);
        btnOk = view.findViewById(R.id.btnOk);
        chbErrorAlarm = view.findViewById(R.id.chbErrorAlarm);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        Calendar calendar = Calendar.getInstance();

        // Editace existujícího hlášení
        if (isEdit) {
            btnOk.setText("ULOŽIT ÚPRAVY");

            calendar.setTime(new Date(activity.actualReport.getTime()));
            activity.setDateData(calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR));
            activity.setTimeData(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        }
        // Vytvoření nového hlášení podle jiného hlášení
        else if (isUsed) {
            Report copiedReport = new Report();
            copiedReport.setMessageType(activity.actualReport.getMessageType());
            copiedReport.setTime(activity.actualReport.getTime());
            copiedReport.setSentTime(activity.actualReport.getSentTime());
            copiedReport.setDeliveryTime(activity.actualReport.getDeliveryTime());
            copiedReport.setErrorAlert(activity.actualReport.isErrorAlert());
            copiedReport.setMessageType(activity.actualReport.getMessageType());

            activity.actualReport = copiedReport;

            calendar.setTime(new Date(activity.actualReport.getTime()));
            activity.setDateData(calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR));
            activity.setTimeData(calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE));
        }
        // Vytvoření nového hlášení
        else {
            btnOk.setText("NASTAVIT");

            activity.actualReport = new Report();
            activity.actualReport.setMessageType(getMessageType());

            activity.setDateData(calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR));
        }

        updateViews();

        labelDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dilogFragment = new DatePicker(new DatePicker.OnDateSelectedListener() {
                    @Override
                    public void onDateSelected(int day, int month, int year) {
                        activity.setDateData(day, month, year);
                        labelDate.setText("" + day + "." + (month + 1) + "." + year);
                    }
                });

                dilogFragment.show(getActivity().getSupportFragmentManager(), "datePicker");

            }
        });

        labelTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment dilogFragment = new TimePicker(new TimePicker.OnTimeSelectedListener() {
                    @Override
                    public void onTimeSelected(int hours, int minutes) {
                        activity.setTimeData(hours, minutes);
                        activity.actualReport.setTime(activity.getTimeInMillis());
                        labelTime.setText("" + addZero(hours) + ":" + addZero(minutes));
                        btnOk.setVisibility(activity.isTimeSet() ? View.VISIBLE : View.GONE);
                        Animators.animateButton(btnOk);
                    }
                });

                dilogFragment.show(getActivity().getSupportFragmentManager(), "timePicker");
            }
        });

        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.d(LOG_TAG, "FragmentTimer - btnOk click" + LOG_UNDERLINED);
                Animators.animateButtonClick(btnOk, true);

                // Test zadání času
                if (activity.checkTimeInput()) {
                    long time = activity.getTimeInMillis();

                    // Test jestli není nastavován report v minulosti
                    if (activity.isInFuture(time)) {
                        // Test jestli není nastaven report se stejným datem (neplatí pro případ editace hlášení)
                        activity.getDataSource().getReportByTime(time, new OnReportLoadedListener() {
                            @Override
                            public void onReportLoaded(Report loadedReport) {
                                //Nenalezeno žádné hlášení se stejným časem
                                if (loadedReport == null || isEdit) {
                                    if (isEdit) {
                                        // Zrušení původních časovačů
                                        activity.cancelTimer(activity.actualReport, false);

                                        activity.actualReport.setMessageType(rbStart.isChecked() ? MESSAGE_TYPE_START : MESSAGE_TYPE_END);
                                        activity.actualReport.setTime(activity.getTimeInMillis());
                                        activity.actualReport.setSentTime(WAITING);
                                        activity.actualReport.setDeliveryTime(WAITING);
                                        activity.actualReport.setErrorAlert(chbErrorAlarm.isChecked());

                                        activity.getDataSource().updateReportValue(
                                                activity.actualReport.getId(),
                                                new String[]{
                                                        DbHelper.COLUMN_TYPE,
                                                        DbHelper.COLUMN_TIME,
                                                        DbHelper.COLUMN_SENT,
                                                        DbHelper.COLUMN_DELIVERED,
                                                        DbHelper.COLUMN_IS_ERROR_ALERT},
                                                new long[]{
                                                        activity.actualReport.getMessageType(),
                                                        activity.actualReport.getTime(),
                                                        WAITING,
                                                        WAITING,
                                                        activity.actualReport.isErrorAlert() ? 1 : 0},
                                                new OnReportUpdatedListener() {
                                                    @Override
                                                    public void onReportUpdated(Report updatedReport) {
                                                        activity.updateItems(new OnItemsLoadedListener() {
                                                            @Override
                                                            public void onItemsLoaded(ArrayList<Report> loadedItems) {
                                                                activity.setTimerWithErrorCheck(activity.actualReport);
                                                                activity.showFragment(AppConstants.FRAGMENT_ITEMS_NAME, null);
                                                                Toast.makeText(activity, "Hlášení bylo upraveno...", Toast.LENGTH_LONG).show();
                                                            }
                                                        });
                                                    }
                                                });
                                    } else {
                                        if (activity.actualReport == null) {
                                            activity.actualReport = new Report();
                                            activity.actualReport.setMessageType(rbStart.isChecked() ? MESSAGE_TYPE_START : MESSAGE_TYPE_END);
                                        }

                                        activity.actualReport.setTime(activity.getTimeInMillis());
                                        activity.actualReport.setSentTime(WAITING);
                                        activity.actualReport.setDeliveryTime(WAITING);
                                        activity.actualReport.setErrorAlert(chbErrorAlarm.isChecked());

                                        activity.addReportToDatabase(activity.actualReport, new OnReportAddedListener() {
                                            @Override
                                            public void onReportAdded(Report addedReport) {
                                                Log.d(LOG_TAG, "Nové hlášení bylo přidáno a získávám jeho ID, které je: " + addedReport.getId());
                                                activity.actualReport.setId(addedReport.getId());
                                                activity.setTimerWithErrorCheck(activity.actualReport);
                                                activity.showFragment(FRAGMENT_MAIN_NAME, null);
                                            }
                                        });
                                    }
                                } else {
                                    DialogInfo.createDialog(activity).setTitle("Chyba").setMessage("Na zadaný čas je již nastaveno jiné hlášení...").show();
                                }
                            }
                        });
                    } else {
                        DialogInfo.createDialog(activity).setTitle("Chyba").setMessage("Nelze nastavit hlášení v minulosti...").show();
                    }
                }

            }
        });

        rgStartEnd.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                if (activity.actualReport == null) return;
                activity.actualReport.setMessageType(checkedId == R.id.rbStart ? MESSAGE_TYPE_START : MESSAGE_TYPE_END);
            }
        });
    }

    public void updateViews() {
        Calendar calendar = Calendar.getInstance();

        if (activity.isTimeSet()) {
            labelTime.setText("" + addZero(activity.getHours()) + ":" + addZero(activity.getMinutes()));
            btnOk.setVisibility(View.VISIBLE);

            calendar.set(
                    activity.getYear(),
                    activity.getMonth(),
                    activity.getDay(),
                    activity.getHours(),
                    activity.getMinutes());
        } else {
            labelTime.setText("Nastavit čas");
            btnOk.setVisibility(View.GONE);
        }

        SimpleDateFormat sdfDate = new SimpleDateFormat("d.M. yyyy");

        labelDate.setText(sdfDate.format(calendar.getTimeInMillis()));
        chbErrorAlarm.setChecked(activity.actualReport.isErrorAlert());
        btnOk.setVisibility(activity.isTimeSet() ? View.VISIBLE : View.GONE);
    }

    public String addZero(int minutes) {
        if (minutes < 10) return "0" + minutes;
        else return "" + minutes;
    }

    private int getMessageType() {
        if (rbStart.isChecked()) return MESSAGE_TYPE_START;
        else return MESSAGE_TYPE_END;
    }
}
