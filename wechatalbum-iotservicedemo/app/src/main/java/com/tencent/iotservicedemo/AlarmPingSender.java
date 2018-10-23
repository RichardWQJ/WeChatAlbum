package com.tencent.iotservicedemo;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.PowerManager;
import android.util.Log;

import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttPingSender;
import org.eclipse.paho.client.mqttv3.internal.ClientComms;

import java.util.Calendar;


/**
 * Created by zbl on 2017/3/9.
 * <p>
 * 使用AlarmManager作定时器发送心跳
 */

public class AlarmPingSender implements MqttPingSender {
    private static final String TAG = "AlarmPingSender";
    private static String PING_ACTION;

    private Context context;
    private ClientComms comms;
    private Calendar calendar;
    private Intent intent;
    private PendingIntent senderIntent;
    private IntentFilter intentFilter;
    private AlarmReceiver receiver;
    private volatile boolean hasStarted = false;

    public AlarmPingSender(Context context) {
        this.context = context;
        PING_ACTION = context.getPackageName() + ".support.core.mqtt.PING_ACTION";
        intent = new Intent(PING_ACTION);
        senderIntent = PendingIntent.getBroadcast(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        intentFilter = new IntentFilter(PING_ACTION);
        calendar = Calendar.getInstance();
    }

    @Override
    public void init(ClientComms comms) {
        if (comms == null) {
            throw new IllegalArgumentException("ClientComms cannot be null.");
        }
        this.comms = comms;
    }

    @Override
    public void start() {
        //开始时间
        Log.w(TAG, "Ping Sender start");
        if (receiver != null) {
            context.unregisterReceiver(receiver);
        }
        receiver = new AlarmReceiver();
//        context.registerReceiver(receiver, intentFilter);
//        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        calendar.setTimeInMillis(System.currentTimeMillis());
//        calendar.add(Calendar.MILLISECOND, (int) comms.getKeepAlive());
//        am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), senderIntent);


        context.registerReceiver(receiver, intentFilter);

        schedule(comms.getKeepAlive());
        hasStarted = true;
    }

    @Override
    public void stop() {
        Log.w(TAG, "Ping Sender stop");
//        if (receiver != null) {
//            context.unregisterReceiver(receiver);
//            receiver = null;
//        }
//        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        am.cancel(senderIntent);

        if(hasStarted){
            if(senderIntent != null){
                // Cancel Alarm.
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Service.ALARM_SERVICE);
                alarmManager.cancel(senderIntent);
            }

            hasStarted = false;
            try{
                if (receiver != null) {
                    context.unregisterReceiver(receiver);
                    receiver = null;
                }
            }catch(IllegalArgumentException e){
                //Ignore unregister errors.
            }
        }
    }

    @Override
    public void schedule(long delayInMilliseconds) {
        Log.w(TAG, "Ping Sender schedule");
//        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
//        calendar.setTimeInMillis(System.currentTimeMillis());
//        calendar.add(Calendar.MILLISECOND, (int) comms.getKeepAlive());
//        am.set(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), senderIntent);


        long nextAlarmInMilliseconds = System.currentTimeMillis()
                + delayInMilliseconds;
        Log.d(TAG, "Schedule next alarm at " + nextAlarmInMilliseconds);
        AlarmManager alarmManager = (AlarmManager) context
                .getSystemService(Service.ALARM_SERVICE);

//        if (Build.VERSION.SDK_INT >= 23) {
//            // In SDK 23 and above, dosing will prevent setExact, setExactAndAllowWhileIdle will force
//            // the device to run this task whilst dosing.
//            Log.d(TAG, "Alarm scheule using setExactAndAllowWhileIdle, next: " + delayInMilliseconds);
//            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
//                    senderIntent);
//        } else if (Build.VERSION.SDK_INT >= 19) {
//            Log.d(TAG, "Alarm scheule using setExact, delay: " + delayInMilliseconds);
//            alarmManager.setExact(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
//                    senderIntent);
//        } else {
            alarmManager.set(AlarmManager.RTC_WAKEUP, nextAlarmInMilliseconds,
                    senderIntent);
//        }
    }

    /*
     * This class sends PingReq packet to MQTT broker
     */
    class AlarmReceiver extends BroadcastReceiver {
        private PowerManager.WakeLock wakelock;
        private final String wakeLockTag = "PING_WAKELOCK"
                + comms.getClient().getClientId();

        @Override
        @SuppressLint("Wakelock")
        public void onReceive(Context context, Intent intent) {
            // According to the docs, "Alarm Manager holds a CPU wake lock as
            // long as the alarm receiver's onReceive() method is executing.
            // This guarantees that the phone will not sleep until you have
            // finished handling the broadcast.", but this class still get
            // a wake lock to wait for ping finished.

            Log.d(TAG, "Sending Ping at:" + System.currentTimeMillis());

            PowerManager pm = (PowerManager) context
                    .getSystemService(Service.POWER_SERVICE);
            wakelock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, wakeLockTag);
            wakelock.acquire();

            // Assign new callback to token to execute code after PingResq
            // arrives. Get another wakelock even receiver already has one,
            // release it until ping response returns.
            IMqttToken token = comms.checkForActivity(new IMqttActionListener() {

                @Override
                public void onSuccess(IMqttToken asyncActionToken) {
                    Log.d(TAG, "Success. Release lock(" + wakeLockTag + "):"
                            + System.currentTimeMillis());
                    //Release wakelock when it is done.
                    wakelock.release();
                }

                @Override
                public void onFailure(IMqttToken asyncActionToken,
                                      Throwable exception) {
                    Log.d(TAG, "Failure. Release lock(" + wakeLockTag + "):"
                            + System.currentTimeMillis());
                    //Release wakelock when it is done.
                    wakelock.release();
                }
            });


            if (token == null && wakelock.isHeld()) {
                wakelock.release();
            }
        }
    }
}
