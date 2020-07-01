package cz.stodva.hlaseninastupu.fragments;

import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.SmsManager;
import android.telephony.TelephonyManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
    TelephonyManager telephonyManager;
    PhoneStateListener phoneStateListener;

    TextView btnStartShift, btnEndShift, btnSetTimeForReport;
    TextView labelLastTimerStart, labelLastTimerEnd;
    TextView labelNextTimerStart, labelNextTimerEnd;
    RelativeLayout layoutLastTimerStart, layoutLastTimerEnd;
    ImageView imgLastSentStart, imgLastDeliveredSart, imgLastSentEnd, imgLastDeliveredEnd;
    ImageView imgStartWarn, imgEndWarn;


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

        layoutLastTimerStart = view.findViewById(R.id.layoutLastTimerStart);
        layoutLastTimerEnd = view.findViewById(R.id.layoutLastTimerEnd);

        labelLastTimerStart = view.findViewById(R.id.labelLastTimerStart);
        labelLastTimerEnd = view.findViewById(R.id.labelLastTimerEnd);
        labelNextTimerStart = view.findViewById(R.id.labelNextTimerStart);
        labelNextTimerEnd = view.findViewById(R.id.labelNextTimerEnd);

        imgLastSentStart = view.findViewById(R.id.imgLastSentStart);
        imgLastDeliveredSart = view.findViewById(R.id.imgLastDeliveredSart);
        imgLastSentEnd = view.findViewById(R.id.imgLastSentEnd);
        imgLastDeliveredEnd = view.findViewById(R.id.imgLastDeliveredEnd);

        imgStartWarn = view.findViewById(R.id.imgStartWarn);
        imgEndWarn = view.findViewById(R.id.imgEndWarn);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateReportInfo();

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
    }

    private void requestSendReport(final int messageType) {
        if (messageType == MESSAGE_TYPE_START) {
            if (PrefsUtils.isTimerSet(activity, MESSAGE_TYPE_START)) {
                Toast.makeText(activity, "Nelze odesílat hlášení nástupu při nastaveném budíku na automatické hlášení nástupu", Toast.LENGTH_LONG).show();
                return;
            }
        } else if (messageType ==MESSAGE_TYPE_END) {
            if (PrefsUtils.isTimerSet(activity, MESSAGE_TYPE_END)) {
                Toast.makeText(activity, "Nelze odesílat hlášení konce při nastaveném budíku na automatické hlášení konce", Toast.LENGTH_LONG).show();
                return;
            }
        }

        final String phone = activity.getPhoneNumber();
        final String text = activity.getMessage(messageType);

        if (text == null) {
            Toast.makeText(activity, "Není nastaven text hlášení!", Toast.LENGTH_LONG).show();
            return;
        }

        if (!activity.checkSmsPermissionGranted()) return;

        DialogYesNo.createDialog(activity)
                .setTitle(messageType == MESSAGE_TYPE_START ? "Nástup" : "Konec")
                .setMessage("Odeslat na tel. číslo " +
                        activity.getPhoneNumber() +
                        " hlášení " +
                        (messageType == MESSAGE_TYPE_START ? " nástupu na směnu?" : "konce směny?"))
                .setListener(new YesNoSelectedListener() {
                    @Override
                    public void yesSelected() {
                        // Zapne sledování stavu zařízení a pokud je schopné odesílat SMS, bude odesláno hlášení
                        initPhoneStateListener(phone, text, messageType);
                    }

                    @Override public void noSelected() {}
                }).show();
    }

    private void sendSms(String phone, String text, int messageType) {
        // Vynulování příznaku úspěšného odeslání hlášení
        PrefsUtils.saveIsReportSent(activity, false, messageType, REPORT_TYPE_LAST);

        // Vynulování příznaku úspěšného doručení hlášení
        PrefsUtils.saveIsReportDelivered(activity, false, messageType, REPORT_TYPE_LAST);

        // Uložení času hlášení
        PrefsUtils.setReportTime(activity, (new Date()).getTime(), messageType, REPORT_TYPE_LAST);
        updateReportInfo();

        Intent sentIntent = new Intent(activity, MessageSentReceiver.class);
        sentIntent.putExtra("message_type", messageType);
        sentIntent.putExtra("report_type", REPORT_TYPE_LAST);

        PendingIntent pi1 = PendingIntent.getBroadcast(
                activity,
                messageType == MESSAGE_TYPE_START ? SENT_REQUEST_START : SENT_REQUEST_END,
                sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        Intent deliveredIntent = new Intent(activity, MessageDeliveredReceiver.class);
        deliveredIntent.putExtra("message_type", messageType);
        sentIntent.putExtra("report_type", REPORT_TYPE_LAST);

        PendingIntent pi2 = PendingIntent.getBroadcast(
                activity,
                messageType == MESSAGE_TYPE_START ? DELIVERED_REQUEST_START : DELIVERED_REQUEST_END,
                deliveredIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        SmsManager sm = SmsManager.getDefault();
        sm.sendTextMessage(phone, null, text, pi1, pi2);
    }

    public void updateReportInfo() {
        Log.d(LOG_TAG, "FragmentMain - updateReportInfo()");

        SimpleDateFormat sdf = new SimpleDateFormat("d.MM.yyyy k:mm");

        long lastStart = PrefsUtils.getReportTime(activity, MESSAGE_TYPE_START, REPORT_TYPE_LAST);
        long lastEnd = PrefsUtils.getReportTime(activity, MESSAGE_TYPE_END, REPORT_TYPE_LAST);

        long nextStart = PrefsUtils.getReportTime(activity, MESSAGE_TYPE_START, REPORT_TYPE_NEXT);
        long nextEnd = PrefsUtils.getReportTime(activity, MESSAGE_TYPE_END, REPORT_TYPE_NEXT);

        if (lastStart > -1) labelLastTimerStart.setText(sdf.format(lastStart));
        else labelLastTimerStart.setText("Není nastaveno");

        if (lastEnd > -1) labelLastTimerEnd.setText(sdf.format(lastEnd));
        else labelLastTimerEnd.setText("Není nastaveno");

        if (nextStart > -1) labelNextTimerStart.setText(sdf.format(nextStart));
        else labelNextTimerStart.setText("Není nastaveno");

        if (nextEnd > -1) labelNextTimerEnd.setText(sdf.format(nextEnd));
        else labelNextTimerEnd.setText("Není nastaveno");


        if (PrefsUtils.isReportSent(activity, MESSAGE_TYPE_START, REPORT_TYPE_LAST)) imgLastSentStart.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_green));
        else imgLastSentStart.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_gray));

        if (PrefsUtils.isReportDelivered(activity, MESSAGE_TYPE_START, REPORT_TYPE_LAST)) imgLastDeliveredSart.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_green));
        else imgLastDeliveredSart.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_gray));

        if (PrefsUtils.isReportSent(activity, MESSAGE_TYPE_END, REPORT_TYPE_LAST)) imgLastSentEnd.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_green));
        else imgLastSentEnd.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_gray));

        if (PrefsUtils.isReportDelivered(activity, MESSAGE_TYPE_END, REPORT_TYPE_LAST)) imgLastDeliveredEnd.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_green));
        else imgLastDeliveredEnd.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_gray));

        showWarn(
                MESSAGE_TYPE_START,
                !PrefsUtils.isReportSent(activity, MESSAGE_TYPE_START, REPORT_TYPE_LAST)
                        || !PrefsUtils.isReportDelivered(activity, MESSAGE_TYPE_START, REPORT_TYPE_LAST));

        showWarn(
                MESSAGE_TYPE_END,
                !PrefsUtils.isReportSent(activity, MESSAGE_TYPE_END, REPORT_TYPE_LAST)
                        || !PrefsUtils.isReportDelivered(activity, MESSAGE_TYPE_END, REPORT_TYPE_LAST));
    }

    public void showWarn(int messageType, boolean show) {
        Log.d(LOG_TAG, "FragmentMain - showWarn()");

        if (messageType == MESSAGE_TYPE_START) {
            imgStartWarn.setVisibility(show ? View.VISIBLE : View.GONE);
        } else if (messageType == MESSAGE_TYPE_END) {
            imgEndWarn.setVisibility(show ? View.VISIBLE : View.GONE);
        } else {
            imgStartWarn.setVisibility(show ? View.VISIBLE : View.GONE);
            imgEndWarn.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    public void initPhoneStateListener(final String phone, final String text, final int messageType) {
        Log.d(LOG_TAG_SMS, "FragmentMain - initPhoneStateListener()");
        if (telephonyManager == null) {
            Log.d(LOG_TAG_SMS, "telephonyManager == null -> new init");
            telephonyManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
        }

        if (phoneStateListener == null) {
            Log.d(LOG_TAG_SMS, "phoneStateListener == null -> new init");

            phoneStateListener = new PhoneStateListener() {
                @Override
                public void onServiceStateChanged(ServiceState serviceState) {
                    super.onServiceStateChanged(serviceState);

                    switch (serviceState.getState()) {
                        case ServiceState.STATE_IN_SERVICE:
                            Log.d(LOG_TAG_SMS, "FragmentMain - onServiceStateChanged: STATE_IN_SERVICE");
                            sendSms(phone, text, messageType);
                            break;
                        case ServiceState.STATE_OUT_OF_SERVICE:
                            Log.d(LOG_TAG_SMS, "FragmentMain - onServiceStateChanged: STATE_OUT_OF_SERVICE: ");
                            showPhoneStateError("HLÁŠENÍ NELZE ODESLAT - není dostupná síť");
                            break;
                        case ServiceState.STATE_EMERGENCY_ONLY:
                            Log.d(LOG_TAG_SMS, "FragmentMain - onServiceStateChanged: STATE_EMERGENCY_ONLY");
                            showPhoneStateError("HLÁŠENÍ NELZE ODESLAT - je povoleno pouze tísňové volání");
                            break;
                        case ServiceState.STATE_POWER_OFF:
                            Log.d(LOG_TAG_SMS, "FragmentMain - onServiceStateChanged: STATE_POWER_OFF");
                            showPhoneStateError("HLÁŠENÍ NELZE ODESLAT - je zapnut režim letadlo");
                            break;
                        default:
                            Log.d(LOG_TAG_SMS, "FragmentMain - onServiceStateChanged: UNKNOWN_STATE");
                            showPhoneStateError("HLÁŠENÍ NELZE ODESLAT - neznámý důvod nedostupnosti sítě");
                            break;
                    }

                    cancelPhoneStateListener();
                }
            };

            telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
        }
    }

    public void cancelPhoneStateListener() {
        Log.d(LOG_TAG_SMS, "FragmentMain - cancelPhoneStateListener()");

        telephonyManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);

        telephonyManager = null;
        phoneStateListener = null;
    }

    private void showPhoneStateError(String message) {
        DialogInfo.createDialog(activity).setTitle("Chyba").setMessage(message).show();
    }

    public ImageView getImgResult(int imgId) {
        switch (imgId) {
            case R.id.imgLastSentStart:
                return imgLastSentStart;
            case R.id.imgLastDeliveredSart:
                return imgLastDeliveredSart;
            case R.id.imgLastSentEnd:
                return imgLastSentEnd;
            case R.id.imgLastDeliveredEnd:
                return imgLastDeliveredEnd;
        }

        return null;
    }
}
