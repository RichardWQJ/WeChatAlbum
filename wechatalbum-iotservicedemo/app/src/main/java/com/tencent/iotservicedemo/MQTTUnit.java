package com.tencent.iotservicedemo;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;
import android.util.Log;
import org.eclipse.paho.client.mqttv3.DisconnectedBufferOptions;
import org.eclipse.paho.client.mqttv3.IMqttActionListener;
import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken;
import org.eclipse.paho.client.mqttv3.IMqttToken;
import org.eclipse.paho.client.mqttv3.MqttAsyncClient;
import org.eclipse.paho.client.mqttv3.MqttCallbackExtended;
import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.eclipse.paho.client.mqttv3.MqttException;
import org.eclipse.paho.client.mqttv3.MqttMessage;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.X509Certificate;
import java.util.HashMap;
import java.util.UUID;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

/**
 * Created by zbl on 2017/3/2.
 * MQTT通信接口
 */

public class MQTTUnit {
    private static final String TAG = "MQTTUnit";

    private Context context;
    private MqttAsyncClient client;
    private MqttConnectOptions options;
//    private List<IMQTTMessageParser> messageParserList = Collections.synchronizedList(new ArrayList<IMQTTMessageParser>());
    private HashMap<String, Topic> topicHashMap = new HashMap<>();
//    private Handler handler;
    private static long ReconnectTimeGap = 60000;
    private long saveConnectionLostTime = 0;

    private String mScheme = "tcp://";
    private String mServerUrl = "localhost:1883";
    private int tag = 2;

    private String mUserName, mPassword;

    private String t_topic;
    private String t_data;

    private String m_topic_confirmpwd = "wl/wechat/confirmpwd";
    private String mDevId, mDevPwd;

    public MQTTUnit(Context context) {
        this.context = context;
        this.tag = tag;
//        handler = new Handler(Looper.getMainLooper());
        options = new MqttConnectOptions();
//        options.setKeepAliveInterval(MQTTApiConfig.KEEP_ALIVE_INTERVAL_TIME);
//        options.setConnectionTimeout(MQTTApiConfig.CONNECT_TIMEOUT);
        options.setMaxInflight(100);
        options.setAutomaticReconnect(true);
        options.setCleanSession(false);
        options.setMqttVersion(MqttConnectOptions.MQTT_VERSION_3_1_1);
    }

    public MQTTUnit setScheme(@NonNull String scheme) {
        this.mScheme = scheme;
        return this;
    }

    /**
     * 设置遗嘱，在connect之前调用
     */
    public MQTTUnit setWill(@NonNull String topic, @NonNull String will, int qos, boolean retained) {
        options.setWill(topic, will.getBytes(), qos, retained);
        return this;
    }

    /**
     * 增加主题，在subcribeAll后批量订阅
     */
    public MQTTUnit addTopic(String topic, int qos) {
        topicHashMap.put(topic, new Topic(topic, qos));
        return this;
    }

    /**
     * 单纯从数据结构中删除主题
     */
    public MQTTUnit removeTopic(String topic) {
        topicHashMap.remove(topic);
        return this;
    }

    /**
     * 连接MQTT服务器，在用户登录成功后调用
     *
     * @param userName  例如："wltest"
     * @param password  例如："wl168cloud"
     * @param serverUrl 例如："testv2.wulian.cc:52185"
     */
    public void connect(@NonNull String userName, @NonNull String password, @NonNull String serverUrl) {
        if (TextUtils.equals(userName, mUserName)
                && TextUtils.equals(password, mPassword)
                && TextUtils.equals(serverUrl, mServerUrl)
                && isConnected()) {
            return;//防止重复连接
        }
        if (TextUtils.isEmpty(serverUrl)) {
            return;
        }
        if (client != null) {
            disconnect(client);
        }
        try {
            mUserName = userName;
            mPassword = password;
            mServerUrl = serverUrl;
            String appID;
            try {
                appID = android.os.Build.class.getField("SERIAL").get(null).toString();
            } catch (Exception e) {
                //发生错误的情况下，使用随机生成的UUID
                appID = UUID.randomUUID().toString();
                e.printStackTrace();
            }
            String clientID = appID + System.currentTimeMillis();
            client = new MqttAsyncClient(mScheme + mServerUrl, clientID, null, new AlarmPingSender(context));
            client.setCallback(new MqttCallbackExtended() {
                @Override
                public void connectComplete(boolean reconnect, String serverURI) {
                    Log.i(TAG, "connectComplete " + tag + " reconnect:" + reconnect);
                    if (reconnect) {
                        Log.i(TAG, "Reconnected to : " + serverURI);
                        // Because Clean Session is true, we need to re-subscribe
                        subscribeAll();
                    } else {
                        Log.i(TAG, "Connected to: " + serverURI);
                    }
                }

                @Override
                public void connectionLost(Throwable throwable) {
                    Log.e(TAG, "connectionLost " + tag);
//                    topicHashMap.clear();
//                    clearMessageParser();
//                    EventBus.getDefault().post(new MQTTEvent(MQTTEvent.STATE_CONNECT_LOST, tag));
//                    long reconnectTimeDelay = ReconnectTimeGap - (System.currentTimeMillis() - saveConnectionLostTime);
//                    if (reconnectTimeDelay <= 0) {
//                        reconnectTimeDelay = 200;
//                    }
//                    handler.removeCallbacks(reconnectTask);
//                    handler.postDelayed(reconnectTask, reconnectTimeDelay);
//                    saveConnectionLostTime = System.currentTimeMillis();
                    reconnect();
                }

                @Override
                public void messageArrived(final String topic, MqttMessage mqttMessage) throws Exception {
                    //处理收到的推送消息
                    if (mqttMessage.getPayload() != null && mqttMessage.getPayload().length > 0) {
                        final String payload = new String(mqttMessage.getPayload(), "utf-8");
                        Log.i(TAG, "messageArrived:tag" + tag + ": " + topic + " : " + payload);
                        mDevId = payload.substring(0, 16);
                        mDevPwd = payload.substring(16, payload.length());
                        Log.i(TAG, "lock infomation:tag" + tag + ": " + mDevId + " : " + mDevPwd);
                        if(TestService.getService() != null){
                            TestService.getService().configurePwd(mDevId, mDevPwd);
                        }
//                        for (IMQTTMessageParser parser : messageParserList) {
//                            parser.parseMessage(tag, topic, payload);
//                        }
                    }

                }

                @Override
                public void deliveryComplete(IMqttDeliveryToken iMqttDeliveryToken) {
                    //处理消息交付完成
                    Log.i(TAG, "deliveryComplete: tag" + tag + " " + iMqttDeliveryToken.getMessageId());
                }
            });
//            options.setUserName(mUserName);
//            options.setPassword(mPassword.toCharArray());
//            if (mScheme.startsWith("ssl")) {
//                setupSSL(options);
//            }
            client.connect(options, null, new IMqttActionListener() {
                @Override
                public void onSuccess(IMqttToken iMqttToken) {
                    Log.i(TAG, "connect Success " + tag);
                    DisconnectedBufferOptions disconnectedBufferOptions = new DisconnectedBufferOptions();
                    disconnectedBufferOptions.setBufferEnabled(true);
                    disconnectedBufferOptions.setBufferSize(100);
                    disconnectedBufferOptions.setPersistBuffer(false);
                    disconnectedBufferOptions.setDeleteOldestMessages(false);
                    client.setBufferOpts(disconnectedBufferOptions);
                    //Richard
                    addTopic(m_topic_confirmpwd, 2);
                    subscribeAll();
//                    EventBus.getDefault().post(new MQTTEvent(MQTTEvent.STATE_CONNECT_SUCCESS, tag));
                }

                @Override
                public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                    Log.e(TAG, "connect Failure : " + tag + throwable.toString());
//                    EventBus.getDefault().post(new MQTTEvent(MQTTEvent.STATE_CONNECT_FAIL, tag));
//                    handler.removeCallbacks(reconnectTask);
//                    handler.postDelayed(reconnectTask, 5000);
                    reconnect();
                }
            });

        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void reconnect() {
        Log.i(TAG, "reconnect");
        if (client != null) {
            try {
                client.reconnect();
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void disconnect() {
        disconnect(client);
        client = null;
    }

    public void disconnect(MqttAsyncClient client) {
        Log.i(TAG, "disconnect " + tag);
//        handler.removeCallbacksAndMessages(null);
        if (client != null) {
            client.setCallback(null);
            if (client.isConnected()) {
                try {
                    client.disconnect(null, new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken asyncActionToken) {
//                            EventBus.getDefault().post(new MQTTEvent(MQTTEvent.STATE_CONNECT_DISCONNECT, tag));
                        }

                        @Override
                        public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

                        }
                    });
                } catch (MqttException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private IMqttActionListener subscribeListener = new IMqttActionListener() {
        @Override
        public void onSuccess(IMqttToken iMqttToken) {
            if (iMqttToken != null && iMqttToken.getTopics() != null) {
                for (String topic : iMqttToken.getTopics()) {
                    Log.i(TAG, "subscribe success : " + topic);
                }
            }
        }

        @Override
        public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
            if (iMqttToken != null && iMqttToken.getTopics() != null) {
                for (String topic : iMqttToken.getTopics()) {
                    Log.e(TAG, "subscribe fail : " + topic);
                }
            }
        }
    };

    public void subscribeAll() {
        if (client != null) {
            try {
                for (Topic topic : topicHashMap.values()) {
                    client.subscribe(topic.topic, topic.qos, null, subscribeListener);
                }
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void subscribe(String topic, int qos, IMqttActionListener listener) {
        try {
            addTopic(topic, qos);
            client.subscribe(topic, qos, null, listener);
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }


    public void unsubscribe(String topic) {
        unsubscribe(topic, new IMqttActionListener() {
            @Override
            public void onSuccess(IMqttToken asyncActionToken) {
                if (asyncActionToken != null && asyncActionToken.getTopics() != null) {
                    for (String topic : asyncActionToken.getTopics()) {
                        Log.i(TAG, "unsubscribe success : " + topic);
                    }
                }
            }

            @Override
            public void onFailure(IMqttToken asyncActionToken, Throwable exception) {

            }
        });
    }

    public void unsubscribe(String topic, IMqttActionListener listener) {
        try {
            removeTopic(topic);
            if (client != null) {
                client.unsubscribe(topic, null, listener);
            }
        } catch (MqttException e) {
            e.printStackTrace();
        }
    }

    public void publish(String topic, String data) {
        publish(topic, data, 2, null);
    }

    public void publish(String topic, String data, IMqttActionListener listener) {
        publish(topic, data, 2, listener);
    }

    public void publish(final String topic, String data, int qos, IMqttActionListener listener) {
        t_topic = topic;
        t_data = data;
        if (client != null) {
            if (!client.isConnected()) {
                reconnect();
            }
            try {
                MqttMessage message = new MqttMessage();
                message.setQos(qos);
                message.setRetained(false);
                message.setPayload(data.getBytes());
                if (listener == null) {
                    listener = new IMqttActionListener() {
                        @Override
                        public void onSuccess(IMqttToken iMqttToken) {
                            Log.i(TAG, "publish success : tag" + tag + " " + iMqttToken.getMessageId());
                            Log.i(TAG, "topic : " + t_topic + " data : " + t_data);
                        }

                        @Override
                        public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                            Log.i(TAG, "publish fail : tag" + tag + " " + iMqttToken.getMessageId());
                        }
                    };
                }
                client.publish(topic, message, null, listener);
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

    public void publish(String topic, byte[] data) {
        publish(topic, data, 2);
    }

    public void publish(String topic, byte[] data, int qos) {
        if (client != null) {
            try {
                MqttMessage message = new MqttMessage();
                message.setQos(qos);
                message.setRetained(false);
                message.setPayload(data);
                client.publish(topic, message, null, new IMqttActionListener() {
                    @Override
                    public void onSuccess(IMqttToken iMqttToken) {
                        Log.i(TAG, "publish success : tag" + tag + " " + iMqttToken.getMessageId());
                    }

                    @Override
                    public void onFailure(IMqttToken iMqttToken, Throwable throwable) {
                        Log.i(TAG, "publish fail : tag" + tag + " " + iMqttToken.getMessageId());
                    }
                });
            } catch (MqttException e) {
                e.printStackTrace();
            }
        }
    }

//    public MQTTUnit addMessageParser(IMQTTMessageParser parser) {
//        messageParserList.add(parser);//推送处理
//        return this;
//    }
//
//    public boolean removeMessageParser(IMQTTMessageParser parser) {
//        return messageParserList.remove(parser);//取消监听
//    }

//    public void clearMessageParser() {
//        messageParserList.clear();
//    }

    public boolean isConnected() {
        if (client != null) {
            return client.isConnected();
        } else {
            return false;
        }
    }

    private void setupSSL(MqttConnectOptions options) {

        // 创建X509信任管理器
        X509TrustManager x509TrustManager = new X509TrustManager() {
            public void checkClientTrusted(X509Certificate[] xcs, String string) {
            }

            public void checkServerTrusted(X509Certificate[] xcs, String string) {
            }

            public X509Certificate[] getAcceptedIssuers() {
                return null;
            }
        };

        SSLContext ctx = null;
        try {
            ctx = SSLContext.getInstance("TLS");
            ctx.init(null, new TrustManager[]{x509TrustManager}, null);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyManagementException e) {
            e.printStackTrace();
        }
        SSLSocketFactory sslFactory = ctx.getSocketFactory();
        options.setSocketFactory(sslFactory);
    }
}
