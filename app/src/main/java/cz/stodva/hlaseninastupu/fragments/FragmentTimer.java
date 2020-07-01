package cz.stodva.hlaseninastupu.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Calendar;

import cz.stodva.hlaseninastupu.MainActivity;
import cz.stodva.hlaseninastupu.R;
import cz.stodva.hlaseninastupu.pickers.DatePicker;
import cz.stodva.hlaseninastupu.pickers.TimePicker;
import cz.stodva.hlaseninastupu.utils.Animators;
import cz.stodva.hlaseninastupu.utils.AppConstants;
import cz.stodva.hlaseninastupu.utils.PrefsUtils;

public class FragmentTimer extends Fragment implements AppConstants, CompoundButton.OnCheckedChangeListener {

    MainActivity activity;

    TextView labelDateStart, labelDateEnd, labelTimeStart, labelTimeEnd;
    TextView btnOkStart, btnOkEnd;
    TextView titleStartTime, titleEndTime;
    TextView labelNextTimerStart, labelNextTimerEnd;
    Button btnCancelTimerStart, btnCancelTimerEnd;
    LinearLayout layoutStart, layoutEnd;
    CheckBox chbNoSentStart;
    CheckBox chbNoSentEnd;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        if (context instanceof MainActivity) activity = (MainActivity) context;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_timer, container, false);

        labelDateStart = view.findViewById(R.id.labelDateStart);
        labelDateEnd = view.findViewById(R.id.labelDateEnd);
        labelTimeStart = view.findViewById(R.id.labelTimeStart);
        labelTimeEnd = view.findViewById(R.id.labelTimeEnd);
        labelNextTimerStart = view.findViewById(R.id.labelNextTimerStart);
        labelNextTimerEnd = view.findViewById(R.id.labelNextTimerEnd);
        titleStartTime = view.findViewById(R.id.titleStartTime);
        titleEndTime = view.findViewById(R.id.titleEndTime);

        btnOkStart = view.findViewById(R.id.btnOkStart);
        btnOkEnd = view.findViewById(R.id.btnOkEnd);
        btnCancelTimerStart = view.findViewById(R.id.btnCancelTimerStart);
        btnCancelTimerEnd = view.findViewById(R.id.btnCancelTimerEnd);
        layoutStart = view.findViewById(R.id.layoutStart);
        layoutEnd = view.findViewById(R.id.layoutEnd);
        chbNoSentStart = view.findViewById(R.id.chbNoSentStart);
        chbNoSentEnd = view.findViewById(R.id.chbNoSentEnd);

        chbNoSentStart.setOnCheckedChangeListener(this);
        chbNoSentEnd.setOnCheckedChangeListener(this);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        SimpleDateFormat sdf = new SimpleDateFormat("d.M. yyyy");
        Calendar calendar = Calendar.getInstance();

        activity.setDateData(calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR), MESSAGE_TYPE_START);
        activity.setDateData(calendar.get(Calendar.DAY_OF_MONTH), calendar.get(Calendar.MONTH), calendar.get(Calendar.YEAR), AppConstants.MESSAGE_TYPE_END);

        labelDateStart.setText(sdf.format(calendar.getTimeInMillis()));
        labelDateEnd.setText(sdf.format(calendar.getTimeInMillis()));

        updateLayoutsVisibility();

        labelDateStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new DatePicker(new DatePicker.OnDateSelectedListener() {
                    @Override
                    public void onDateSelected(int day, int month, int year) {
                        activity.setDateData(day, month, year, MESSAGE_TYPE_START);
                        labelDateStart.setText("" + day + "." + (month + 1) + "." + year);
                    }
                });

                newFragment.show(getActivity().getSupportFragmentManager(), "datePicker");

            }
        });

        labelDateEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new DatePicker(new DatePicker.OnDateSelectedListener() {
                    @Override
                    public void onDateSelected(int day, int month, int year) {
                        activity.setDateData(day, month, year, MESSAGE_TYPE_END);
                        labelDateEnd.setText("" + day + "." + (month + 1) + "." + year);
                    }
                });

                newFragment.show(getActivity().getSupportFragmentManager(), "datePicker");

            }
        });

        labelTimeStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new TimePicker(new TimePicker.OnTimeSelectedListener() {
                    @Override
                    public void onTimeSelected(int hours, int minutes) {
                        activity.setTimeData(hours, minutes, MESSAGE_TYPE_START);
                        labelTimeStart.setText("" + hours + ":" + addZero(minutes));
                        Animators.animateButton(btnOkStart);
                    }
                });

                newFragment.show(getActivity().getSupportFragmentManager(), "timePicker");

            }
        });

        labelTimeEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                DialogFragment newFragment = new TimePicker(new TimePicker.OnTimeSelectedListener() {
                    @Override
                    public void onTimeSelected(int hours, int minutes) {
                        activity.setTimeData(hours, minutes, MESSAGE_TYPE_END);
                        labelTimeEnd.setText("" + hours + ":" + addZero(minutes));
                        Animators.animateButton(btnOkEnd);
                    }
                });

                newFragment.show(getActivity().getSupportFragmentManager(), "timePicker");

            }
        });

        btnOkStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animators.animateButtonClick(btnOkStart, true);

                if (activity.checkTimeInput(MESSAGE_TYPE_START))
                    activity.setTimer(MESSAGE_TYPE_START, REPORT_TYPE_NEXT);
            }
        });

        btnOkEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animators.animateButtonClick(btnOkEnd, true);

                if (activity.checkTimeInput(MESSAGE_TYPE_END))
                    activity.setTimer(MESSAGE_TYPE_END, REPORT_TYPE_NEXT);
            }
        });

        btnCancelTimerStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.cancelTimer(MESSAGE_TYPE_START);
            }
        });

        btnCancelTimerEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.cancelTimer(MESSAGE_TYPE_END);
            }
        });
    }

    public void updateLayoutsVisibility() {
        SimpleDateFormat sdf = new SimpleDateFormat("d.MM. yyyy  k:mm");
        long nextTimerStart = PrefsUtils.getReportTime(activity, MESSAGE_TYPE_START, REPORT_TYPE_LAST);
        long nextTimerEnd = PrefsUtils.getReportTime(activity, MESSAGE_TYPE_END, REPORT_TYPE_LAST);

        labelNextTimerStart.setText(sdf.format(nextTimerStart));
        labelNextTimerEnd.setText(sdf.format(nextTimerEnd));

        showStartLayout(PrefsUtils.isTimerSet(activity, MESSAGE_TYPE_START));
        showEndLayout(PrefsUtils.isTimerSet(activity, MESSAGE_TYPE_END));
    }

    public String addZero(int minutes) {
        if (minutes < 10) return "0" + minutes;
        else return "" + minutes;
    }

    private void showStartLayout(boolean show) {
        if (show) {
            layoutStart. setVisibility(View.VISIBLE);
            Animators.animateButton(chbNoSentStart);
        } else {
            layoutStart. setVisibility(View.GONE);
        }
    }

    private void showEndLayout(boolean show) {
        if (show) {
            layoutEnd. setVisibility(View.VISIBLE);
            Animators.animateButton(chbNoSentEnd);
        } else {
            layoutEnd. setVisibility(View.GONE);
        }
    }

    @Override
    public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
        switch (buttonView.getId()) {
            case R.id.chbNoSentStart:
                PrefsUtils.setAlarm(activity, isChecked, MESSAGE_TYPE_START, ALARM_TYPE_NO_SENT);
                PrefsUtils.setAlarm(activity, isChecked, MESSAGE_TYPE_START, ALARM_TYPE_NO_DELIVERED);

                if (isChecked) {
                    activity.setTimerForError(AppConstants.MESSAGE_TYPE_START, AppConstants.ERROR_TYPE_NO_SENT);
                    activity.setTimerForError(AppConstants.MESSAGE_TYPE_START, AppConstants.ERROR_TYPE_NO_DELIVERED);
                } else {
                    activity.cancelTimerForError(MESSAGE_TYPE_START);
                }

                break;
            case R.id.chbNoSentEnd:
                PrefsUtils.setAlarm(activity, isChecked, MESSAGE_TYPE_END, ALARM_TYPE_NO_SENT);
                PrefsUtils.setAlarm(activity, isChecked, MESSAGE_TYPE_END, ALARM_TYPE_NO_DELIVERED);

                if (isChecked) {
                    activity.setTimerForError(AppConstants.MESSAGE_TYPE_END, AppConstants.ERROR_TYPE_NO_SENT);
                    activity.setTimerForError(AppConstants.MESSAGE_TYPE_END, AppConstants.ERROR_TYPE_NO_DELIVERED);
                }
                else {
                    activity.cancelTimerForError(MESSAGE_TYPE_END);
                }

                break;
        }
    }
}
