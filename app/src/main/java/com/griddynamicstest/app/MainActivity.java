package com.griddynamicstest.app;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.griddynamicstest.service.DownloadIntentService;

import java.util.Formatter;


public class MainActivity extends Activity {

    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            switch (intent.getAction()) {
                case DownloadIntentService.DOWNLOAD_FINISHED:
                    TextView textView = (TextView) findViewById(R.id.result);
                    textView.setText(
                            String.format(
                                    getResources().getString(R.string.formatted_result),
                                    intent.getStringExtra(DownloadIntentService.EXTRA_URL),
                                    intent.getIntExtra(DownloadIntentService.EXTRA_TOTAL_PRINTABLE, -1)));
                    break;
                case DownloadIntentService.MALFORMED_URL_ERROR:
                    handleUrlError(intent.getStringExtra(DownloadIntentService.EXTRA_URL));
                    break;
                case DownloadIntentService.NETWORK_ERROR:
                    handleNetworkError();
                    break;
                case DownloadIntentService.SDCARD_ERROR:
                    handleSdcardError();
                    break;
                case DownloadIntentService.UNKNOWN_HOST_ERROR:
                    handleUnknnownHostkError();
                    break;
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        final EditText editText = (EditText) findViewById(R.id.url_edit_text);
        findViewById(R.id.perform_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String url = editText.getText().toString();
                if (!URLUtil.isNetworkUrl(url)) {
                    handleUrlError(url);
                    return;
                }
                Intent intent = new Intent(MainActivity.this, DownloadIntentService.class);
                intent.setAction(DownloadIntentService.ACTION_DOWNLOAD);
                intent.putExtra(DownloadIntentService.EXTRA_URL, url);
                startService(intent);
            }
        });
    }

    @Override
    protected void onStart() {
        super.onStart();
        IntentFilter filter = new IntentFilter();
        filter.addAction(DownloadIntentService.DOWNLOAD_FINISHED);
        filter.addAction(DownloadIntentService.MALFORMED_URL_ERROR);
        filter.addAction(DownloadIntentService.NETWORK_ERROR);
        filter.addAction(DownloadIntentService.SDCARD_ERROR);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }

    @Override
    protected void onStop() {
        super.onStop();
        LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
    }

    private void handleUrlError(String url) {
        Toast.makeText(this, String.format(getResources().getString(R.string.malformed_url), url), Toast.LENGTH_SHORT).show();
    }

    private void handleNetworkError() {
        Toast.makeText(this, R.string.network_error, Toast.LENGTH_SHORT).show();
    }

    private void handleUnknnownHostkError() {
        Toast.makeText(this, R.string.unknown_host_error, Toast.LENGTH_SHORT).show();
    }

    private void handleSdcardError() {
        Toast.makeText(this, R.string.sdcard_error, Toast.LENGTH_SHORT).show();
    }

}
