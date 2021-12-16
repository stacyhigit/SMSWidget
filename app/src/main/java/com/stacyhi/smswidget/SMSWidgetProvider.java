package com.stacyhi.smswidget;

import static android.text.format.DateUtils.isToday;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class SMSWidgetProvider extends AppWidgetProvider {
    public static final String TAG = "SMSWidgetProvider";
    public static final String ACTION_VIEW_SMS = "com.stacyhi.smsWidget.actionViewSMS";
    public static final String ACTION_REFRESH = "com.stacyhi.smsWidget.actionRefresh";
    public static final String ACTION_CREATE_SMS = "com.stacyhi.smsWidget.actionCreateSms";
    public static final String EXTRA_ITEM_POSITION = "com.stacyhi.smsWidget.extraItemPosition";
    public static final String EXTRA_ITEM_NUMBERS = "com.stacyhi.smsWidget.extraItemNumbers";
    public static final String EXTRA_ITEM_ID = "com.stacyhi.smsWidget.extraItemId";

    static void updateAppWidget(Context context, AppWidgetManager appWidgetManager,
                                int appWidgetId) {
        String dateString = getFormattedDate(new Date().getTime());

        Intent refreshIntent = new Intent(context, SMSWidgetProvider.class);
        refreshIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        refreshIntent.setAction(ACTION_REFRESH);
        PendingIntent refreshPendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            refreshPendingIntent = PendingIntent.getBroadcast(
                    context, appWidgetId, refreshIntent,  PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        } else {
            refreshPendingIntent = PendingIntent.getBroadcast(
                    context, appWidgetId, refreshIntent,  PendingIntent.FLAG_UPDATE_CURRENT);
        }


        Intent configIntent = new Intent(context, SMSWidgetConfigureActivity.class);
        configIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        configIntent.setData(Uri.parse(configIntent.toUri(Intent.URI_INTENT_SCHEME)));
        configIntent.setAction("android.appwidget.action.APPWIDGET_CONFIGURE");
        PendingIntent configPendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            configPendingIntent = PendingIntent.getActivity(context, appWidgetId, configIntent, 0 | PendingIntent.FLAG_IMMUTABLE);
        } else {
            configPendingIntent = PendingIntent.getActivity(context, appWidgetId, configIntent, 0);
        }

        Intent createSmsIntent = new Intent(context, SMSWidgetProvider.class);
        createSmsIntent.setAction(ACTION_CREATE_SMS);
        PendingIntent createSmsPendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            createSmsPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, createSmsIntent, 0 | PendingIntent.FLAG_IMMUTABLE);
        } else {
            createSmsPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, createSmsIntent, 0);
        }

        Intent serviceIntent = new Intent(context, SMSWidgetService.class);
        serviceIntent.putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, appWidgetId);
        serviceIntent.setData(Uri.parse(serviceIntent.toUri(Intent.URI_INTENT_SCHEME)));

        Intent clickIntent = new Intent(context, SMSWidgetProvider.class);
        clickIntent.setAction(ACTION_VIEW_SMS);
        PendingIntent clickPendingIntent;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            clickPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, clickIntent, 0 | PendingIntent.FLAG_MUTABLE);
        } else {
            clickPendingIntent = PendingIntent.getBroadcast(context, appWidgetId, clickIntent, 0);
        }

        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.sms_widget_provider);
        views.setViewVisibility(R.id.pb_refresh, View.GONE);
        views.setViewVisibility(R.id.iv_refresh, View.VISIBLE);

        if (!PermissionManager.checkAllPermissions(context)) {
            views.setTextViewText(R.id.tv_empty_listview, context.getResources().getString(R.string.add_permissions));

        } else {
            views.setTextViewText(R.id.tv_updated, "Updated " + dateString);
            views.setEmptyView(R.id.lv_messages, R.id.tv_empty_listview);
            views.setRemoteAdapter(R.id.lv_messages, serviceIntent);
            views.setPendingIntentTemplate(R.id.lv_messages, clickPendingIntent);
        }

        views.setOnClickPendingIntent(R.id.fl_refresh, refreshPendingIntent);
        views.setOnClickPendingIntent(R.id.iv_configure, configPendingIntent);
        views.setOnClickPendingIntent(R.id.iv_edit_sms, createSmsPendingIntent);
        appWidgetManager.updateAppWidget(appWidgetId, views);
        appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lv_messages);
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
        Log.d(TAG, "onUpdate");
        //TODO: Make update interval configurable
        int smsUpdateInterval = SMSWidgetConfigureActivity.loadPref(context, 0, SMSWidgetConfigureActivity.KEY_SMS_UPDATE_INTERVAL, 10);
        Log.d(TAG, "onUpdate: Adding alarm to update widget every " + smsUpdateInterval + " minutes");

        SMSAlarm smsAlarm = new SMSAlarm(context.getApplicationContext());
        smsAlarm.startAlarm(smsUpdateInterval);

        for (int appWidgetId : appWidgetIds) {
            updateAppWidget(context, appWidgetManager, appWidgetId);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        Log.d(TAG, "onReceive: INTENT " + intent.getAction());
        Bundle extras = intent.getExtras();
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        ComponentName thisAppWidgetComponentName = new ComponentName(context.getPackageName(), getClass().getName());
        int[] appWidgetIds = appWidgetManager.getAppWidgetIds(thisAppWidgetComponentName);
        int appWidgetId = 0;
        if (extras != null) {
            appWidgetId = extras.getInt(AppWidgetManager.EXTRA_APPWIDGET_ID, AppWidgetManager.INVALID_APPWIDGET_ID);
        }

        switch (intent.getAction()) {
            case ACTION_REFRESH:
                if (appWidgetId == AppWidgetManager.INVALID_APPWIDGET_ID) {
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetIds, R.id.lv_messages);
                } else {
                    appWidgetManager.notifyAppWidgetViewDataChanged(appWidgetId, R.id.lv_messages);
                }
                break;
            case ACTION_VIEW_SMS:
                int clickPosition = intent.getIntExtra(EXTRA_ITEM_POSITION, 0);
                String messageId = intent.getExtras().getString(EXTRA_ITEM_ID, "0");
                String numbers = intent.getExtras().getString(EXTRA_ITEM_NUMBERS, "0");

                try {
                    Intent smsIntent = new Intent(Intent.ACTION_VIEW);
                    smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    smsIntent.setData(Uri.fromParts("sms", numbers, null));
                    context.startActivity(smsIntent);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
                break;
            case ACTION_CREATE_SMS:
                try {
                    Intent smsIntent = new Intent(Intent.ACTION_SENDTO);
                    smsIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    smsIntent.setData(Uri.parse("smsto:"));
                    context.startActivity(smsIntent);
                } catch (RuntimeException e) {
                    e.printStackTrace();
                }
                break;
            default:
                Log.d(TAG, "onReceive: DEFAULT");
                break;
        }
    }

    @Override
    public void onDeleted(Context context, int[] appWidgetIds) {
        super.onDeleted(context, appWidgetIds);
        for (int id : appWidgetIds) {
            SMSWidgetConfigureActivity.deletePref(context, id, SMSWidgetConfigureActivity.KEY_SMS_MESSAGE_COUNT);
        }
    }

    @Override
    public void onDisabled(Context context) {
        super.onDisabled(context);
        Log.d(TAG, "onDisabled: Deleting alarm");

        SMSAlarm smsAlarm = new SMSAlarm(context.getApplicationContext());
        smsAlarm.stopAlarm();
        SMSWidgetConfigureActivity.deletePref(context, 0, SMSWidgetConfigureActivity.KEY_SMS_UPDATE_INTERVAL);
    }

    @Override
    public void onEnabled(Context context) {
        super.onEnabled(context);
        //TODO: Make update interval configurable
        int smsUpdateInterval = SMSWidgetConfigureActivity.loadPref(context, 0, SMSWidgetConfigureActivity.KEY_SMS_UPDATE_INTERVAL, 10);
        Log.d(TAG, "onEnabled: Adding alarm to update widget every " + smsUpdateInterval + " minutes");

        SMSAlarm smsAlarm = new SMSAlarm(context.getApplicationContext());
        smsAlarm.startAlarm(smsUpdateInterval);
    }

    public static String getFormattedDate (long date) {
        String dateFormat = "h:mm a";
        if(isToday(date)){
            return new SimpleDateFormat(dateFormat, Locale.getDefault()).format(date);
        }
        if (DateUtils.isToday(date + DateUtils.DAY_IN_MILLIS)) {
            return "Yesterday " + new SimpleDateFormat(dateFormat, Locale.getDefault()).format(date);
        }
        dateFormat = "MMM d h:mm a";
        return new SimpleDateFormat(dateFormat, Locale.getDefault()).format(date);
    }
}