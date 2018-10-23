package com.tencent.iotservicedemo;

/**
 * @author Anqiren
 * @package com.tencent.broadcastreceivetest
 * @create date 2018/8/30 5:26 PM
 * @describe TODO
 * @email anqirens@qq.com
 */
public class Config {
    private static final int DEFAULT_REBIND_INTERVAL = 1000*10;

    private static final int DEFAULT_REBIND_TIMES = 720; //2hr

    /**
     * 尝试绑定远程IotService服务的次数
     */
    protected static int tryRebindIotServiceTimes = DEFAULT_REBIND_TIMES;

    /**
     * 尝试绑定远程IotService服务的间隔
     */
    protected static int tryRebindIotServiceInterval = DEFAULT_REBIND_INTERVAL;


    /**
     * 设置绑定远程IotService服务的时间间隔 单位ms
     * @param tryRebindIotServiceInterval
     */
    public static void setTryRebindIotServiceInterval(int tryRebindIotServiceInterval){
        Config.tryRebindIotServiceInterval = tryRebindIotServiceInterval;
    }

    /**
     * 设置绑定远程IotService服务的次数
     * @param tryRebindIotServiceTimes
     */
    public static void setTryRebtindIotServiceTimes(int tryRebindIotServiceTimes){
        Config.tryRebindIotServiceTimes =  tryRebindIotServiceTimes;
    }
}
