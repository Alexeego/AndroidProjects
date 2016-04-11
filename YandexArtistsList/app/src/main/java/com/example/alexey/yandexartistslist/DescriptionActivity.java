package com.example.alexey.yandexartistslist;

import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;

public class DescriptionActivity extends AppCompatActivity {

    TextView link;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_description);
        ImageLoader imageLoader = ImageLoader.getInstance(); // Класс для работы с изображениями: загрузки, кеширования и др.
        imageLoader.init(ImageLoaderConfiguration.createDefault(this));
        DisplayImageOptions options = new DisplayImageOptions.Builder().showStubImage(R.drawable.smiley_mal).showImageForEmptyUri(R.drawable.smiley_mal)
                .cacheInMemory().cacheOnDisc().build(); // Опция с заданием кеширования в память и файловую систему, а также отображением иного изображения если же оно не было загруженно
        Intent intent = getIntent();
        imageLoader.displayImage(intent.getStringExtra(MainActivity.ATTRIBUTE_IMAGE_BIG), (ImageView) findViewById(R.id.imBig), options);
        // Вставка текста из intent в соответствующие текстовые поля
        ((TextView)findViewById(R.id.description)).setText(intent.getStringExtra(MainActivity.ATTRIBUTE_NAME) + " "
                + intent.getStringExtra(MainActivity.ATTRIBUTE_DESCRIPTION));
        ((TextView)findViewById(R.id.genres2)).setText(intent.getStringExtra(MainActivity.ATTRIBUTE_GENRES));
        ((TextView)findViewById(R.id.albums)).setText(intent.getStringExtra(MainActivity.ATTRIBUTE_ALBUMS_TRACKS));
        if(!intent.getStringExtra(MainActivity.ATTRIBUTE_LINK).isEmpty()) // Если ссылка есть тогда подключаем поле отображения ссылки
        {
            findViewById(R.id.linkInfo).setVisibility(View.VISIBLE);
            link = (TextView)findViewById(R.id.link);
            link.setVisibility(View.VISIBLE);
            link.setText(Html.fromHtml(intent.getStringExtra(MainActivity.ATTRIBUTE_LINK)));
            link.setPaintFlags(Paint.UNDERLINE_TEXT_FLAG);
            link.setOnClickListener(onClickListener);
        }
        // Настройка ActionBar
        getSupportActionBar().setTitle(intent.getStringExtra(MainActivity.ATTRIBUTE_NAME));
        getSupportActionBar().setHomeButtonEnabled(true);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    }

    // Обработка нажатия на стрелку возврата в ActionBar
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if(item.getItemId() == android.R.id.home)
        {
            finish();
            overridePendingTransition(R.anim.left_in, R.anim.right_out);
        }
        return super.onOptionsItemSelected(item);
    }

    // Обработка нажатия на кнопку Back с анимацией
    @Override
    public void onBackPressed() {
        finish();
        overridePendingTransition(R.anim.left_in, R.anim.right_out);
    }

    View.OnClickListener onClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            if(v.getId() == R.id.link) { // Обработчик нажатия на ссылку с открытием встроенного WebView с анимацией
                String url = ((TextView)v).getText().toString();
                Intent intent = new Intent(DescriptionActivity.this, WebActivity.class);
                intent.setData(Uri.parse(url));
                startActivity(intent);
                overridePendingTransition(R.anim.web_in, R.anim.alpha_out);
            }
        }
    };
}
