package com.example.dell.simulatelottery.base;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.Random;

public class Util {

    //根据busybox获取本地Mac
    public static String getMacAddress(Context context) {
        StringBuilder    builder            =   new StringBuilder();
        Random r                            =   new Random();
        SharedPreferences sharedPreferences =   context.getSharedPreferences("mac",Context.MODE_PRIVATE);
        String macAddress                   =   sharedPreferences.getString("mac_address","");
        if ("".equals(macAddress)) {
            for (int i = 0; i < 10; i++) {
                builder.append((char) ('a' + (r.nextInt() % 26)));
            }
            SharedPreferences.Editor editor = sharedPreferences.edit();
            editor.putString("mac_address",builder.toString());
            editor.apply();
        }else {
            return macAddress;
        }
        return builder.toString();
    }

    private static String callCmd(String cmd, String filter) {
        String result = "";
        String line   = "";
        try {
            Process           proc = Runtime.getRuntime().exec(cmd);
            InputStreamReader is   = new InputStreamReader(proc.getInputStream());
            BufferedReader    br   = new BufferedReader(is);

            //执行命令cmd，只取结果中含有filter的这一行
            while ((line = br.readLine()) != null && !line.contains(filter)) {
                //result += line;
                Log.i("test", "line: " + line);
            }

            result = line;
            Log.i("test", "result: " + result);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return result;
    }

    public static String getVersion(Context context) {
        PackageManager packageManager = context.getPackageManager();
        try {
            PackageInfo info = packageManager.getPackageInfo(context.getPackageName(), 0);
            return info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    public static int getStatusBarHeight(Context context) {
        int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        return context.getResources().getDimensionPixelSize(resId);
    }
}
