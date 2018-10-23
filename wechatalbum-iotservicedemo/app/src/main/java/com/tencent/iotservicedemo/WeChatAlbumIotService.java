package com.tencent.iotservicedemo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;
import android.util.Log;

import com.tencent.iot.aidl.IAlbumIotService;
import com.tencent.iot.aidl.IAlbumIotServiceCallback;

import java.util.Timer;
import java.util.TimerTask;

/**
 * @author Anqiren
 * @package com.tencent.broadcastreceivetest
 * @create date 2018/8/30 5:55 PM
 * @describe TODO
 * @email anqirens@qq.com
 */
public class WeChatAlbumIotService {
    private static final String TAG = "WeChatAlbumIotService";

    /**
     * prepare to download ,before start download.
     * prepare -> start -> downloading -> finish -> checking.
     */
    //注意，这里提供的信息必须与门锁提供的信息一致才能正确调用。

    public static final String MANUFACTURE = "WULIANGROUP";
    public static final String DEVICEID = "11111";

    //注意，如果在demo上使用请换成，demo的包名"com.tencent.vdoorapp"

    public static final String SERVICE_PACKAGE_NAME = "com.tencent.ma.app";
    public static final String SERVICE_ACTION = "com.tencent.iot.WeChatAlbumIOTService.ACTION";

    private static Context mContext;

    private static String mDeviceId;
    private static String mManufacturer;
    private static int mDeviceType;
    private static int[] mFeature;
    private static boolean isServiceConnected = false;
    private static boolean isUnregistered = false;

    private static OnServiceCallback mOnServiceListener = null;

    private static IBinder mBinder;

    private static Timer mReconnectTimer;
    private static int mRconnectCounter = 0;
    private static int mDisconnectCounter = 0;


    public static final int IOTSERVICE_STATE_AVAILABLE = 1;
    public static final int IOTSERVICE_STATE_NOTAVAILABLE = 2;

    public interface OnServiceCallback {

        void onIotServiceStateChange(int state);

        int onOperate(String deviceId, String nuicode, int feature, int cmd);
    }

    public static void start(final Context context) {
        String processName = Utils.getProcessName();
        android.util.Log.i(TAG, "start processName:" + processName);
        if (context.getPackageName().equals(processName)) {
            mContext = context;
            mBinder = new Binder();//一个生命周期只有一个binder，作为连接service的本地token
            startTryingBindService();
        }
    }


    public static void setOnIotServiceListener(OnServiceCallback listener) {
        mOnServiceListener = listener;
        if (isServiceConnected) {
            mOnServiceListener.onIotServiceStateChange(IOTSERVICE_STATE_AVAILABLE);
        }
    }


    public static int getIotServiceState() {
        if (!isServiceConnected) {
            startTryingBindService();
            return IOTSERVICE_STATE_NOTAVAILABLE;
        } else {
            return IOTSERVICE_STATE_AVAILABLE;
        }
    }


    public static boolean register(String manufacturer, String deviceId, int deviceType, int[] feature) {
        mManufacturer = manufacturer;
        mDeviceId = deviceId;
        mDeviceType = deviceType;
        mFeature = feature;
        boolean ret = false;
        if (Utils.isNullOrNil(manufacturer) || Utils.isNullOrNil(deviceId)) {
            return ret;
        }
        isUnregistered = false;
        if (isServiceConnected) {
            tryRegisterApp();
            ret = true;
        } else {
            startTryingBindService();
        }
        Log.d(TAG, "register, manufacturer:" + manufacturer + ",deviceId:" + deviceId);
        return ret;
    }


    public static String getUnicode() {
        Log.d(TAG, "getDevicetoken");
        String devicetoken = "";
        if (mInvoke != null) {
            try {
                devicetoken = mInvoke.getUnicode();
            } catch (RemoteException e) {
                e.printStackTrace();
                reBindService();
            }
        } else {
            Log.e(TAG, "haven't register yet!");
            tryRegisterApp();
        }

        Log.d(TAG, "getDevicetoken:" + devicetoken);
        return devicetoken;
    }

    public static boolean unregister() {
        if (isUnregistered) {
            Log.v(TAG, "already unregistered!");
            return true;
        }
        boolean success = false;
        if (mInvoke != null) {
            try {
                isUnregistered = true;
                mInvoke.unregister(mManufacturer, mDeviceId);
                mContext.unbindService(mServiceConn);
                isServiceConnected = false;
                mInvoke = null;
                success = true;
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        } else {
            stopTryingBindService();
        }
        return success;
    }


    public static void release() {
        mContext.unbindService(mServiceConn);
    }


    private static IAlbumIotService mInvoke;
    private static IAlbumIotServiceCallback mCallback = new IAlbumIotServiceCallback.Stub() {

        @Override
        public int operate(String deviceId, String unicode, int feature, int cmd) throws RemoteException {
            Log.d(TAG, "operate: deviceId:" + deviceId + ",nuicode:" + unicode + ",feature:" + feature + ",cmd:" + cmd);
            return _onOperate(deviceId,unicode,feature,cmd);
        }
    };



    private static int _onOperate(String deviceId, String unicode, int feature, int cmd) {
        if (mOnServiceListener != null) {
            return mOnServiceListener.onOperate(deviceId, unicode, feature,cmd);
        }
        return -1;
    }


    private static void reBindService() {
        if (isUnregistered) {
            Log.i(TAG, "unregistered! stop rebind service.");
            return;
        }
        mInvoke = null;
        isServiceConnected = false;

        mDisconnectCounter++;
        Log.e("client", "onServiceDisconnected : " + mDisconnectCounter);
        startTryingBindService();
    }

    private static ServiceConnection mServiceConn = new ServiceConnection() {
        @Override
        public void onServiceDisconnected(ComponentName name) {
            Log.d(TAG,"onServiceDisconnected");
            reBindService();
            if (mOnServiceListener != null) {
                Log.e(TAG, "onIotServiceStateChange:" + IOTSERVICE_STATE_NOTAVAILABLE);
                mOnServiceListener.onIotServiceStateChange(IOTSERVICE_STATE_NOTAVAILABLE);
            }
        }

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            Log.d(TAG,"onServiceConnected");
            stopTryingBindService();

            isServiceConnected = true;
            mRconnectCounter = 0;
            mInvoke = IAlbumIotService.Stub.asInterface(service);

            tryRegisterApp();
            if (mOnServiceListener != null) {
                Log.e(TAG, "onIotServiceStateChange:" + IOTSERVICE_STATE_AVAILABLE);
                mOnServiceListener.onIotServiceStateChange(IOTSERVICE_STATE_AVAILABLE);
            }

        }
    };

    private static void tryRegisterApp() {
        Log.d(TAG,"tryRegisterApp");
        if(Utils.isNullOrNil(mManufacturer) || Utils.isNullOrNil(mDeviceId)){
            Log.e(TAG,"not manufacture or deviceId");
            return;
        }
        if (mInvoke != null) {
            try {
                Log.v(TAG,"mManufacturer:" + mManufacturer + mDeviceId);
                mInvoke.register(mCallback, mBinder, mManufacturer, mDeviceId, mDeviceType,mFeature);
            } catch (RemoteException e) {
                e.printStackTrace();
                reBindService();
            }
        } else {
            reBindService();
            Log.e(TAG, "tryRegisterApp mInvoke is null !");
        }
    }


    private static void bindService() {
        //android 5.0 之后必须显式绑定。
        Intent intent = new Intent();
        intent.setFlags(Intent.FLAG_INCLUDE_STOPPED_PACKAGES);
        intent.setAction(SERVICE_ACTION);
        intent.setPackage(SERVICE_PACKAGE_NAME);
        mContext.bindService(intent, mServiceConn, Context.BIND_AUTO_CREATE);
    }

    private static void startTryingBindService() {
        mRconnectCounter++;

        if (mReconnectTimer != null) {
            Log.e(TAG, "startTryingBindService err , already started!!");
        }
        stopTryingBindService();
        mReconnectTimer = new Timer();
        mReconnectTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                mRconnectCounter++;
                Log.e(TAG, "try to reconnect service! time:" + mRconnectCounter);
                bindService();
                if (mRconnectCounter > Config.tryRebindIotServiceTimes) {
                    stopTryingBindService();
                }

            }
        }, 0, Config.tryRebindIotServiceInterval);
    }

    private static void stopTryingBindService() {
        if (mReconnectTimer != null) {
            mReconnectTimer.cancel();
            mReconnectTimer = null;
        }
    }
}
