package cz.stodva.hlaseninastupu.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.content.res.AppCompatResources;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import cz.stodva.hlaseninastupu.MainActivity;
import cz.stodva.hlaseninastupu.R;
import cz.stodva.hlaseninastupu.customviews.DialogYesNo;
import cz.stodva.hlaseninastupu.database.DbHelper;
import cz.stodva.hlaseninastupu.listeners.OnItemDeletedListener;
import cz.stodva.hlaseninastupu.listeners.OnItemsLoadedListener;
import cz.stodva.hlaseninastupu.listeners.YesNoSelectedListener;
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

        // TEST
        TextView labelId;
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


        // TEST
        vh.labelId = v.findViewById(R.id.labelId);
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

                Report reportToCancel = activity.items.get(position);

                AppUtils.vibrate(activity);

                // Úplné odstranění zrušeného nebo starého hlášení z databáze
                if (reportToCancel.getTime() == CANCELED || reportToCancel.getTime() < new Date().getTime() || reportToCancel.getAlarmRequestCode() <= NONE) {
                    DialogYesNo.createDialog(activity)
                            .setTitle("")
                            .setMessage("Odstranit toto hlášení?")
                            .setListener(new YesNoSelectedListener() {
                                @Override
                                public void yesSelected() {
                                    activity.getDataSource().removeItem(activity.items.get(position).getId(), new OnItemDeletedListener() {
                                        @Override
                                        public void onItemDeleted() {
                                            activity.getDataSource().getAllItems(new OnItemsLoadedListener() {
                                                @Override
                                                public void onItemsLoaded(ArrayList<Report> loadedItems) {
                                                    activity.items = new ArrayList<Report>(loadedItems);
                                                    notifyDataSetChanged();
                                                }
                                            });
                                        }
                                    });
                                }

                                @Override public void noSelected() {}
                            }).show();
                }
                // Deaktivace čekajícího hlášení
                else {
                    DialogYesNo.createDialog(activity)
                            .setTitle("")
                            .setMessage("Zrušit toto hlášení?")
                            .setListener(new YesNoSelectedListener() {
                                @Override
                                public void yesSelected() {
                                    activity.cancelTimer(activity.items.get(position));
                                    activity.cancelTimerForError(activity.items.get(position));
                                    // notifyDataSetChanged() je voláno v metodách cancelTimer()
                                    // a cancelTimerForError() po aktualizaci dat v databázi
                                }

                                @Override public void noSelected() {}
                            }).show();
                }

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

        // TEST
        holder.labelId.setOnLongClickListener(onLongClickListener);
        holder.labelMsgType.setOnLongClickListener(onLongClickListener);
        holder.labelSent.setOnLongClickListener(onLongClickListener);
        holder.labelDelivered.setOnLongClickListener(onLongClickListener);
        holder.labelCodeAlarm.setOnLongClickListener(onLongClickListener);
        holder.labelCodeError.setOnLongClickListener(onLongClickListener);

        // TEST
        holder.labelId.setText("" + report.getId());
        holder.labelMsgType.setText(report.getMessageType() == MESSAGE_TYPE_START ? "Nástup" : "Konec");
        holder.labelSent.setText(AppUtils.timeToString(report.getSentTime()));
        holder.labelDelivered.setText(AppUtils.timeToString(report.getDeliveryTime()));
        holder.labelCodeAlarm.setText("" + report.getAlarmRequestCode());
        holder.labelCodeError.setText("" + report.getRequestCodeForErrorAlarm());

        holder.labelMessageType.setText(report.getMessageType() == MESSAGE_TYPE_START ? "Nástup" : "Konec");
        holder.labelReportTime.setText(AppUtils.timeToString(report.getTime()));

        if (report.getSentTime() == 0) holder.labelTimeOfSending.setText("");
        else holder.labelTimeOfSending.setText(AppUtils.timeToString(report.getSentTime()));

        if (report.getDeliveryTime() == 0) holder.labelDeliveryTime.setText("?");
        else holder.labelDeliveryTime.setText(AppUtils.timeToString(report.getDeliveryTime()));

        if (report.getMessageType() == MESSAGE_TYPE_START)
            holder.root.setBackgroundResource(R.drawable.bg_item_start);
        else
            holder.root.setBackgroundResource(R.drawable.bg_item_end);

        if (report.getMessage() == null) {
            holder.labelMessage.setVisibility(View.GONE);
        } else {
            holder.labelMessage.setVisibility(View.VISIBLE);
            holder.labelMessage.setText(report.getMessage());
        }

        // Hlášení bylo úspěšně odesláno i doručeno
        if (report.getSentTime() > NONE && report.getDeliveryTime() > NONE) {
            holder.img.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_check_yellow));
        }
        // Hlášení čeká na odeslání nebo doručení
        else if (report.getSentTime() == WAITING || report.getDeliveryTime() == WAITING){
            if (report.getRequestCodeForErrorAlarm() > NONE) holder.img.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_alarm_on));
            else holder.img.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_alert_add));
        }
        // Hlášení se nepodařilo odeslat nebo doručit v nastaveném časovém limitu
        else if (report.getSentTime() == UNSUCCESFUL || report.getDeliveryTime() == UNSUCCESFUL) {
            holder.img.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_warning));
            holder.root.setBackgroundResource(R.drawable.bg_item_failed);
        }
        // Hlášení bylo zrušeno uživatelem
        else if (report.getTime() == CANCELED) {
            holder.img.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_canceled));
        }

        holder.img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (report.getSentTime() == WAITING || report.getDeliveryTime() == WAITING) {
                    Animators.animateButtonClick(holder.img, true);

                    if (report.getRequestCodeForErrorAlarm() > NONE) {
                        activity.cancelTimerForError(activity.items.get(position));
                        holder.img.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_alert_add));
                        notifyDataSetChanged();
                        Toast.makeText(activity, "Buzení při neúspěšném odeslání hlášení VYPNUTO...", Toast.LENGTH_LONG).show();
                    } else {
                        activity.items.get(position).setRequestCodeForErrorAlarm(activity.getTimerRequestCode());
                        activity.setTimerForError(activity.items.get(position));
                        holder.img.setImageDrawable(AppCompatResources.getDrawable(activity, R.drawable.ic_alert_on));
                        notifyDataSetChanged();
                        Toast.makeText(activity, "Buzení při neúspěšném odeslání hlášení ZAPNUTO...", Toast.LENGTH_LONG).show();
                    }
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return activity.getItems().size();
    }
}
