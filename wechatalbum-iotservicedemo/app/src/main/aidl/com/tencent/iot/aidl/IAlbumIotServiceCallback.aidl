package com.tencent.iot.aidl;

interface IAlbumIotServiceCallback {
    int operate(String deviceId, String nuicode, int feature, int cmd);
}
