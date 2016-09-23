package com.example.alexey.dictaphone;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.database.Cursor;
import android.media.MediaRecorder;
import android.os.IBinder;
import android.provider.ContactsContract;
import android.telephony.PhoneNumberUtils;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RecordingService extends Service {

    volatile ExecutorService executorService = Executors.newFixedThreadPool(1);
    volatile boolean recording = false;
    volatile boolean startRecord = false;
    BroadcastReceiver broadcastReceiver;

    public RecordingService() {
    }

    @Override
    public IBinder onBind(Intent intent) {
        // TODO: Return the communication channel to the service.
        return null;
    }

    @Override
    public void onCreate() {
        Toast.makeText(this, "create", Toast.LENGTH_SHORT).show();
        broadcastReceiver = new BroadcastReceiver() {
            String phoneNumber = null;
            String inOrOut = "";
            @Override
            public void onReceive(Context context, Intent intent) {
                if (intent.getAction().equals("android.intent.action.NEW_OUTGOING_CALL")) {
                    //исходящий вызов
                    phoneNumber = intent.getExtras().getString("android.intent.extra.PHONE_NUMBER");
                    inOrOut = ":Out";
                    startRecord = true;
                } else if (intent.getAction().equals("android.intent.action.PHONE_STATE")) {

                    String phoneState = intent.getStringExtra(TelephonyManager.EXTRA_STATE);

                    if (phoneState.equals(TelephonyManager.EXTRA_STATE_RINGING)) {
                        //телефон звонит, получаем входящий номер
                        phoneNumber = intent.getStringExtra(TelephonyManager.EXTRA_INCOMING_NUMBER);
                        inOrOut = ":In";
                        startRecord = true;

                    } else if (phoneState.equals(TelephonyManager.EXTRA_STATE_OFFHOOK)) {
                        //Телефон находится в режиме звонка (набор номера при исходящем звонке / разговор)
                        if (startRecord) {
                            startRecord = false;
                            recording = true;
                            String callName = "(" + phoneNumber + ")";
                            // Определяем Имя звонящего по номеру телефона, если он сохранён в телефонной книге
                            if (phoneNumber != null) {
                                phoneNumber = PhoneNumberUtils.stripSeparators(phoneNumber);

                                String[] projection = new String[]
                                        {ContactsContract.Data.CONTACT_ID,
                                                ContactsContract.Contacts.LOOKUP_KEY,
                                                ContactsContract.Contacts.DISPLAY_NAME,
                                                ContactsContract.Contacts.STARRED,
                                                ContactsContract.Contacts.CONTACT_STATUS,
                                                ContactsContract.Contacts.CONTACT_PRESENCE};

                                String selection = "PHONE_NUMBERS_EQUAL(" +
                                        ContactsContract.CommonDataKinds.Phone.NUMBER + ",?) AND " +
                                        ContactsContract.Data.MIMETYPE + "='" +
                                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE + "'";

                                String selectionArgs[] = {phoneNumber};

                                Cursor cursor = context.getContentResolver().query(ContactsContract.Data.CONTENT_URI, projection, selection, selectionArgs, null);

                                if ((cursor != null ? cursor.getCount() : 0) > 0) {
                                    cursor.moveToFirst();
                                    callName = cursor.getString(2) + callName;
                                }
                            }
                            executorService.submit(new Record(new MediaRecorder(), callName + inOrOut));
                        }
                    } else if (phoneState.equals(TelephonyManager.EXTRA_STATE_IDLE)) {
                        //Телефон находится в ждущем режиме - это событие наступает по окончанию разговора
                        //или в ситуации "отказался поднимать трубку и сбросил звонок".
                        if (executorService != null) {
                            recording = false;
                            startRecord = false;
                            phoneNumber = null;
                        }
                    }
                }
            }
        };
        registerReceiver(broadcastReceiver, new IntentFilter("android.intent.action.PHONE_STATE"));
        registerReceiver(broadcastReceiver, new IntentFilter("android.intent.action.NEW_OUTGOING_CALL"));
    }

    @Override
    public void onDestroy() {
        recording = false;
        executorService.shutdownNow();
        if (broadcastReceiver != null) {
            unregisterReceiver(broadcastReceiver);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }


    private class Record implements Callable<Void> {
        MediaRecorder myAudioRecorder;
        String callName;

        public Record(MediaRecorder myAudioRecorder, String callName) {
            this.myAudioRecorder = myAudioRecorder;
            try {
                this.myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.VOICE_CALL);
            } catch (RuntimeException ignore) {
                this.myAudioRecorder.setAudioSource(MediaRecorder.AudioSource.MIC);
            }
            this.myAudioRecorder.setOutputFormat(MediaRecorder.OutputFormat.THREE_GPP);
            this.myAudioRecorder.setAudioEncoder(MediaRecorder.OutputFormat.AMR_NB);
            this.callName = callName;
        }

        @Override
        public Void call() {
            File sdPath = new File(MainActivity.FILES_PATH);
            boolean pathExists = true;
            if (!sdPath.exists())
                pathExists = sdPath.mkdir();
            if (pathExists)
                try {
                    String outputFile = sdPath.toString() + "/"
                            + new SimpleDateFormat("yyyy|MM|dd-HH:mm:ss-").format(new Date()) + callName + ".3gp";
                    myAudioRecorder.setOutputFile(outputFile);
                    myAudioRecorder.prepare();
                    myAudioRecorder.start();
                    while (!Thread.currentThread().isInterrupted() && recording) {
                    }
                    if (!recording) {
                        myAudioRecorder.stop();
                        myAudioRecorder.release();
                    }
                } catch (IOException ignored) {
                }
            return null;
        }
    }
}
