package com.example.dell.simulatelottery.base;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * Created by dell on 2017/3/15.
 */

public class CrashHandler implements Thread.UncaughtExceptionHandler {

    private static String TAG = "CrashHandler";
    //系统默认的UncaughtException处理类
    private Thread.UncaughtExceptionHandler mDefaultHandler;
    //CrashHandler实例
    private static CrashHandler instance = new CrashHandler();
    //程序的Context对象
    private Context context;
    //用来保存设备信息和异常信息
    private Map<String, String> infos = new HashMap<>();
    //用于格式化日期,作为日志文件名的一部分
    private DateFormat format = new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss");
    //请求异常地址
    private String ExceptionURL = "/exception/save_mobile_exception_log";
    /**
     * 保证只有一个CrashHandler实例
     **/
    public CrashHandler() {
    }

    /**
     * 获取CrashHandler实例,单例模式
     **/
    public static CrashHandler getInstance() {
        return instance;
    }

    /**
     * 初始化
     */
    public void init(Context context) {
        this.context = context;
        //获取系统默认的UncaughtException处理器
        mDefaultHandler = Thread.getDefaultUncaughtExceptionHandler();
        //设置该CrashHandler为程序的默认处理器
        Thread.setDefaultUncaughtExceptionHandler(this);
    }

    /**
     * 当UncaughtException发生时传入该函数进行处理
     */
    @Override
    public void uncaughtException(Thread thread, Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            //如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        }
        //退出程序
        android.os.Process.killProcess(android.os.Process.myPid());
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成
     *
     * @param ex
     * @return true:如果处理该异常信息;否则返回fasle;
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        //收集设备参数信息
        collectDeviceInfo(context);
        //保存日志文件
        saveCrashInfo2File(ex);
        uploadCrash();
        return false;
    }


    /**
     * 上传崩溃信息
     */
    private OkHttpClient mClient = new OkHttpClient();

    //上传崩溃信息
    private void uploadCrash() {
        JSONObject root       = new JSONObject();
        JSONArray  exceptions = new JSONArray();
        JSONObject exception  = new JSONObject();
        try {
            for (Map.Entry<String, String> entry : infos.entrySet()) {
                exception.put(entry.getKey(), entry.getValue());
                exceptions.put(exception);
            }
            root.put("exception_list", exceptions);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Request request = new Request.Builder()
                .url(Constants.API+
                        ExceptionURL +
                        root.toString()
                )
                .build();
        mClient.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {

            }
            @Override
            public void onResponse(Call call, Response response) throws IOException {

            }
        });
    }


    /**
     * 收集设备参数信息
     */
    public void collectDeviceInfo(Context context) {
        try {
            PackageManager pm = context.getPackageManager();
            PackageInfo pi = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_ACTIVITIES);
            if (pi != null) {
                String versionName = pi.versionName == null ? "null" : pi.versionName;
                infos.put("version", versionName);
            }
            infos.put("device-info", Build.PRODUCT + "-" + Build.MODEL);
            infos.put("mac",         SendMessage.getInstance().getMac());
            infos.put("remark", "");
            infos.put("os", "Android");
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "an error occurred when collect package info", e);
        }
    }

    /**
     * 保存错误信息到文件中
     *@param ex
     * @return 返回文件名称,便于将文件传送到服务器
     * */
    private String saveCrashInfo2File(Throwable ex){
        StringBuilder sb = new StringBuilder();
        for(Map.Entry<String,String> entry : infos.entrySet()){
            String key   = entry.getKey();
            String value = entry.getValue();
            sb.append(key).append("=").append(value).append("\n");
        }

        Writer  writer  =  new StringWriter();
        PrintWriter printWriter  =  new PrintWriter(writer);
        ex.printStackTrace(printWriter);
        Throwable cause = ex.getCause();
        while(cause != null){
            cause.printStackTrace(printWriter);
            cause = cause.getCause();
        }
        printWriter.close();
        String result = writer.toString();
        infos.put("exception",result);
        sb.append(result);
        try{
            long    timestamp = System.currentTimeMillis();
            String  time      = format.format(new Date());
            String  fileName  = "crash-"+ time + "-" + timestamp + ".log";
            if(Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)){
                String path = "/sdcard/carsh/";
                File   dir  = new File(path);
                if(!dir.exists()){
                    dir.mkdirs();
                }
                FileOutputStream fos = new FileOutputStream(path + fileName );
                fos.write(sb.toString().getBytes());
                fos.close();
            }
            return fileName;
        } catch (Exception e) {
            Log.e(TAG, "an error occured while writing file...", e);
            e.printStackTrace();
        }
        return null;
    }

}
