package com.example.dell.simulatelottery.getjson;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;

public class NetworkTool {

    public String getContentFromUrl(String url) {
        StringBuilder sb = new StringBuilder();
        try {
            InputStream is = new URL(url).openStream();
            InputStreamReader isr = new InputStreamReader(is, "utf-8");
            BufferedReader br = new BufferedReader(isr);
            String line = null;
            while ((line = br.readLine()) != null) {
                sb.append(line);
            }
            is.close();
        } catch (final IOException e) {
            return null;
        }
        return sb.toString();
    }
}