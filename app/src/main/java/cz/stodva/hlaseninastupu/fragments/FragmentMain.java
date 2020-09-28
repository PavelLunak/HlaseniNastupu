package cz.stodva.hlaseninastupu.fragments;

import android.app.AlarmManager;
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

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.Date;

import cz.stodva.hlaseninastupu.MainActivity;
import cz.stodva.hlaseninastupu.R;
import cz.stodva.hlaseninastupu.customviews.DialogInfo;
import cz.stodva.hlaseninastupu.customviews.DialogYesNo;
import cz.stodva.hlaseninastupu.listeners.OnItemsCountCheckedListener;
import cz.stodva.hlaseninastupu.listeners.OnItemsLoadedListener;
import cz.stodva.hlaseninastupu.listeners.OnNextLastReportLoadedListener;
import cz.stodva.hlaseninastupu.listeners.OnReportAddedListener;
import cz.stodva.hlaseninastupu.listeners.YesNoSelectedListener;
import cz.stodva.hlaseninastupu.objects.Report;
import cz.stodva.hlaseninastupu.receivers.MessageDeliveredReceiver;
import cz.stodva.hlaseninastupu.receivers.MessageSentReceiver;
import cz.stodva.hlaseninastupu.receivers.TimerReceiver;
import cz.stodva.hlaseninastupu.utils.Animators;
import cz.stodva.hlaseninastupu.utils.AppConstants;
import cz.stodva.hlaseninastupu.utils.AppUtils;

public class FragmentMain extends Fragment implements AppConstants {

    MainActivity activity;
    TelephonyManager telephonyManager;
    PhoneStateListener phoneStateListener;

    TextView btnStartShift, btnEndShift, btnSetTimeForReport;
    TextView labelLastMessageType, labelLastReportTime, labelLastMessage;
    TextView labelNextMessageType, labelNextReportTime, labelNextMessage;
    TextView labelCount;
    TextView labelAutomatNext, labelAutomatLast;
    ImageView imgLastSent, imgLastDelivered, imgLastWarn;
    ImageView imgNextSent, imgNextDelivered, imgNextWarn;

    RelativeLayout layoutNextReport, layoutLastReport;


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

        layoutNextReport = view.findViewById(R.id.layoutNextReport);
        layoutLastReport = view.findViewById(R.id.layoutLastReport);

        labelLastMessageType = view.findViewById(R.id.labelLastMessageType);
        labelLastReportTime = view.findViewById(R.id.labelLastReportTime);
        labelLastMessage = view.findViewById(R.id.labelLastMessage);
        labelNextMessageType = view.findViewById(R.id.labelNextMessageType);
        labelNextReportTime = view.findViewById(R.id.labelNextReportTime);
        labelNextMessage = view.findViewById(R.id.labelNextMessage);

        labelAutomatNext = view.findViewById(R.id.labelAutomatNext);
        labelAutomatLast = view.findViewById(R.id.labelAutomatLast);

        labelCount = view.findViewById(R.id.labelCount);

        imgLastSent = view.findViewById(R.id.imgLastSent);
        imgLastDelivered = view.findViewById(R.id.imgLastDelivered);
        imgLastWarn = view.findViewById(R.id.imgLastWarn);
        imgNextSent = view.findViewById(R.id.imgNextSent);
        imgNextDelivered = view.findViewById(R.id.imgNextDelivered);
        imgNextWarn = view.findViewById(R.id.imgNextWarn);

        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        updateInfo();

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
                activity.showFragment(FRAGMENT_TIMER_NAME, null);
            }
        });

        layoutNextReport.setOnLongClickListener(activity.onLongClickListener);
        layoutLastReport.setOnLongClickListener(activity.onLongClickListener);
    }

    // Voláno při okamžitém odeslání hlášení (bez budíku)
    private void requestSendReport(final int messageType) {
        final String sap = activity.getSap();
        final String phone = activity.getPhoneNumber();
        final String text = activity.getMessage(messageType);

        if (sap == null) {
            DialogInfo.createDialog(activity).setTitle("Chyba").setMessage("Hlášení nelze odeslat - není nastaven SAP.").show();
            return;
        }

        if (!activity.checkSmsPermissionGranted()) return;

        boolean missingContactName = activity.getAppSettings().getContactName().equals("");
        String cName = "";
        if (!missingContactName) cName = " (" + activity.getAppSettings().getContactName() + ")";

        DialogYesNo.createDialog(activity)
                .setTitle(messageType == MESSAGE_TYPE_START ? "Nástup" : "Konec")
                .setMessage("Odeslat na tel. číslo " +
                        activity.getPhoneNumber() +
                        cName +
                        " hlášení " +
                        (messageType == MESSAGE_TYPE_START ? " nástupu na směnu?" : "konce směny?"))
                .setListener(new YesNoSelectedListener() {
                    @Override
                    public void yesSelected() {
                        // Zapne sledování stavu zařízení a pokud je schopné odesílat SMS, bude odesláno hlášení
                        initPhoneStateListener(phone, text, messageType);

                        /*
                        activity.actualReport = new Report();
                        activity.actualReport.setMessageType(messageType);
                        activity.actualReport.setTime(new Date().getTime());
                        activity.actualReport.setSentTime(WAITING);
                        activity.actualReport.setDeliveryTime(WAITING);
                        activity.actualReport.setRequestCodeForErrorAlarm(activity.getTimerRequestCode());

                        activity.addReportToDatabase(activity.actualReport, new OnReportAddedListener() {
                            @Override
                            public void onReportAdded(final Report addedReport) {
                                activity.actualReport.setId(addedReport.getId());

                                // Zapne sledování stavu zařízení a pokud je schopné odesílat SMS, bude odesláno hlášení
                                initPhoneStateListener(phone, text, activity.actualReport);
                            }
                        });
                        */
                    }

                    @Override public void noSelected() {}
                }).show();
    }

    private void sendSms(final String phone, final String text, Report report) {

        // Nastavení časovače pro upozornění chyb v odesílání nebo doručování hlášení
        activity.setOnlyErrorCheckTimer(report);

        // Sledování úspěšného odeslání hlášení
        Intent sentIntent = new Intent(activity, MessageSentReceiver.class);
        sentIntent.putExtra("report_id", report.getId());

        AppUtils.vibrate(activity);

        PendingIntent pi1 = PendingIntent.getBroadcast(
                activity,
                1,
                sentIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        // Sledování úspěšného doručení hlášení
        Intent deliveredIntent = new Intent(activity, MessageDeliveredReceiver.class);
        deliveredIntent.putExtra("report_id", report.getId());

        PendingIntent pi2 = PendingIntent.getBroadcast(
                activity,
                2,
                deliveredIntent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        SmsManager sm = SmsManager.getDefault();
        sm.sendTextMessage(phone, null, text, pi1, pi2);
    }

    public void updateInfo() {
        Log.d(LOG_TAG, "(1001) FragmentMain - updateReportInfo()");

        activity.getDataSource().getNextLastReport(new OnNextLastReportLoadedListener() {
            @Override
            public void onNextLastReportLoaded(Report[] result) {

                // Následující hlášení
                if (result[0] == null) {
                    layoutNextReport.setVisibility(View.GONE);
                } else {
                    layoutNextReport.setVisibility(View.VISIBLE);
                    layoutNextReport.setTag(result[0]);

                    if (result[0].isAutomat()) labelAutomatNext.setVisibility(View.VISIBLE);
                    else labelAutomatNext.setVisibility(View.GONE);

                    labelNextMessageType.setText(result[0].getMessageType() == MESSAGE_TYPE_START ? "NÁSTUP" : "KONEC");
                    labelNextReportTime.setText(AppUtils.timeToString(result[0].getTime(), REPORT_PHASE_NONE));

                    if (result[0].getMessage() == null) {
                        labelNextMessage.setVisibility(View.GONE);
                    } else {
                        labelNextMessage.setVisibility(View.VISIBLE);
                        labelNextMessage.setText(result[0].getMessage());
                    }

                    boolean isSent = result[0].getSentTime() > NONE;
                    boolean isDelivered = result[0].getDeliveryTime() > NONE;

                    if (isSent) imgNextSent.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_green));
                    else imgNextSent.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_gray));

                    if (isDelivered) imgNextDelivered.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_green));
                    else imgNextDelivered.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_gray));

                    showWarn(result[0].isFailed(), REPORT_INFO_TYPE_NEXT);
                }

                // Poslední hlášení
                if (result[1] == null) {
                    layoutLastReport.setVisibility(View.GONE);
                } else {
                    layoutLastReport.setVisibility(View.VISIBLE);
                    layoutLastReport.setTag(result[1]);

                    if (result[1].isAutomat()) labelAutomatLast.setVisibility(View.VISIBLE);
                    else labelAutomatLast.setVisibility(View.GONE);

                    labelLastMessageType.setText(result[1].getMessageType() == MESSAGE_TYPE_START ? "NÁSTUP" : "KONEC");
                    labelLastReportTime.setText(AppUtils.timeToString(result[1].getTime(), REPORT_PHASE_NONE));

                    if (result[1].getMessage() == null) {
                        labelLastMessage.setVisibility(View.GONE);
                    } else {
                        labelLastMessage.setVisibility(View.VISIBLE);
                        labelLastMessage.setText(result[1].getMessage());
                    }

                    boolean isSent = result[1].getSentTime() > NONE;
                    boolean isDelivered = result[1].getDeliveryTime() > NONE;

                    if (isSent) imgLastSent.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_green));
                    else imgLastSent.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_gray));

                    if (isDelivered) imgLastDelivered.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_green));
                    else imgLastDelivered.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_gray));

                    showWarn(result[1].isFailed(), REPORT_INFO_TYPE_LAST);
                }
            }
        });

        activity.getDataSource().getWaitingItemsCount(new OnItemsCountCheckedListener() {
            @Override
            public void onItemsCountChecked(int count) {
                labelCount.setText("" + count);
            }
        });
    }

    public void showWarn(boolean show, int reportInfoType) {
        Log.d(LOG_TAG, "(1002) FragmentMain - showWarn()");

        if (reportInfoType == REPORT_INFO_TYPE_NEXT) {
            imgNextWarn.setVisibility(show ? View.VISIBLE : View.GONE);
        } else {
            imgLastWarn.setVisibility(show ? View.VISIBLE : View.GONE);
        }
    }

    public void initPhoneStateListener(final String phone, final String text, final int messageType) {
        Log.d(LOG_TAG_SMS, "(1003) FragmentMain - initPhoneStateListener()");
        if (telephonyManager == null) {
            Log.d(LOG_TAG_SMS, "(1004) telephonyManager == null -> new init");
            telephonyManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
        }

        if (phoneStateListener == null) {
            Log.d(LOG_TAG_SMS, "(1005) phoneStateListener == null -> new init");

            phoneStateListener = new PhoneStateListener() {
                @Override
                public void onServiceStateChanged(ServiceState serviceState) {
                    super.onServiceStateChanged(serviceState);

                    switch (serviceState.getState()) {
                        case ServiceState.STATE_IN_SERVICE:
                            Log.d(LOG_TAG_SMS, "(1006) FragmentMain - onServiceStateChanged: STATE_IN_SERVICE");

                            activity.actualReport = new Report();
                            activity.actualReport.setMessageType(messageType);
                            activity.actualReport.setTime(new Date().getTime());
                            activity.actualReport.setSentTime(WAITING);
                            activity.actualReport.setDeliveryTime(WAITING);
                            activity.actualReport.setRequestCodeForErrorAlarm(activity.getTimerRequestCode());

                            activity.addReportToDatabase(activity.actualReport, new OnReportAddedListener() {
                                @Override
                                public void onReportAdded(final Report addedReport) {
                                    activity.actualReport.setId(addedReport.getId());
                                    sendSms(phone, text, addedReport);
                                }
                            });

                            break;
                        case ServiceState.STATE_OUT_OF_SERVICE:
                            Log.d(LOG_TAG_SMS, "(1007) FragmentMain - onServiceStateChanged: STATE_OUT_OF_SERVICE: ");
                            showPhoneStateError("HLÁŠENÍ NELZE ODESLAT - není dostupná síť");
                            break;
                        case ServiceState.STATE_EMERGENCY_ONLY:
                            Log.d(LOG_TAG_SMS, "(1008) FragmentMain - onServiceStateChanged: STATE_EMERGENCY_ONLY");
                            showPhoneStateError("HLÁŠENÍ NELZE ODESLAT - je povoleno pouze tísňové volání");
                            break;
                        case ServiceState.STATE_POWER_OFF:
                            Log.d(LOG_TAG_SMS, "(1009) FragmentMain - onServiceStateChanged: STATE_POWER_OFF");
                            showPhoneStateError("HLÁŠENÍ NELZE ODESLAT - je zapnut režim letadlo");
                            break;
                        default:
                            Log.d(LOG_TAG_SMS, "(1010) FragmentMain - onServiceStateChanged: UNKNOWN_STATE");
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
        Log.d(LOG_TAG_SMS, "(1011) FragmentMain - cancelPhoneStateListener()");

        telephonyManager = (TelephonyManager) activity.getSystemService(Context.TELEPHONY_SERVICE);
        telephonyManager.listen(phoneStateListener, PhoneStateListener.LISTEN_NONE);

        telephonyManager = null;
        phoneStateListener = null;
    }

    private void showPhoneStateError(String message) {
        DialogInfo.createDialog(activity).setTitle("Chyba").setMessage(message).show();
    }
}
