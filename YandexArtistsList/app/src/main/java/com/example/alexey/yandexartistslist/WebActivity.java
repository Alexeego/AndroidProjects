package com.example.alexey.yandexartistslist;

import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.MenuItem;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class WebActivity extends AppCompatActivity {

    private WebView web;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_web);
        Uri data = getIntent().getData();
        // Настройка ActionView
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        // Настройка WebView
        web = (WebView)findViewById(R.id.web);
        web.getSettings().setJavaScriptEnabled(true);
        web.getSettings().setDisplayZoomControls(true);
        web.setWebViewClient(new MyWebViewClient());
        web.loadUrl(data.toString());

    }
    // Класс для обработки нажатий ссылок в WebView
    private class MyWebViewClient extends WebViewClient{
        @Override
        public boolean shouldOverrideUrlLoading(WebView view, String url) {
            view.loadUrl(url);
            return true;
        }
    }

    // Обработка нажатия на кнопку Back с анимацией
    @Override
    public void onBackPressed() {
        if(web.canGoBack()){
            web.goBack();
        } else {
            finish();
            overridePendingTransition(R.anim.alpha_in, R.anim.web_out);
        }
    }
    // Обработка нажатия на стрелку возврата в ActionBar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home)
        {
            finish();
            overridePendingTransition(R.anim.alpha_in, R.anim.web_out);
        }
        return super.onOptionsItemSelected(item);
    }
}
