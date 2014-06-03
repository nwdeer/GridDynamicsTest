package com.griddynamicstest.service;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.net.http.AndroidHttpClient;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;
import android.util.Patterns;
import android.view.KeyEvent;
import android.webkit.URLUtil;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

public class DownloadIntentService extends IntentService {
    private static final String SERVICE_NAME = "DownloadIntentService";

    public static final String ACTION_DOWNLOAD = "com.griddynamicstest.service.action.download";

    public static final String EXTRA_URL = "com.griddynamicstest.service.extra.url";
    public static final String EXTRA_TOTAL_PRINTABLE = "com.griddynamicstest.service.extra.total";

    public static final String DOWNLOAD_FINISHED = "com.griddynamicstest.service.download.finished";
    public static final String MALFORMED_URL_ERROR = "com.griddynamicstest.service.malformed.url";
    public static final String NETWORK_ERROR = "com.griddynamicstest.service.network.error";
    public static final String UNKNOWN_HOST_ERROR = "com.griddynamicstest.service.unknown.host.error";
    public static final String SDCARD_ERROR = "com.griddynamicstest.service.sdcard.error";

    public DownloadIntentService() {
        super(SERVICE_NAME);
    }

    private static final int BUFFER_SIZE = 16 * 1024; //16 kb
    private byte[] buffer = new byte[BUFFER_SIZE];

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent != null) {
            final String action = intent.getAction();
            if (action == null) {
                return;
            }
            switch (action) {
                case ACTION_DOWNLOAD:
                    final String stringUrl = intent.getStringExtra(EXTRA_URL);
                    final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
                    if (!isUrlValid(stringUrl)) {
                        Intent errorIntent = new Intent(MALFORMED_URL_ERROR);
                        errorIntent.putExtra(EXTRA_URL, stringUrl);
                        localBroadcastManager.sendBroadcast(errorIntent);
                        return;
                    }
                    AndroidHttpClient client = AndroidHttpClient.newInstance(SERVICE_NAME, this);
                    HttpResponse response = null;
                    try {
                        HttpGet get = new HttpGet(stringUrl);
                        response = client.execute(get);
                        InputStream inputStream = response.getEntity().getContent(); // will be closed by consumeContent
                        OutputStream fileOutputStream = new FileOutputStream(getFileName(this, stringUrl));
                        int totalPrintable = 0;
                        int len = 0;
                        //Need to separate network errors from errors with writing to SDCARD.
                        //UI might handle these in different ways (offer to clear sdcard, etc.)
                        try {
                            while ((len = inputStream.read(buffer)) != -1) {
                                totalPrintable += countPrintable(buffer, len);
                                fileOutputStream.write(buffer, 0, len);
                            }
                        } catch (IOException e) {
                            e.printStackTrace();
                            localBroadcastManager.sendBroadcast(new Intent(SDCARD_ERROR));
                            return;
                        } finally {
                            if (fileOutputStream != null) {
                                fileOutputStream.flush();
                                fileOutputStream.close();
                            }
                        }
                        Intent result = new Intent(DOWNLOAD_FINISHED);
                        result.putExtra(EXTRA_URL, stringUrl);
                        result.putExtra(EXTRA_TOTAL_PRINTABLE, totalPrintable);
                        localBroadcastManager.sendBroadcast(result);
                    } catch (UnknownHostException e) {
                        localBroadcastManager.sendBroadcast(new Intent(UNKNOWN_HOST_ERROR));
                        e.printStackTrace();
                        return;
                    } catch (IOException e) {
                        localBroadcastManager.sendBroadcast(new Intent(NETWORK_ERROR));
                        e.printStackTrace();
                        return;
                    } finally {
                        client.close();
                        if (response != null) {
                            try {
                                response.getEntity().consumeContent();
                            } catch (IOException e) {
                                e.printStackTrace();

                            }
                        }
                    }
                    break;
            }
        }
    }

    private static int countPrintable(byte[] bytes, int len) {
        int result = 0;
        for (int i = 0; i < len; i++) {
            if (isPrintableChar((char) bytes[i])) {
                result++;
            }
        }
        return result;
    }

    private static boolean isPrintableChar(char c) {
        Character.UnicodeBlock block = Character.UnicodeBlock.of(c);
        return (!Character.isISOControl(c)) &&
                c != KeyEvent.KEYCODE_UNKNOWN &&
                block != null &&
                block != Character.UnicodeBlock.SPECIALS;
    }

    public static String getFileName(Context context, String url) {
        return context.getFilesDir() + File.separator + toMD5(url);
    }

    public static boolean isUrlValid(String url) {
        return Patterns.WEB_URL.matcher(url).matches();
    }

    private static String toMD5(String s) {
        try {
            // Create MD5 Hash
            MessageDigest digest = java.security.MessageDigest.getInstance("MD5");
            digest.update(s.getBytes());
            byte messageDigest[] = digest.digest();

            // Create Hex String
            StringBuilder hexString = new StringBuilder();
            for (int i = 0; i < messageDigest.length; i++) {
                String h = Integer.toHexString(0xFF & messageDigest[i]);
                while (h.length() < 2)
                    h = "0" + h;
                hexString.append(h);
            }
            return hexString.toString();

        } catch (NoSuchAlgorithmException e) {
            Log.e(SERVICE_NAME, "MD5 algorithm is not found.", e);
        }
        return null;
    }


}
