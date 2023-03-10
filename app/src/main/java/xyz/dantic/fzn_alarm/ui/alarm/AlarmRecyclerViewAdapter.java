package xyz.dantic.fzn_alarm.ui.alarm;


import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

import xyz.dantic.fzn_alarm.R;

/**
 * Responsible for populating each alarm item card
 */
public class AlarmRecyclerViewAdapter extends RecyclerView.Adapter<AlarmRecyclerViewAdapter.ViewHolder> {

    private AlarmViewModel alarmViewModel;
    private ArrayList<Alarm> alarmList;
    private Context context;
    private Fragment fragment;

    public AlarmRecyclerViewAdapter(AlarmViewModel alarmViewModel, Fragment fragment) {
        this.alarmViewModel = alarmViewModel;
        alarmList = alarmViewModel.getAlarmList();
        this.fragment = fragment;
    }

    @NonNull
    @Override
    public AlarmRecyclerViewAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);

        // Inflate the custom layout
        View alarmView = inflater.inflate(R.layout.alarm_item, parent, false);

        // Return a new holder instance
        ViewHolder viewHolder = new ViewHolder(alarmView);
        return viewHolder;
    }

    @Override
    public void onBindViewHolder(@NonNull AlarmRecyclerViewAdapter.ViewHolder holder, int position) {
        // Get the data model based on position
        Alarm alarm = alarmList.get(position);

        // Set item views based the data model
        holder.onSwitch.setChecked(alarm.isOn());
        holder.timeTextView.setText(alarm.getTimeAsString());
        String daysForGongText = alarm.getGongDaysAsString(context);
        String daysForNotifText = alarm.getNotifDaysAsString(context);
        if (daysForGongText.equals("")) {
            holder.daysForGongTextView.setVisibility(View.GONE);
            holder.gongSoundTextView.setVisibility(View.GONE);
            holder.gongIconImageView.setVisibility(View.GONE);
        } else {
            holder.daysForGongTextView.setText(daysForGongText);
        }
        if (daysForNotifText.equals("")) {
            holder.daysForNotifTextView.setVisibility(View.GONE);
            holder.notifSoundTextView.setVisibility(View.GONE);
            holder.notifIconImageView.setVisibility(View.GONE);
        } else {
            holder.daysForNotifTextView.setText(daysForNotifText);
        }

        // Set listeners
        holder.onSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean checked) {
                int updatedPosition = holder.getAdapterPosition();
                alarmViewModel.toggleAlarm(updatedPosition, checked);

                // Check to handle edge case where all days were previously manually turned off
                boolean allOff = true;
                for (int i = 0; i < 7; i++) {
                    if (alarm.getSoundOnDay(i) != Alarm.Sound.OFF) {
                        allOff = false;
                        break;
                    }
                }
                if (allOff) {
                    for (int i = 0; i < 7; i++) {
                        alarm.setSoundOnDay(i, Alarm.Sound.GONG);
                    }
                    alarmViewModel.replaceAlarm(alarm, updatedPosition, false);
                    holder.daysForGongTextView.setText(alarm.getGongDaysAsString(context, true));
                    holder.daysForGongTextView.setVisibility(View.VISIBLE);
                    holder.gongSoundTextView.setVisibility(View.VISIBLE);
                    holder.gongIconImageView.setVisibility(View.VISIBLE);
                }
            }
        });

        // If a card is clicked, open the editor
        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                ((AlarmContainerFragment) fragment.getParentFragment()).insertAlarmEditorFragment(position);
            }
        });

    }

    @Override
    public int getItemCount() {
        return alarmList.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        public TextView timeTextView;
        public Switch onSwitch;
        public TextView daysForGongTextView;
        public TextView daysForNotifTextView;
        public TextView gongSoundTextView;
        public TextView notifSoundTextView;
        public ImageView gongIconImageView;
        public ImageView notifIconImageView;

        public ViewHolder(View itemView) {
            super(itemView);
            timeTextView = (TextView) itemView.findViewById(R.id.text_time);
            onSwitch = (Switch) itemView.findViewById(R.id.switch_alarm_time);
            daysForGongTextView = (TextView) itemView.findViewById(R.id.text_days_for_gong);
            daysForNotifTextView = (TextView) itemView.findViewById(R.id.text_days_for_notif);
            gongSoundTextView = (TextView) itemView.findViewById(R.id.text_gong_sound);
            notifSoundTextView = (TextView) itemView.findViewById(R.id.text_notif_sound);
            gongIconImageView = (ImageView) itemView.findViewById(R.id.icon_gong_id);
            notifIconImageView = (ImageView) itemView.findViewById(R.id.icon_notif_id);
        }
    }
}
