package cz.stodva.hlaseninastupu.adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.recyclerview.widget.RecyclerView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;

import cz.stodva.hlaseninastupu.MainActivity;
import cz.stodva.hlaseninastupu.R;
import cz.stodva.hlaseninastupu.database.DbHelper;
import cz.stodva.hlaseninastupu.listeners.OnItemsLoadedListener;
import cz.stodva.hlaseninastupu.listeners.OnReportUpdatedListener;
import cz.stodva.hlaseninastupu.objects.Report;
import cz.stodva.hlaseninastupu.utils.Animators;
import cz.stodva.hlaseninastupu.utils.AppConstants;


public class AdapterItems extends RecyclerView.Adapter<AdapterItems.MyViewHolder> implements AppConstants {

    SimpleDateFormat sdf = new SimpleDateFormat("d.MM.yyyy  k:mm");

    static class MyViewHolder extends RecyclerView.ViewHolder {

        ConstraintLayout root;
        TextView labelMessageType;
        TextView labelReportTime;
        TextView labelDeliveryTime;
        TextView labelTimeOfSending;
        ImageView img;

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
        vh.img = v.findViewById(R.id.img);

        return vh;
    }

    @Override
    public void onBindViewHolder(final MyViewHolder holder, final int position) {
        final Report report = activity.getItems().get(position);

        holder.labelMessageType.setText(report.getMessageType() == MESSAGE_TYPE_START ? "Nástup" : "Konec");
        holder.labelReportTime.setText(sdf.format(report.getTime()));

        if (report.getSendingTime() == 0) holder.labelTimeOfSending.setText("");
        else holder.labelTimeOfSending.setText(sdf.format(report.getSendingTime()));

        if (report.getDeliveryTime() == 0) holder.labelDeliveryTime.setText("?");
        else holder.labelDeliveryTime.setText(sdf.format(report.getDeliveryTime()));

        if (report.getMessageType() == MESSAGE_TYPE_START)
            holder.root.setBackgroundResource(R.drawable.bg_item_start);
        else
            holder.root.setBackgroundResource(R.drawable.bg_item_end);

        // Hlášení bylo úspěšně odesláno i doručeno
        if (report.getSendingTime() > WAITING && report.getDeliveryTime() > WAITING) {
            holder.img.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_check_yellow));
        }
        // Hlášení čeká na odeslání nebo doručení
        else if (report.getSendingTime() == WAITING || report.getDeliveryTime() == WAITING){
            if (report.isAlarm()) holder.img.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_alarm_on));
            else holder.img.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_alarm_add));
        }
        // Hlášení se nepodařilo odeslat nebo doručit v nastaveném časovém limitu
        else if (report.getSendingTime() == UNSUCCESFUL && report.getDeliveryTime() == UNSUCCESFUL) {
            holder.img.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_warning));
            holder.root.setBackgroundResource(R.drawable.bg_item_failed);
        }

        holder.img.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (report.getSendingTime() == WAITING || report.getDeliveryTime() == WAITING) {
                    Animators.animateButtonClick(holder.img, true);

                    if (report.isAlarm()) {
                        activity.getDataSource().updateReportValue(activity.getItems().get(position).getId(), DbHelper.COLUMN_IS_ALARM, 0, new OnReportUpdatedListener() {
                            @Override
                            public void onReportUpdated(Report report) {
                                report.setAlarm(false);
                                holder.img.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_alarm_add));
                                notifyDataSetChanged();
                            }
                        });
                    } else {
                        activity.getDataSource().updateReportValue(activity.getItems().get(position).getId(), DbHelper.COLUMN_IS_ALARM, 0, new OnReportUpdatedListener() {
                            @Override
                            public void onReportUpdated(Report report) {
                                report.setAlarm(true);
                                holder.img.setImageDrawable(activity.getResources().getDrawable(R.drawable.ic_alarm_on));
                                notifyDataSetChanged();
                            }
                        });
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
