package com.example.dell.simulatelottery.getjson;

import android.os.AsyncTask;
import android.util.Log;

public class HttpTask extends AsyncTask<String, Integer, String>
{
    private static final String TAG = "HTTP_TASK";
    public HttpTaskHandler taskHandler;

    @Override
    protected String doInBackground(String... params) {
        // Performed on Background Thread
        String url = params[0];
        try {
            String json = new NetworkTool().getContentFromUrl(url);
            return json;
        } catch (Exception e) {
            // TODO handle different exception cases
            Log.e(TAG, e.toString());
            e.printStackTrace();
            return null;
        }
    }

    public void setTaskHandler(HttpTaskHandler taskHandler)
    {
        this.taskHandler = taskHandler;
    }

    @Override
    protected void onPostExecute(String json) {
        // Done on UI Thread
        if (json != null && json != "") {
            Log.d(TAG, "taskSuccessful");
            int i1 = json.indexOf("["), i2 = json.indexOf("{"), i = i1 > -1 && i1 < i2 ? i1 : i2;
            if (i > -1) {
                json = json.substring(i);
                taskHandler.taskSuccessful(json);
            } else {
                Log.d(TAG, "taskFailed");
                taskHandler.taskFailed();
            }
        } else {
            Log.d(TAG, "taskFailed");
            taskHandler.taskFailed();
        }
    }

    public static interface HttpTaskHandler
    {
        void taskSuccessful(String json);
        void taskFailed();
    }


}