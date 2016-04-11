package com.example.alexey.yandexartistslist;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.SimpleAdapter;
import android.widget.Toast;

import com.nostra13.universalimageloader.core.DisplayImageOptions;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.ImageLoaderConfiguration;
import com.nostra13.universalimageloader.utils.StorageUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity implements AdapterView.OnItemClickListener{

    Context context;
    ListView lv;
    ArrayList<HashMap<String, Object>> list; // Список который хранит в себе данные о каждом исполнителе
    ImageLoader imageLoader; // Класс для загрузки изображений из сети с последуующим его кешированием в память и в файловую систему
    DisplayImageOptions options; // Опция для ImageLoader
    String JSONFile = null; // Переменная которая хранит в себе прочитанный файл, для того, чтобы не повторять чтение при уничтожении с последующим восстановлением активности
    int listPos = 0; // Переменная для сохранения состояния ListView
    ProgressDialog pd; // Прогресс Диалог покажет сообщение в случае возникновения ошибки или оповестит о процессе загрузки

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        context = this;
        lv = (ListView)findViewById(R.id.listView);
        imageLoader = ImageLoader.getInstance();
        imageLoader.init(ImageLoaderConfiguration.createDefault(this));
        options = new DisplayImageOptions.Builder().showStubImage(R.drawable.smile_v_small).showImageForEmptyUri(R.drawable.smile_v_small)
                .cacheInMemory().cacheOnDisc().build(); // Опция с заданием кеширования в память и файловую систему, а также отображением иного изображения если же оно не было загруженно
    }
    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString("jsonFile", JSONFile); // Сохраняем прочитанные данные из файла
        outState.putInt("list", lv.getFirstVisiblePosition()); // Схраняем состояние ListView
    }
    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        JSONFile = savedInstanceState.getString("jsonFile");
        listPos = savedInstanceState.getInt("list");
    }
    @Override
    protected void onResume() {
        super.onResume();
        readFile(); // Вызываем метод для заполнения ListView и прочих элементов
    }
    @Override
    protected void onPause() {
        super.onPause();
        listPos = lv.getFirstVisiblePosition(); // Сохраняем состояние ListView
    }

    final String FILENAME = "ArtistsFile";

    void readFile()
    {
        if(JSONFile == null) { // Чтобы заново не читать файл, при повороте, например, происходит проверка на заполненность JSONFile, его заполненность означает, что в этот блок повторно не зайдёт
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(openFileInput(FILENAME)))) {
                if (reader.ready()) { // Проверка на то, пустой ли файл, если нет то начинается чтение из файла с последующим заполнением ListView
                    StringBuilder sb = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                    }
                    JSONFile = sb.toString();
                    progressDialog("Обработка данных", "Формирование списка, пожалуйста, подождите", 0);
                    createArrayList();
                    createListView();
                    pd.dismiss();
                } else { // Если файл существует, но он пуст, инициируем загрузку данных из сети
                    progressDialog("Данные ещё не загруженны", "Сейчас будет произведена загрузка", 0);
                    new AsyncDownload().execute("http://download.cdn.yandex.net/mobilization-2016/artists.json"); // Если файла с данныме нету, тогда загружаем его в новой нити (потоке)
                }
            } catch (IOException e) { // если файла ещё нет, значит данные ещё не загружены и файл для них не был создан
                progressDialog("Данные ещё не загруженны", "Сейчас будет произведена загрузка", 0);
                new AsyncDownload().execute("http://download.cdn.yandex.net/mobilization-2016/artists.json"); // Если файла с данныме нету, тогда загружаем его в новой нити (потоке)
            } catch (JSONException e) { // Если возникла непредвиденная ошибка при работе с JSONFile, то появится соответствующий Диалог
                progressDialog("Ошибка загрузки", "Рекомендуется перезагрузить данные из сети", 1);
            }
        } else { // Если файл уже был прочитан, то просто заполняем ListView
            try{
                createArrayList();
                createListView();
            } catch (JSONException e) { // Если возникла непредвиденная ошибка при работе с JSONFile, то появится соответствующий Диалог
                progressDialog("Ошибка загрузки", "Рекомендуется перезагрузить данные из сети", 1);
            }
        }
    }

    // Константы для Получения полей из JSONFile и ключи для HashMap
    public static final String ATTRIBUTE_NAME = "name";
    public static final String ATTRIBUTE_GENRES = "genres";
    public static final String ATTRIBUTE_ALBUMS_TRACKS = "albums_tracks";
    final String ATTRIBUTE_IMAGE_SMALL = "small";

    final String ATTRIBUTE_ID = "id";
    public static final String ATTRIBUTE_LINK = "link";
    public static final String ATTRIBUTE_DESCRIPTION = "description";
    public static final String ATTRIBUTE_IMAGE_BIG = "big";

    protected void createArrayList() throws JSONException{ // Заполняем ArrayList
        JSONArray jsonArray = new JSONArray(JSONFile);
        list = new ArrayList<>(jsonArray.length());
        for (int i = 0; i < jsonArray.length(); i++) {
            JSONObject jsonObject = jsonArray.getJSONObject(i);
            HashMap<String, Object> map = new HashMap<>();
            // Данные для списка
            map.put(ATTRIBUTE_IMAGE_SMALL, jsonObject.getJSONObject("cover").getString(ATTRIBUTE_IMAGE_SMALL));
            map.put(ATTRIBUTE_NAME, jsonObject.getString(ATTRIBUTE_NAME));
            JSONArray array = jsonObject.getJSONArray(ATTRIBUTE_GENRES);
            StringBuilder builder = new StringBuilder();
            for (int j = 0; j < array.length(); j++) {
                if (j != 0) {
                    builder.append(", ");
                }
                builder.append(array.getString(j));
            }
            map.put(ATTRIBUTE_GENRES, builder.toString());
            StringBuilder sb = new StringBuilder();
            wordEdit(sb, jsonObject.getInt("albums"), "альбом", "ов", "", "а");
            sb.append(", ");
            wordEdit(sb, jsonObject.getInt("tracks"), "пес", "ен", "ня", "ни");
            map.put(ATTRIBUTE_ALBUMS_TRACKS, sb.toString());
            // Остальные данные для выбранного пункта
            map.put(ATTRIBUTE_ID, jsonObject.getInt(ATTRIBUTE_ID));
            String link;
            try {
                link = jsonObject.getString(ATTRIBUTE_LINK);
            } catch(JSONException e){
                link = "";
            }
            map.put("link", link);
            map.put(ATTRIBUTE_DESCRIPTION, jsonObject.getString(ATTRIBUTE_DESCRIPTION));
            map.put(ATTRIBUTE_IMAGE_BIG, jsonObject.getJSONObject("cover").getString(ATTRIBUTE_IMAGE_BIG));
            list.add(map);
        }
    }
    protected void createListView(){
        // Заполняем соответствие: данне - view-компоненты
        String[] From = {ATTRIBUTE_IMAGE_SMALL, ATTRIBUTE_NAME, ATTRIBUTE_GENRES, ATTRIBUTE_ALBUMS_TRACKS};
        int[] To = {R.id.imSmall, R.id.name, R.id.genres, R.id.albums_tracks};
        SimpleAdapter simpleAdapter = new SimpleAdapter(this, list, R.layout.list_item, From, To); // Создание адаптера для ListView
        simpleAdapter.setViewBinder(new MyViewBinder()); // Подключение своего ViewBinder`а
        lv.setAdapter(simpleAdapter);
        lv.setOnItemClickListener(this);
        lv.setSelectionFromTop(listPos, 0); // Восстанавливаем состояние (позицию) ListView
    }

    public void wordEdit(StringBuilder sb, int count, String word, String many, String one, String second) // Функция для написания правильного окончания в слове в зависимости от их числа
    {
        sb.append(count + " " + word);
        if(count % 10 == 0 || count % 10 >4 || count % 100 > 10 && count % 100 < 20){
            sb.append(many);
        } else if(count % 10 == 1) {
            sb.append(one);
        } else {
            sb.append(second);
        }
    }

    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) { // Переход в новую активность содержащую общие данные по выбранному пункту
        HashMap<String, Object>  map = list.get(position); // Получение данных по позиции выбранного элемента из списка
        Intent intent = new Intent(this, DescriptionActivity.class);
        intent.putExtra(ATTRIBUTE_NAME, (String)map.get(ATTRIBUTE_NAME));
        intent.putExtra(ATTRIBUTE_DESCRIPTION, (String)map.get(ATTRIBUTE_DESCRIPTION));
        intent.putExtra(ATTRIBUTE_ALBUMS_TRACKS, (String)map.get(ATTRIBUTE_ALBUMS_TRACKS));
        intent.putExtra(ATTRIBUTE_GENRES, (String)map.get(ATTRIBUTE_GENRES));
        intent.putExtra(ATTRIBUTE_LINK, (String) map.get(ATTRIBUTE_LINK));
        intent.putExtra(ATTRIBUTE_IMAGE_BIG, (String) map.get(ATTRIBUTE_IMAGE_BIG));
        startActivity(intent);
        overridePendingTransition(R.anim.right_in, R.anim.left_out); // Анимация перехода
    }

    void progressDialog(String title, String mess, int mod) // Отображение диалога с информацией по состоянию действий в активности
    {
        if(pd != null) // Если какой-либо диалог уже запущен, тогда убераем его перед тем как создать новый
        {
            pd.dismiss();
        }
        pd = new ProgressDialog(this);
        pd.setTitle(title);
        pd.setMessage(mess);
        if(mod != 0){ // Если mod равен нулю, тогда просто отображается информация о том что происходит какая-либо загрузка, в противном случае предоставляется выбор последующих действий
            pd.setButton(Dialog.BUTTON_POSITIVE, "Повторить загрузку", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    deleteFile(FILENAME);
                    readFile();
                }
            });
            pd.setButton(Dialog.BUTTON_NEGATIVE, "Выйти", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
        }
        pd.show();  // Запускаем ProgressDialog, чтобы показать что происходит взаимодействие
    }

    // AlertDialog который удостоверяется в желании выйти из программы
    final int DIALOG_EXIT = 1;
    @Override
    protected Dialog onCreateDialog(int id) {
        if(id == DIALOG_EXIT){
            AlertDialog.Builder adb = new AlertDialog.Builder(this);
            adb.setTitle("Вы действительно хотите выйти?");
            adb.setIcon(android.R.drawable.ic_dialog_info);
            adb.setPositiveButton("Выйти", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) {
                    finish();
                }
            });
            adb.setNegativeButton("Пока нет", new DialogInterface.OnClickListener() {
                @Override
                public void onClick(DialogInterface dialog, int which) { }
            });
            return adb.create();
        }
        return super.onCreateDialog(id);
    }

    // При нажатии на кнопку Back будет вызван Диалог о согласии выйти
    @Override
    public void onBackPressed() {
        showDialog(DIALOG_EXIT);
    }

    class MyViewBinder implements SimpleAdapter.ViewBinder{ // Мой ViewBinder для своей реализации сопостовления View-елемента и данных из Map
        @Override
        public boolean setViewValue(View view, Object data, String textRepresentation) {
            if(view.getId()==R.id.imSmall)
            {
                imageLoader.displayImage(textRepresentation, (ImageView)view, options);
                return true;
            }
            else return false;
        }
    }
    class AsyncDownload extends AsyncTask<String, String, Boolean> {
        @Override
        protected void onPreExecute() {
            super.onPreExecute();
        }

        @Override
        protected Boolean doInBackground(String... params) {
            boolean result = false;
            try {
                 result = getContent(params[0]);
            } catch(IOException e){}
            return result;
        }

        @Override
        protected void onPostExecute(Boolean aVoid) {
            super.onPostExecute(aVoid);
            if(aVoid)
                readFile();
            else {
                progressDialog("Ошибка загрузки", "Рекомендуется перезагрузить данные из сети", 1);
            }
        }

        // Метод для загрузки файла с из сети и последующей его записью в локальный файл
        private boolean getContent(String path) throws IOException {
            BufferedReader reader = null; // Поток для чтения из сети
            try(BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(openFileOutput(FILENAME, MODE_PRIVATE)))) { // Поток для записи в локальный файл
                URL url = new URL(path);
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();
                connection.setRequestMethod("GET");
                connection.connect();
                reader = new BufferedReader(new InputStreamReader(connection.getInputStream()));
                String line;
                while((line = reader.readLine())!=null)
                {
                    writer.write(line);
                }
            } catch (Exception e) {
                return false; //Если нет доступа к сети, тогда возвращаем false
            } finally {
                if(reader != null){
                    reader.close();
                }
            }
            return true;
        }
    }

}
