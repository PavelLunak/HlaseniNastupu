package cz.stodva.hlaseninastupu.fragments;

import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioGroup;
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
import cz.stodva.hlaseninastupu.utils.AppConstants;
import cz.stodva.hlaseninastupu.utils.PrefsUtils;

public class FragmentTimer extends Fragment implements AppConstants {

    MainActivity activity;

    TextView labelDateStart, labelDateEnd, labelTimeStart, labelTimeEnd, labelNextTimerStart, labelNextTimerEnd;
    Button btnOkStart, btnOkEnd, btnCancelTimerStart, btnCancelTimerEnd;
    RadioGroup rgStartEnd;

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

        btnOkStart = view.findViewById(R.id.btnOkStart);
        btnOkEnd = view.findViewById(R.id.btnOkEnd);
        btnCancelTimerStart = view.findViewById(R.id.btnCancelTimerStart);
        btnCancelTimerEnd = view.findViewById(R.id.btnCancelTimerEnd);

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

        updateBtnCancelTimer(MESSAGE_TYPE_START);
        updateBtnCancelTimer(MESSAGE_TYPE_END);

        updateNextTimerInfo(MESSAGE_TYPE_START);
        updateNextTimerInfo(MESSAGE_TYPE_END);

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
                    }
                });

                newFragment.show(getActivity().getSupportFragmentManager(), "timePicker");

            }
        });

        btnOkStart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity.checkTimeInput(MESSAGE_TYPE_START))
                    activity.setTimer(MESSAGE_TYPE_START);
            }
        });

        btnOkEnd.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (activity.checkTimeInput(MESSAGE_TYPE_END)) activity.setTimer(MESSAGE_TYPE_END);
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

    public void updateBtnCancelTimer(int messageType) {
        if (messageType == MESSAGE_TYPE_START) {
            if (PrefsUtils.isTimerSet(activity, messageType))
                btnCancelTimerStart.setVisibility(View.VISIBLE);
            else btnCancelTimerStart.setVisibility(View.GONE);
        } else {
            if (PrefsUtils.isTimerSet(activity, messageType))
                btnCancelTimerEnd.setVisibility(View.VISIBLE);
            else btnCancelTimerEnd.setVisibility(View.GONE);
        }
    }

    public void updateNextTimerInfo(int messageType) {
        SimpleDateFormat sdf = new SimpleDateFormat("d.MM. yyyy  k:mm");

        long nextTimer = PrefsUtils.getLastTimer(activity, messageType);

        if (messageType == MESSAGE_TYPE_START) {
            if (!PrefsUtils.isTimerSet(activity, MESSAGE_TYPE_START)) {
                labelNextTimerStart.setText("");
                labelNextTimerStart.setVisibility(View.GONE);
                return;
            }

            if (nextTimer > -1) {
                labelNextTimerStart.setVisibility(View.VISIBLE);
                labelNextTimerStart.setText("Následující automatické hlášení nástupu: " + sdf.format(nextTimer));
            } else {
                labelNextTimerStart.setText("");
                labelNextTimerStart.setVisibility(View.GONE);
            }
        } else if (messageType == MESSAGE_TYPE_END){
            if (!PrefsUtils.isTimerSet(activity, MESSAGE_TYPE_END)) {
                labelNextTimerEnd.setText("");
                labelNextTimerEnd.setVisibility(View.GONE);
                return;
            }

            if (nextTimer > -1) {
                labelNextTimerEnd.setVisibility(View.VISIBLE);
                labelNextTimerEnd.setText("Následující automatické hlášení konce: " + sdf.format(nextTimer));
            }
            else {
                labelNextTimerEnd.setText("");
                labelNextTimerEnd.setVisibility(View.GONE);
            }
        }
    }

    public String addZero(int minutes) {
        if (minutes < 10) return "0" + minutes;
        else return "" + minutes;
    }
}
