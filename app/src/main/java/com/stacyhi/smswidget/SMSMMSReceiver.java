package com.stacyhi.smswidget;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class SMSMMSReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context mContext, Intent intent) {
        Intent smsIntent = new Intent(mContext, SMSWidgetProvider.class);
        smsIntent.setAction(SMSWidgetProvider.ACTION_REFRESH);
        mContext.sendBroadcast(smsIntent);
    }
}
