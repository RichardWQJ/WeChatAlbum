package com.tencent.iot.aidl;

import com.tencent.iot.aidl.IAlbumIotServiceCallback;

interface IAlbumIotService {
    oneway void register(IAlbumIotServiceCallback cb,IBinder binder,String manufacturer, String deviceId,int deviceType, in int[] feature);
    oneway void unregister(String manufacturer,String deviceId);
    String getUnicode();
}
