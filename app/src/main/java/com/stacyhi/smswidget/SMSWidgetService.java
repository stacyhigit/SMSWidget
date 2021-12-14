package com.stacyhi.smswidget;

import android.content.Intent;
import android.widget.RemoteViewsService;

public class SMSWidgetService extends RemoteViewsService {
    @Override
    public RemoteViewsFactory onGetViewFactory(Intent intent) {
        return new SMSDataProvider (this.getApplicationContext(), intent);
    }
}
