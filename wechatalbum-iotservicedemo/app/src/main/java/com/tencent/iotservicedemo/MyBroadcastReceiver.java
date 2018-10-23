package com.tencent.iotservicedemo;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

/**
 * @author Anqiren
 * @package com.tencent.broadcastreceivetest
 * @create date 2018/8/28 3:20 PM
 * @describe TODO
 * @email anqirens@qq.com
 */
public class MyBroadcastReceiver extends BroadcastReceiver {
    public static final String TAG = "MyBroadcastReceiver";
    public static int m = 1;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG,"onReceive:" + intent.getAction());
        if(intent.getAction().equals("com.tencent.WECHATALBUM_ENTER_QRCODE")){
            Bundle bundle = intent.getExtras();
            Log.d(TAG,"进入二维码界面, unicode:" + bundle.getString("unicode"));
            startService(context);
        } else if(intent.getAction().equals("com.tencent.WECHATALBUM_LEAVE_QRCODE")){
            Log.d(TAG,"离开二维码界面");
            startService(context);
        } else if(intent.getAction().equals("com.tencent.WECHATALBUM_STARTUP")){
            Log.d(TAG,"相框已启动");
            //相框启动后 启动iot服务
            startService(context);
        }
    }

    private void startService(Context context){
        //服务未启动，启动服务
        if(TestService.getService() == null){
            Intent service = new Intent(context,TestService.class);
            context.startService(service);
        }
    }
}
