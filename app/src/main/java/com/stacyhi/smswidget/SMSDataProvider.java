package com.stacyhi.smswidget;

import android.appwidget.AppWidgetManager;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.provider.ContactsContract;
import android.provider.Telephony;
import android.telephony.PhoneNumberUtils;
import android.util.Log;
import android.view.View;
import android.widget.RemoteViews;
import android.widget.RemoteViewsService;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class SMSDataProvider implements RemoteViewsService.RemoteViewsFactory{
    private final String TAG = "SMSDataProvider";
    private final Context mContext;
    private final int appWidgetId;
    private static List<Message> messageList = new ArrayList<>();
    private static String locale;

    public SMSDataProvider(Context context, Intent intent) {
        mContext = context;
        this.appWidgetId = intent.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID,AppWidgetManager.INVALID_APPWIDGET_ID);
        locale = context.getResources().getConfiguration().getLocales().get(0).getCountry();
    }

    @Override
    public void onCreate() {
    }

    @Override
    public void onDataSetChanged() {
        Log.d(TAG, "onDataSetChanged: START");
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);

        RemoteViews loadingView = new RemoteViews(mContext.getPackageName(), R.layout.sms_widget_provider);
        loadingView.setViewVisibility(R.id.pb_refresh, View.VISIBLE);
        loadingView.setViewVisibility(R.id.iv_refresh, View.INVISIBLE);
        loadingView.setTextViewText(R.id.tv_empty_listview, "Loading");
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, loadingView);

        RemoteViews loadingDoneView = new RemoteViews(mContext.getPackageName(), R.layout.sms_widget_provider);
        if (PermissionManager.checkAllPermissions(mContext)) {
            //initTestData();
            messageList = getSmsMms();
            loadingDoneView.setScrollPosition(R.id.lv_messages,0);
        } else {
            loadingDoneView.setTextViewText(R.id.tv_empty_listview, mContext.getResources().getString(R.string.add_permissions));
            messageList.clear();
        }

        String dateString = SMSWidgetProvider.getFormattedDate(new Date().getTime());
        loadingDoneView.setTextViewText(R.id.tv_updated, "Updated " + dateString);
        loadingDoneView.setViewVisibility(R.id.pb_refresh, View.INVISIBLE);
        loadingDoneView.setViewVisibility(R.id.iv_refresh, View.VISIBLE);
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, loadingDoneView);
        Log.d(TAG, "onDataSetChanged: END");

    }

    @Override
    public void onDestroy() {
    }

    @Override
    public int getCount() {
        return messageList.size();
    }

    @Override
    public RemoteViews getViewAt(int position) {
        RemoteViews views = new RemoteViews(mContext.getPackageName(), R.layout.sms_row);
        try {
            Message message = messageList.get(position);
            views.setTextViewText(R.id.tv_message_date, SMSWidgetProvider.getFormattedDate(message.getDate()));
            views.setTextViewText(R.id.tv_message_header, message.getHeader());
            views.setTextViewText(R.id.tv_message_content, message.getContent());
            Intent fillIntent = new Intent();
            fillIntent.setData(Uri.parse(fillIntent.toUri(Intent.URI_INTENT_SCHEME)));
            fillIntent.putExtra(SMSWidgetProvider.EXTRA_ITEM_POSITION, position);
            fillIntent.putExtra(SMSWidgetProvider.EXTRA_ITEM_NUMBERS, message.getRecipientNumbers());
            fillIntent.putExtra(SMSWidgetProvider.EXTRA_ITEM_ID, message.getId());
            views.setOnClickFillInIntent(R.id.layout_message_row, fillIntent);
            return views;
        } catch (Exception e) {
            messageList.clear();
            Log.d(TAG, "getViewAt: ERROR");
            e.printStackTrace();
            setErrorView("loading messages");
            return views;
        }
    }

    @Override
    public RemoteViews getLoadingView() {
        return null;
    }

    @Override
    public int getViewTypeCount() {
        return 1;
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean hasStableIds() {
        return true;
    }

    private void initTestData() {
        messageList.clear();
        Message message1 = new Message("1", new Date().getTime(),  "you don't have to put on pants today");
        Recipient recipient1 = new Recipient("Steve", "", "");
        message1.addRecipient(recipient1);
        messageList.add(message1);

        Message message2 = new Message("3", (new Date().getTime() - 350000),"I have cake!");
        Recipient recipient2 = new Recipient("Jen & Rob", "Jen's Cell", "");
        message2.addRecipient(recipient2);
        messageList.add(message2);

        Message message3 = new Message("4", (new Date().getTime() - 760000),"We miss you, Stacy");
        Recipient recipient3 = new RecipientNoContact("", "", "888-555-1212");
        message3.addRecipient(recipient3);
        messageList.add(message3);
    }

    private List<Message> getSmsMms () {
        final Uri uri = Uri.parse("content://mms-sms/conversations?simple=true");
        final String[] projection = new String[]{"_id", "recipient_ids", "snippet", "date", "type", "message_count"};
        final String selection = "message_count>0 and error<=1";

        int smsMessageCountPosition = SMSWidgetConfigureActivity.loadPref(mContext, appWidgetId, SMSWidgetConfigureActivity.KEY_SMS_MESSAGE_COUNT, 2);
        String smsMessageCount = mContext.getResources().getStringArray(R.array.message_numbers)[smsMessageCountPosition];
        final String sortOrder = "date DESC LIMIT " + smsMessageCount;

        final List<Message> messages = new ArrayList<>();

        try {
            Cursor smsCursor = mContext.getContentResolver().query(uri, projection, selection, null, sortOrder);

            if (smsCursor.moveToFirst()) {
                do {
                    Message message = new Message(smsCursor.getString(smsCursor.getColumnIndexOrThrow(projection[0])),
                            smsCursor.getLong(smsCursor.getColumnIndexOrThrow(projection[3])),
                            smsCursor.getString(smsCursor.getColumnIndexOrThrow(projection[2])));

                    String[] recipientIds = smsCursor.getString(smsCursor.getColumnIndexOrThrow(projection[1])).split(" ");

                    for (String recipientId : recipientIds) {
                        String recipientNumber = getRecipientNumber(Integer.parseInt(recipientId));

                        Recipient recipient = getRecipientName(recipientNumber);
                        message.addRecipient(recipient);
                    }
                    messages.add(message);
                } while (smsCursor.moveToNext());
            }
            if (smsCursor != null) {
                smsCursor.close();
            }
        } catch (Exception e) {
            Log.d(TAG, "getSmsMms: ERROR");
            e.printStackTrace();
            messageList.clear();
            setErrorView("retrieving messages");
        }
        return messages;
    }

    private String getRecipientNumber(int recipientId){
        Uri uri = Uri.parse("content://mms-sms/canonical-address/" + recipientId);
        String[] projection = {Telephony.CanonicalAddressesColumns.ADDRESS};
        String selectionAdd = Telephony.CanonicalAddressesColumns._ID + "=" + recipientId;
        String number = "0";
        try {
            Cursor recipientCursor = mContext.getContentResolver().query(uri, null, selectionAdd, null, null);

            if (recipientCursor.moveToFirst()) {
                number = recipientCursor.getString(recipientCursor.getColumnIndexOrThrow(projection[0]));
            }
            if (recipientCursor != null) {
                recipientCursor.close();
            }
        } catch (Exception e) {
            Log.d(TAG, "getRecipientNumber: ERROR");
            e.printStackTrace();
        }
        return number;
    }

    private Recipient getRecipientName(String number) {
        Recipient recipient;
        Uri uri = Uri.withAppendedPath(ContactsContract.PhoneLookup.CONTENT_FILTER_URI, Uri.encode(number));
        String[] projection = new String[]{"display_name","label"};
        Cursor contactsCursor = mContext.getContentResolver().query(uri, projection, null, null, null);

        try {
            if (contactsCursor.moveToFirst()) {
                recipient = new Recipient(contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(projection[0])),
                        contactsCursor.getString(contactsCursor.getColumnIndexOrThrow(projection[1])), number);
            } else {
                recipient = new RecipientNoContact("", "", number);
            }
            if (contactsCursor != null) {
                contactsCursor.close();
            }
        } catch (Exception e) {
            Log.d(TAG, "getRecipientName: ERROR");
            e.printStackTrace();
            return new RecipientNoContact("", "", number);
        }
        return recipient;
    }

    public void setErrorView(String errorMessage) {
        Log.d(TAG, "ERROR " + errorMessage);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(mContext);
        RemoteViews errorView = new RemoteViews(mContext.getPackageName(), R.layout.sms_widget_provider);
        errorView.setTextViewText(R.id.tv_empty_listview, "ERROR " + errorMessage);
        errorView.setViewVisibility(R.id.tv_empty_listview, View.VISIBLE);
        String dateString = SMSWidgetProvider.getFormattedDate(new Date().getTime());
        errorView.setTextViewText(R.id.tv_updated, "Updated " + dateString);
        errorView.setViewVisibility(R.id.pb_refresh, View.INVISIBLE);
        errorView.setViewVisibility(R.id.iv_refresh, View.VISIBLE);
        appWidgetManager.partiallyUpdateAppWidget(appWidgetId, errorView);
    }

    public static String formatNumber(String number) {
        return PhoneNumberUtils.formatNumber(number, locale);
    }
}
