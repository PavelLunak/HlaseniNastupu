package cz.stodva.hlaseninastupu.fragments;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.SmsManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;

import java.text.SimpleDateFormat;
import java.util.Date;

import cz.stodva.hlaseninastupu.MainActivity;
import cz.stodva.hlaseninastupu.R;
import cz.stodva.hlaseninastupu.customviews.DialogInfo;
import cz.stodva.hlaseninastupu.customviews.DialogYesNo;
import cz.stodva.hlaseninastupu.listeners.YesNoSelectedListener;
import cz.stodva.hlaseninastupu.receivers.MessageDeliveredReceiver;
import cz.stodva.hlaseninastupu.receivers.MessageSentReceiver;
import cz.stodva.hlaseninastupu.utils.Animators;
import cz.stodva.hlaseninastupu.utils.AppConstants;
import cz.stodva.hlaseninastupu.utils.PrefsUtils;

public class FragmentMain extends Fragment implements AppConstants {

    MainActivity activity;

    TextView btnStartShift, btnEndShift, btnSetTimeForReport;
    TextView labelLastTimerStart, labelLastTimerEnd;
    RelativeLayout layoutLastTimerStart, layoutLastTimerEnd;
    ImageButton btnSettings;
    ImageView imgSentStart, imgDeliveredSart;
    ImageView imgSentEnd, imgDeliveredEnd;

    Button btnTest;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);

        if (context instanceof MainActivity) activity = (MainActivity) context;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_main, container, false);

        btnStartShift = view.findViewById(R.id.btnStartShift);
        btnEndShift = view.findViewById(R.id.btnEndShift);
        btnSetTimeForReport = view.findViewById(R.id.btnSetTimeForReport);
        btnSettings = view.findViewById(R.id.btnSettings);

        layoutLastTimerStart = view.findViewById(R.id.layoutLastTimerStart);
        layoutLastTimerEnd = view.findViewById(R.id.layoutLastTimerEnd);
        labelLastTimerStart = view.findViewById(R.id.labelLastTimerStart);
        labelLastTimerEnd = view.findViewById(R.id.labelLastTimerEnd);
        imgSentStart = view.findViewById(R.id.imgSentStart);
        imgDeliveredSart = view.findViewById(R.id.imgDeliveredSart);
        imgSentEnd = view.findViewById(R.id.imgSentEnd);
        imgDeliveredEnd = view.findViewById(R.id.imgDeliveredEnd);

        btnTest = view.findViewById(R.id.btnTest);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateLastReportInfo();

        btnTest.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(activity,
                        "Sent start: " + PrefsUtils.isReportSent(activity, MESSAGE_TYPE_START) +
                                "\n" +
                                "Delivered start: " + PrefsUtils.isReportDelivered(activity, MESSAGE_TYPE_START) +
                                "\n" +
                                "Sent end: " + PrefsUtils.isReportSent(activity, MESSAGE_TYPE_END) +
                                "\n" +
                                "Delivered end: " + PrefsUtils.isReportDelivered(activity, MESSAGE_TYPE_END), Toast.LENGTH_LONG).show();
            }
        });

        btnStartShift.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animators.animateButtonClick(btnStartShift, true);
                requestSendReport(MESSAGE_TYPE_START);
            }
        });

        btnEndShift.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animators.animateButtonClick(btnEndShift, true);
                requestSendReport(MESSAGE_TYPE_END);
            }
        });

        btnSetTimeForReport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Animators.animateButtonClick(btnSetTimeForReport, true);
                if (!activity.checkSmsPermissionGranted()) return;
                activity.showFragment(FRAGMENT_TIMER_NAME);
            }
        });

        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                activity.showFragment(FRAGMENT_SETTINGS_NAME);
            }
        });
    }

    private void requestSendReport(final int message_type) {
        if (message_type == MESSAGE_TYPE_START) {
            if (PrefsUtils.isTimerSet(activity, MESSAGE_TYPE_START)) {
                Toast.makeText(activity, "Nelze odesílat hlášení nástupu při nastaveném budíku na automatické hlášení nástupu", Toast.LENGTH_LONG).show();
                return;
            }
        } else if (message_type ==MESSAGE_TYPE_END) {
            if (PrefsUtils.isTimerSet(activity, MESSAGE_TYPE_END)) {
                Toast.makeText(activity, "Nelze odesílat hlášení konce při nastaveném budíku na automatické hlášení konce", Toast.LENGTH_LONG).show();
                return;
            }
        }

        final String phone = activity.getPhoneNumber();
        final String text = activity.getMessage(message_type);

        if (text == null) {
            Toast.makeText(activity, "Není nastaven text hlášení!", Toast.LENGTH_LONG).show();
            return;
        }

        if (!activity.checkSmsPermissionGranted()) return;

        DialogYesNo.createDialog(activity)
                .setTitle(message_type == MESSAGE_TYPE_START ? "Nástup" : "Konec")
                .setMessage("Odeslat na tel. číslo " +
                        activity.getPhoneNumber() +
                        " hlášení " +
                        (message_type == MESSAGE_TYPE_START ? " nástupu na směnu?" : "konce směny?"))
                .setListener(new YesNoSelectedListener() {
                    @Override
                    public void yesSelected() {
                        PrefsUtils.saveIsReportSent(activity, false, message_type);
                        PrefsUtils.saveIsReportDelivered(activity, false, message_type);

                        PrefsUtils.setLastReportTime(activity, (new Date()).getTime(), message_type);
                        updateLastReportInfo();

                        Intent sentIntent = new Intent(activity, MessageSentReceiver.class);
                        sentIntent.putExtra("message_type", message_type);
                        PendingIntent pi1 = PendingIntent.getBroadcast(
                                activity,
                                message_type == MESSAGE_TYPE_START ? SENT_REQUEST_START : SENT_REQUEST_END,
                                sentIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT);

                        Intent deliveredIntent = new Intent(activity, MessageDeliveredReceiver.class);
                        deliveredIntent.putExtra("message_type", message_type);
                        PendingIntent pi2 = PendingIntent.getBroadcast(
                                activity,
                                message_type == MESSAGE_TYPE_START ? DELIVERED_REQUEST_START : DELIVERED_REQUEST_END,
                                deliveredIntent,
                                PendingIntent.FLAG_UPDATE_CURRENT);

                        SmsManager sm = SmsManager.getDefault();
                        sm.sendTextMessage(phone, null, text, pi1, pi2);
                    }

                    @Override public void noSelected() {}
                }).show();
    }

    public void updateLastReportInfo() {
        SimpleDateFormat sdf = new SimpleDateFormat("d.MM.yyyy k:mm");

        long lastStart = PrefsUtils.getLastTimer(activity, MESSAGE_TYPE_START);
        long lastEnd = PrefsUtils.getLastTimer(activity, MESSAGE_TYPE_END);

        if (lastStart > -1) {
            labelLastTimerStart.setText("Nástup: " + sdf.format(lastStart));
        } else {
            labelLastTimerStart.setText("???");
        }

        if (lastEnd > -1) {
            labelLastTimerEnd.setText("Konec: " + sdf.format(lastEnd));
        } else {
            labelLastTimerEnd.setText("???");
        }

        AppCompatResources.getDrawable(activity, R.drawable.ic_check_green);

        if (PrefsUtils.isReportSent(activity, MESSAGE_TYPE_START)) imgSentStart.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_green));
        else imgSentStart.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_gray));

        if (PrefsUtils.isReportDelivered(activity, MESSAGE_TYPE_START)) imgDeliveredSart.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_green));
        else imgDeliveredSart.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_gray));

        if (PrefsUtils.isReportSent(activity, MESSAGE_TYPE_END)) imgSentEnd.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_green));
        else imgSentEnd.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_gray));

        if (PrefsUtils.isReportDelivered(activity, MESSAGE_TYPE_END)) imgDeliveredEnd.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_green));
        else imgDeliveredEnd.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_gray));
    }

    public ImageView getImgResult(int imgId) {
        switch (imgId) {
            case R.id.imgSentStart:
                return imgSentStart;
            case R.id.imgDeliveredSart:
                return imgDeliveredSart;
            case R.id.imgSentEnd:
                return imgSentEnd;
            case R.id.imgDeliveredEnd:
                return imgDeliveredEnd;
        }

        return null;
    }
}
