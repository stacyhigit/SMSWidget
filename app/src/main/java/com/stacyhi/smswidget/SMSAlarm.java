package com.stacyhi.smswidget;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.util.Calendar;

public class SMSAlarm {
    public static final String TAG = "SMSAlarm";
    private final int ALARM_ID = 1;
    private final Context context;

    public SMSAlarm(Context context) { this.context = context;}
    public void startAlarm(int smsUpdateInterval) {
        stopAlarm();
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, smsUpdateInterval);

        Intent alarmIntent=new Intent(context, SMSWidgetProvider.class);
        alarmIntent.setAction(SMSWidgetProvider.ACTION_REFRESH);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        Log.d(TAG, "startAlarm: in " + smsUpdateInterval + " minutes");
        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC, calendar.getTimeInMillis(), (smsUpdateInterval * 60000), pendingIntent);
    }


    public void stopAlarm() {
        Intent alarmIntent = new Intent(SMSWidgetProvider.ACTION_REFRESH);
        PendingIntent pendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            pendingIntent = PendingIntent.getBroadcast(context, ALARM_ID, alarmIntent, PendingIntent.FLAG_UPDATE_CURRENT);
        }

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }
}
