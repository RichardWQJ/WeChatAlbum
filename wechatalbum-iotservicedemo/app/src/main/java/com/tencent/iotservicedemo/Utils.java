package com.tencent.iotservicedemo;

import android.app.ActivityManager;
import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.List;

/**
 * Created by anqiren on 2017/6/12.
 */

public class Utils {
    private static String TAG = "DaemonServiceUtils";

    /**
     *
     * @param object
     * @return
     */
    public static boolean isNullOrNil(String object) {
        return object == null || object.length() <= 0;
    }



    /**
     * 获取版本号
     * @return 当前应用的版本名称
     */
    public static String getVersionName(Context context, String pkgname) {
        String version = "null";
        try {
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(pkgname, 0);
            version = info.versionName;

        } catch (Exception e) {
            e.printStackTrace();
        }
        return version;
    }

    /**
     * 获取版本号
     * @return 当前应用的版本号
     */
    public static int getVersionCode(Context context, String pkgname) {
        try {
            if(context == null){
                Log.e(TAG,"getVersionCode context is null");
            }
            PackageManager manager = context.getPackageManager();
            PackageInfo info = manager.getPackageInfo(pkgname, 0);
            return info.versionCode;
        } catch (Exception e) {
            e.printStackTrace();
            return -1;
        }
    }

    public static boolean isAppInstalled(Context context, String pkgname) {
        return getVersionCode(context,pkgname) > 0 ? true : false;
    }


    /**
     * getProcessName
     * @param cxt context.
     * @param pid process ID.
     * @return null may be returned if the specified process not found
     */
    public static String getProcessName(Context cxt, int pid) {
        ActivityManager am = (ActivityManager) cxt.getSystemService(Context.ACTIVITY_SERVICE);
        List<ActivityManager.RunningAppProcessInfo> runningApps = am.getRunningAppProcesses();
        if (runningApps == null) {
            return null;
        }
        for (ActivityManager.RunningAppProcessInfo procInfo : runningApps) {
            if (procInfo.pid == pid) {
                return procInfo.processName;
            }
        }
        return null;
    }

    /**
     * getProcessName
     * @return
     */
    public static String getProcessName() {
        try {
            File file = new File("/proc/" + android.os.Process.myPid() + "/" + "cmdline");
            BufferedReader mBufferedReader = new BufferedReader(new FileReader(file));
            String processName = mBufferedReader.readLine().trim();
            mBufferedReader.close();
            return processName;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * create directory @param dir.
     * @param dir
     * @return
     */
    public static boolean createDir(String dir)
    {
        File file = new File(dir);
        //判断文件夹是否存在,如果不存在则创建文件夹
        if (!file.exists()) {
            file.mkdirs();
        }
        if(!file.exists())
        {
            return false;
        }
        return true;
    }

}
