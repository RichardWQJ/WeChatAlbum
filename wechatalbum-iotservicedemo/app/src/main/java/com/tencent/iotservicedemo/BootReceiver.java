package com.tencent.iotservicedemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class BootReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent){
        Intent mIntent = new Intent(context,TestService.class);
        context.startService(mIntent);
    }
}
