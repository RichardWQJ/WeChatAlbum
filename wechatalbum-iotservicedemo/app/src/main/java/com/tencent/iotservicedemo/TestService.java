package com.tencent.iotservicedemo;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.util.Log;

/**
 * @author Anqiren
 * @package com.tencent.broadcastreceivetest
 * @create date 2018/8/30 7:42 PM
 * @describe TODO
 * @email anqirens@qq.com
 */
public class TestService extends Service {
    private static final String TAG = "TestService";

    private static TestService service;

    public static TestService getService(){
        return service;
    }

    private int[] features =  new int[1];

    private MQTTUnit mMQTTUnit;

    public static String m_devId;
    public static String m_password;

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onCreate(){
        Log.v(TAG,"onCreate");
        service = this;
        features[0] = 1;
        WeChatAlbumIotService.setOnIotServiceListener(new WeChatAlbumIotService.OnServiceCallback() {
            @Override
            public void onIotServiceStateChange(int state) {
                Log.d(TAG,"onIotServiceStateChange:" + state);
                if(state == WeChatAlbumIotService.IOTSERVICE_STATE_AVAILABLE){
                    Log.v(TAG,"状态可用");
                    //operate is available only after iot service state change available
                    WeChatAlbumIotService.register(WeChatAlbumIotService.MANUFACTURE,WeChatAlbumIotService.DEVICEID,7,features);
                    Log.d(TAG,"unicode:" + WeChatAlbumIotService.getUnicode());

                } else if(state == WeChatAlbumIotService.IOTSERVICE_STATE_NOTAVAILABLE) {
                    Log.e(TAG,"状态不可用");
                }
            }

            @Override
            public int onOperate(String deviceId, String nuicode, int feature, int cmd) {
                return openate(deviceId,nuicode,feature,cmd);
            }
        });

        WeChatAlbumIotService.start(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "start sevice");
        startForeground(1,new Notification());
        if(mMQTTUnit == null){
            mMQTTUnit = new MQTTUnit(this);
            mMQTTUnit.connect("","", "localhost:1883");
        }
        return Service.START_STICKY;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        WeChatAlbumIotService.release();
    }

    private int openate(String deviceId, String nuicode, int feature, int cmd){ //Here deviceId in param is not a real device id.
        Log.d(TAG,"openate device...");
        //do something...
        if(mMQTTUnit != null){
            mMQTTUnit.publish("wl/wechat/unlock", m_devId+m_password); //Use the device Id which configured.
        }
        return 0;
    }

    public void configurePwd(String devId, String pwd) {
        m_devId = devId;
        m_password = pwd; //这里应该在验证成功之后再赋值
    }
}
