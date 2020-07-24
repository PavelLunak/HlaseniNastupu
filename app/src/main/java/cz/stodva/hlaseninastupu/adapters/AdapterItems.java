package cz.stodva.hlaseninastupu.adapters;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Date;

import cz.stodva.hlaseninastupu.MainActivity;
import cz.stodva.hlaseninastupu.R;
import cz.stodva.hlaseninastupu.customviews.DialogSelect;
import cz.stodva.hlaseninastupu.listeners.OnItemDeletedListener;
import cz.stodva.hlaseninastupu.objects.Report;
import cz.stodva.hlaseninastupu.utils.Animators;
import cz.stodva.hlaseninastupu.utils.AppConstants;
import cz.stodva.hlaseninastupu.utils.AppUtils;


public class AdapterItems extends RecyclerView.Adapter<AdapterItems.MyViewHolder> implements AppConstants {

    static class MyViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout root;
        TextView labelMessageType;
        TextView labelReportTime;
        TextView labelDeliveryTime;
        TextView labelTimeOfSending;
        TextView labelMessage;
        ImageView img;
        ImageView imgWaitSend;
        ImageView imgWaitDelivery;

        // Detaily hlášení (pro ladění aplikace)
        ConstraintLayout layoutDetails;
        TextView labelId;
        TextView labelTime;
        TextView labelMsgType;
        TextView labelSent;
        TextView labelDelivered;
        TextView labelCodeAlarm;
        TextView labelCodeError;

        public MyViewHolder(View itemView) {
            super(itemView);
        }
    }

    MainActivity activity;

    public AdapterItems(MainActivity activity) {
        this.activity = activity;
    }

    @Override
    public MyViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item, parent, false);

        MyViewHolder vh = new MyViewHolder(v);

        vh.root = v.findViewById(R.id.root);
        vh.labelMessageType = v.findViewById(R.id.labelMessageType);
        vh.labelReportTime = v.findViewById(R.id.labelReportTime);
        vh.labelDeliveryTime = v.findViewById(R.id.labelDeliveryTime);
        vh.labelTimeOfSending = v.findViewById(R.id.labelTimeOfSending);
        vh.labelMessage = v.findViewById(R.id.labelMessage);
        vh.img = v.findViewById(R.id.img);
        vh.imgWaitSend = v.findViewById(R.id.imgWaitSend);
        vh.imgWaitDelivery = v.findViewById(R.id.imgWaitDelivery);


        // Detaily hlášení (pro ladění aplikace)
        vh.layoutDetails = v.findViewById(R.id.layoutDetails);
        vh.labelId = v.findViewById(R.id.labelId);
        vh.labelTime = v.findViewById(R.id.labelTime);
        vh.labelMsgType = v.findViewById(R.id.labelMsgType);
        vh.labelSent = v.findViewById(R.id.labelSent);
        vh.labelDelivered = v.findViewById(R.id.labelDelivered);
        vh.labelCodeAlarm = v.findViewById(R.id.labelCodeAlarm);
        vh.labelCodeError = v.findViewById(R.id.labelCodeError);

        return vh;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        final Report report = activity.getItems().get(position);

        View.OnLongClickListener onLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {

                final Report reportToUpdate = activity.items.get(position);

                AppUtils.vibrate(activity);

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

                DialogSelect.createDialog(activity)
                        .setTitle("Úprava hlášení...")
                        .setItems(itemsToArray)
                        .setListener(new DialogSelect.OnDialogSelectItemSelectedListener() {
                            @Override
                            public void onDialogSelectItemSelected(String selectedItem) {
                                if (selectedItem.equals("Odstranit hlášení")) {
                                    activity.cancelTimer(reportToUpdate, false);

                                    activity.getDataSource().removeItem(reportToUpdate.getId(), new OnItemDeletedListener() {
                                        @Override
                                        public void onItemDeleted() {
                                            activity.updateItems(null);
                                        }
                                    });
                                } else if (selectedItem.equals("Obnovit automatické hlášení")) {
                                    activity.setTimer(reportToUpdate);
                                } else if (selectedItem.equals("Zrušit hlášení")) {
                                    activity.cancelTimer(reportToUpdate, true);
                                } else if (selectedItem.equals("Upravit hlášení")) {
                                    activity.actualReport = reportToUpdate;
                                    Bundle args = new Bundle();
                                    args.putBoolean("edit", true);
                                    activity.showFragment(FRAGMENT_TIMER_NAME, args);
                                } else if (selectedItem.equals("Použít hlášení")) {
                                    activity.actualReport = reportToUpdate;
                                    Bundle args = new Bundle();
                                    args.putBoolean("use", true);
                                    activity.showFragment(FRAGMENT_TIMER_NAME, args);
                                }
                            }
                        }).show();

                return true;
            }
        };

        holder.root.setOnLongClickListener(onLongClickListener);
        holder.labelMessageType.setOnLongClickListener(onLongClickListener);
        holder.labelReportTime.setOnLongClickListener(onLongClickListener);
        holder.labelDeliveryTime.setOnLongClickListener(onLongClickListener);
        holder.labelTimeOfSending.setOnLongClickListener(onLongClickListener);
        holder.labelMessage.setOnLongClickListener(onLongClickListener);
        holder.img.setOnLongClickListener(onLongClickListener);
        holder.imgWaitSend.setOnLongClickListener(onLongClickListener);
        holder.imgWaitDelivery.setOnLongClickListener(onLongClickListener);


        // Detaily hlášení (pro ladění aplikace)
        if (activity.getAppSettings().isShowItemDetails()) {
            holder.layoutDetails.setVisibility(View.VISIBLE);

            holder.labelId.setOnLongClickListener(onLongClickListener);
            holder.labelTime.setOnLongClickListener(onLongClickListener);
            holder.labelMsgType.setOnLongClickListener(onLongClickListener);
            holder.labelSent.setOnLongClickListener(onLongClickListener);
            holder.labelDelivered.setOnLongClickListener(onLongClickListener);
            holder.labelCodeAlarm.setOnLongClickListener(onLongClickListener);
            holder.labelCodeError.setOnLongClickListener(onLongClickListener);

            holder.labelId.setText("" + report.getId());
            holder.labelTime.setText(AppUtils.timeToString(report.getTime(), REPORT_PHASE_NONE));
            holder.labelMsgType.setText(report.getMessageType() == MESSAGE_TYPE_START ? "Nástup" : "Konec");
            holder.labelSent.setText(AppUtils.timeToString(report.getSentTime(), AppConstants.REPORT_PHASE_SEND));
            holder.labelDelivered.setText(AppUtils.timeToString(report.getDeliveryTime(), AppConstants.REPORT_PHASE_DELIVERY));
            holder.labelCodeAlarm.setText("" + report.getAlarmRequestCode());
            holder.labelCodeError.setText("" + report.getRequestCodeForErrorAlarm());
        } else {
            holder.layoutDetails.setVisibility(View.GONE);
        }

        // Moc velký časový rozdíl mezi odesláním a doručením
        if (((report.getDeliveryTime() - report.getSentTime()) >= TIME_FOR_CONTROL) && report.getRequestCodeForErrorAlarm() > NONE) {
            holder.labelDeliveryTime.setBackground(AppCompatResources.getDrawable(activity, R.drawable.bg_late_delivery));
            holder.labelDeliveryTime.setTextColor(activity.getResources().getColor(R.color.white));
        } else {
            holder.labelDeliveryTime.setBackground(null);
            holder.labelDeliveryTime.setTextColor(activity.getResources().getColor(R.color.white));
        }

        holder.labelMessageType.setText(report.getMessageType() == MESSAGE_TYPE_START ? "Nástup" : "Konec");
        holder.labelReportTime.setText(AppUtils.timeToString(report.getTime(), AppConstants.REPORT_PHASE_NONE));

        if (report.getSentTime() == NONE) {
            holder.labelTimeOfSending.setText("");
        } else {
            holder.labelTimeOfSending.setText(AppUtils.timeToString(report.getSentTime(), AppConstants.REPORT_PHASE_SEND));

            if (report.getSentTime() == WAITING) {
                holder.imgWaitSend.setVisibility(View.VISIBLE);
                holder.labelTimeOfSending.setTextColor(activity.getResources().getColor(R.color.waiting_color));
            } else {
                holder.imgWaitSend.setVisibility(View.GONE);
                holder.labelTimeOfSending.setTextColor(activity.getResources().getColor(R.color.white));
            }
        }

        if (report.getDeliveryTime() == NONE) {
            holder.labelDeliveryTime.setText("?");
        } else {
            holder.labelDeliveryTime.setText(AppUtils.timeToString(report.getDeliveryTime(), AppConstants.REPORT_PHASE_DELIVERY));

            if (report.getDeliveryTime() == WAITING) {
                holder.imgWaitDelivery.setVisibility(View.VISIBLE);
                holder.labelDeliveryTime.setTextColor(activity.getResources().getColor(R.color.waiting_color));
            } else {
                holder.imgWaitDelivery.setVisibility(View.GONE);
                holder.labelDeliveryTime.setTextColor(activity.getResources().getColor(R.color.white));
            }
        }

        // VZHLED POLOŽKY --------------------------------------------------------------------------
        // Neúspěšné doručení hlášení
        if (report.isFailed()) {
            holder.root.setBackgroundResource(R.drawable.bg_item_failed);

            holder.labelMessageType.setTextColor(activity.getResources().getColor(R.color.end_color_item));
            holder.labelReportTime.setTextColor(activity.getResources().getColor(R.color.end_color_item));
        }
        // Hlášení zrušeno uživatelem
        else if (report.getDeliveryTime() == CANCELED) {
            if (report.getMessageType() == MESSAGE_TYPE_START) holder.root.setBackgroundResource(R.drawable.bg_item_old_start);
            else holder.root.setBackgroundResource(R.drawable.bg_item_old_end);

            holder.labelMessageType.setTextColor(activity.getResources().getColor(R.color.old_color_item));
            holder.labelReportTime.setTextColor(activity.getResources().getColor(R.color.old_color_item));
        }
        // Úspěšné doručené hlášení
        else if (report.isDelivered()) {
            if (report.getMessageType() == MESSAGE_TYPE_START)
                holder.root.setBackgroundResource(R.drawable.bg_item_old_start);
            else holder.root.setBackgroundResource(R.drawable.bg_item_old_end);

            holder.labelMessageType.setTextColor(activity.getResources().getColor(R.color.old_color_item));
            holder.labelReportTime.setTextColor(activity.getResources().getColor(R.color.old_color_item));
        }
        // Hlášení nástupu
        else if (report.getMessageType() == MESSAGE_TYPE_START) {
            holder.root.setBackgroundResource(R.drawable.bg_item_start);

            holder.labelMessageType.setTextColor(activity.getResources().getColor(R.color.start_color_item));
            holder.labelReportTime.setTextColor(activity.getResources().getColor(R.color.start_color_item));
        }
        // Hlášení konce
        else if (report.getMessageType() == MESSAGE_TYPE_END) {
            holder.root.setBackgroundResource(R.drawable.bg_item_end);

            holder.labelMessageType.setTextColor(activity.getResources().getColor(R.color.end_color_item));
            holder.labelReportTime.setTextColor(activity.getResources().getColor(R.color.end_color_item));
        }

        if (report.getMessage() == null) {
            holder.labelMessage.setVisibility(View.GONE);
        } else {
            holder.labelMessage.setVisibility(View.VISIBLE);
            holder.labelMessage.setText(report.getMessage());
        }

        // VZHLED IKONY POLOLŽKY -------------------------------------------------------------------
        // Hlášení bylo úspěšně odesláno i doručeno
        if (report.getSentTime() > NONE && report.getDeliveryTime() > NONE) {
            holder.img.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_yellow));
        }
        // Hlášení čeká na odeslání nebo doručení
        else if (report.getSentTime() == WAITING || report.getDeliveryTime() == WAITING) {
            if (report.isErrorAlert()) holder.img.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_alarm_on));
            else holder.img.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_alert_add));
        }
        // Hlášení se nepodařilo odeslat nebo doručit v nastaveném časovém limitu
        else if (report.getSentTime() == UNSUCCESFUL || report.getDeliveryTime() == UNSUCCESFUL) {
            holder.img.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_warning));
            holder.root.setBackgroundResource(R.drawable.bg_item_failed);
        }
        // Hlášení bylo zrušeno uživatelem
        else if (report.getSentTime() == CANCELED) {
            holder.img.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_canceled));
        }

        holder.img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (report.getSentTime() == WAITING || report.getDeliveryTime() == WAITING) {
                    Animators.animateButtonClick(holder.img, true);
                    activity.updateErrorAlert(report, !report.isErrorAlert());
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return activity.getItems().size();
    }
}
