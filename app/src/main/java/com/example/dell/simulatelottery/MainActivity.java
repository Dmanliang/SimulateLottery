package com.example.dell.simulatelottery;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.View;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import com.example.dell.simulatelottery.base.BaseActivity;
import com.example.dell.simulatelottery.base.Constants;
import com.example.dell.simulatelottery.base.DownLoadService;
import com.example.dell.simulatelottery.base.ToastUtil;
import com.example.dell.simulatelottery.base.Util;
import com.example.dell.simulatelottery.getjson.HttpTask;
import com.tbruyelle.rxpermissions.RxPermissions;
import com.umeng.analytics.MobclickAgent;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import rx.functions.Action1;

public class MainActivity extends BaseActivity {

    private WebView             mWebView;
    private String              url="http://www.baidu.com";
    private PopupWindow         loadItem;
    private View                loadview;
    private static final int    EMPTY   = 0xFFFFFFFF;
    private static final int    SUCCESS = 0xFFFFEEEE;
    private String              downloadUrl,content,update_mode;
    private boolean 			isUpdate = false;
    private long                exitTime = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setView(R.layout.activity_main);
        initView();
    }

    @SuppressLint("JavascriptInterface")
    public void initView(){
        mWebView    = (WebView)findViewById(R.id.webviews);
        WebSettings setting = mWebView.getSettings();
        setting.setJavaScriptCanOpenWindowsAutomatically(true);
        setting.setUseWideViewPort(true);//关键点
        setting.setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        setting.setDisplayZoomControls(false);
        setting.setJavaScriptEnabled(true); // 设置支持javascript脚本
        setting.setAllowFileAccess(true); // 允许访问文件
        setting.setBuiltInZoomControls(true); // 设置显示缩放按钮
        setting.setSupportZoom(true); // 支持缩放
        setting.setLoadWithOverviewMode(true);
        mWebView.addJavascriptInterface(this, "android");
        mWebView.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                // TODO Auto-generated method stub
                view.loadUrl(url);
                return false;
            }
        });
        mWebView.loadUrl(url);
    }

    public void requestURL(){

    }

    //加载数据弹框
    public void showItem() {
        loadview = MainActivity.this.getLayoutInflater().inflate(R.layout.loadingdata, null);
        loadItem = new PopupWindow(loadview, LinearLayout.LayoutParams.MATCH_PARENT,LinearLayout.LayoutParams.MATCH_PARENT, true);
        loadItem.setOutsideTouchable(true);
        loadItem.update();
        if (!loadItem.isShowing()) {
            loadItem.showAtLocation(loadview, Gravity.CENTER, 0, 0);
            loadItem.setFocusable(true);
        }
    }

    //更新UI线程信息
    public Handler recomdHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            switch (msg.what) {
                case 1:
                    showItem();
                    break;
                case 2:
                    loadItem.dismiss();
                    break;
            }
        }
    };

    //请求版本数据
    public void requestUpdate(){
        HttpTask httpTask = new HttpTask();
        httpTask.execute(Constants.API+Constants.UPDATE);
        httpTask.setTaskHandler(new HttpTask.HttpTaskHandler() {
            @Override
            public void taskSuccessful(String json) {
                try {
                    JSONObject root        = new JSONObject(json);
                    JSONArray data       	= root.getJSONArray("data");
                    String     	versions 	= data.getJSONObject(0).getString("version_num");
                    String     	urls       	= data.getJSONObject(0).getString("download_url");
                    String 		content		= data.getJSONObject(0).getString("content");
                    String      update_mode = data.getJSONObject(0).getString("update_mode");
                    Message 	message     = mHandler.obtainMessage();
                    Bundle      bundle   	= new Bundle();
                    bundle.putString("version",versions);
                    bundle.putString("url",urls);
                    bundle.putString("content",content);
                    bundle.putString("update_mode",update_mode);
                    message.what = SUCCESS;
                    message.setData(bundle);
                    mHandler.sendMessage(message);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void taskFailed() {

            }
        });
    }

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            if (msg.what != EMPTY) {
                String           localVersion   = Util.getVersion(MainActivity.this);
                Bundle           bundles        = msg.getData();
                String           serverVersion  = bundles.getString("version");
                downloadUrl           			= bundles.getString("url");
                content							= bundles.getString("content");
                update_mode                     = bundles.getString("update_mode");
                if (localVersion.compareTo(serverVersion) < 0 && update_mode.equals("1")) {
                    ToastUtil.getShortToastByString(MainActivity.this, "有新版本");
                    showChioceUpdateDialog();
                }else if(localVersion.compareTo(serverVersion) < 0 && update_mode.equals("2")){
                    ToastUtil.getShortToastByString(MainActivity.this, "有新版本");
                    showNoChioceUpdateDialog();
                }
            }
        }
    };

    //更新弹窗
    private void showChioceUpdateDialog(){
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(MainActivity.this);
        //normalDialog.setIcon(R.drawable.logo);
        normalDialog.setTitle("模拟新版本");
        normalDialog.setMessage("有新版本是否更新？");
        normalDialog.setCancelable(false);
        normalDialog.setPositiveButton("确定",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!isUpdate) {
                            isUpdate = true;
                            RxPermissions.getInstance(MainActivity.this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe(new Action1<Boolean>() {
                                @Override
                                public void call(Boolean aBoolean) {
                                    if (aBoolean) {
                                        Intent service = new Intent(MainActivity.this, DownLoadService.class);
                                        service.putExtra("downloadurl", "http://120.77.242.46:8088/" + downloadUrl);
                                        ToastUtil.getShortToastByString(MainActivity.this, "正在下载中");
                                        startService(service);
                                    } else {
                                        ToastUtil.getShortToastByString(MainActivity.this, "SD卡下载权限被拒绝");
                                    }
                                }
                            });
                        }
                    }
                });
        normalDialog.setNegativeButton("关闭",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                    }
                });
        normalDialog.show();
    }

    //更新强制弹窗
    private void showNoChioceUpdateDialog(){
        final AlertDialog.Builder normalDialog =
                new AlertDialog.Builder(MainActivity.this);
        //normalDialog.setIcon(R.drawable.logo);
        normalDialog.setTitle("模拟新版本");
        normalDialog.setMessage("有新版本是否更新？");
        normalDialog.setCancelable(false);
        normalDialog.setPositiveButton("更新",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        if (!isUpdate) {
                            isUpdate = true;
                            RxPermissions.getInstance(MainActivity.this).request(Manifest.permission.WRITE_EXTERNAL_STORAGE).subscribe(new Action1<Boolean>() {
                                @Override
                                public void call(Boolean aBoolean) {
                                    if (aBoolean) {
                                        Intent service = new Intent(MainActivity.this, DownLoadService.class);
                                        service.putExtra("downloadurl", "http://120.77.242.46:8088/" + downloadUrl);
                                        ToastUtil.getShortToastByString(MainActivity.this, "正在下载中");
                                        startService(service);
                                    } else {
                                        ToastUtil.getShortToastByString(MainActivity.this, "SD卡下载权限被拒绝");
                                    }
                                }
                            });
                        }
                    }
                });
        normalDialog.show();
    }



    @Override
    protected void onResume() {
        super.onResume();
        MobclickAgent.onResume(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        MobclickAgent.onPause(this);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && !mWebView.canGoBack()) {
            ///对于好多应用，会在程序中杀死 进程，这样会导致我们统计不到此时Activity结束的信息
            ///对于这种情况需要调用 'MobclickAgent.onKillProcess( Context )方法
            ///保存一些页面调用的数据。正常的应用是不需要调用此方法的。
            MobclickAgent.onKillProcess( this );
            int pid = android.os.Process.myPid();
            android.os.Process.killProcess(pid);
            return true;
        }else if (keyCode == KeyEvent.KEYCODE_BACK && mWebView.canGoBack()) {
            mWebView.goBack();// 返回前一个页面
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
