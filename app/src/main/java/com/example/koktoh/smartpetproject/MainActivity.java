package com.example.koktoh.smartpetproject;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.BatteryManager;
import android.os.Bundle;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private final int REQUEST_CODE = 59;
    private SpeechRecognizer sr;
    private MyRecognitionListener listener;
    private Intent intent;

    private ImageView iv;
    private Bitmap bmp;

    private int level;

    private class MyRecognitionListener implements RecognitionListener {
        @Override
        public void onBeginningOfSpeech() {
            Log.d(TAG, "on beginning of speech");
        }

        @Override
        public void onBufferReceived(byte[] buffer) {
            Log.d(TAG, "on buffer received");
        }

        @Override
        public void onEndOfSpeech() {
            Log.d(TAG, "on end of speech");
        }

        @Override
        public void onError(int error) {
            switch (error) {
                case SpeechRecognizer.ERROR_AUDIO:
                    Log.e(TAG, "音声データ保存失敗");
                    break;
                case SpeechRecognizer.ERROR_CLIENT:
                    Log.e(TAG, "Android端末内のエラー(その他)");
                    break;
                case SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS:
                    Log.e(TAG, "権限無し");
                    break;
                case SpeechRecognizer.ERROR_NETWORK:
                    Log.e(TAG, "ネットワークエラー(その他)");
                    break;
                case SpeechRecognizer.ERROR_NETWORK_TIMEOUT:
                    Log.e(TAG, "ネットワークタイムアウトエラー");
                    break;
                case SpeechRecognizer.ERROR_NO_MATCH:
                    Log.e(TAG, "音声認識結果無し");
                    break;
                case SpeechRecognizer.ERROR_RECOGNIZER_BUSY:
                    Log.e(TAG, "RecognitionServiceへ要求出せず");
                    break;
                case SpeechRecognizer.ERROR_SERVER:
                    Log.e(TAG, "Server側からエラー通知");
                    break;
                case SpeechRecognizer.ERROR_SPEECH_TIMEOUT:
                    Log.e(TAG, "音声入力無し");
                    break;
                default:
            }

            Log.e(TAG, "Recognizerエラー　リスナ再登録");
            sr.stopListening();
            sr.destroy();
            sr = null;

            sr = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
            sr.setRecognitionListener(listener);
            sr.startListening(intent);
        }

        @Override
        public void onEvent(int eventType, Bundle params) {
            Log.d(TAG, "on event");
        }

        @Override
        public void onPartialResults(Bundle partialResults) {
            Log.d(TAG, "on partial results");
        }

        @Override
        public void onReadyForSpeech(Bundle params) {
            Log.d(TAG, "on ready for speech");
        }

        @Override
        public void onResults(Bundle results) {
            Log.d(TAG, "on results");

            ArrayList<String> resultList = results.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION);

            String result = "";
            for (String s : resultList) {
                result += s + ",";
            }
            Log.d(TAG, result);

            if (resultList.contains("おはよう")) {
                Log.d(TAG, "おはよう");
                releaseBitmap();
                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.face_nomal);
                iv.setImageBitmap(bmp);
            } else if (resultList.contains("つかれた")) {
                Log.d(TAG, "つかれた");
                releaseBitmap();
                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.face_tired);
                iv.setImageBitmap(bmp);
            } else if (resultList.contains("おこる")) {
                Log.d(TAG, "おこる");
                releaseBitmap();
                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.face_angry);
                iv.setImageBitmap(bmp);
            } else if (resultList.contains("しまった")) {
                Log.d(TAG, "しまった");
                releaseBitmap();
                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.face_oops);
                iv.setImageBitmap(bmp);
            } else if (resultList.contains("おやすみ")) {
                Log.d(TAG, "おやすみ");
                releaseBitmap();
                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.face_sleep);
                iv.setImageBitmap(bmp);
            } else if (resultList.contains("げんき") || resultList.contains("元気")) {
                Log.d(TAG, "げんき");
                releaseBitmap();
                if (level < 25) {
                    bmp = BitmapFactory.decodeResource(getResources(), R.drawable.face_oops);
                    iv.setImageBitmap(bmp);
                    Toast.makeText(getApplicationContext(), "もうダメ...", Toast.LENGTH_LONG).show();
                } else if (level < 65) {
                    bmp = BitmapFactory.decodeResource(getResources(), R.drawable.face_tired);
                    iv.setImageBitmap(bmp);
                    Toast.makeText(getApplicationContext(), "ちょっと疲れた", Toast.LENGTH_LONG).show();
                } else {
                    bmp = BitmapFactory.decodeResource(getResources(), R.drawable.face_nomal);
                    iv.setImageBitmap(bmp);
                    Toast.makeText(getApplicationContext(), "元気！", Toast.LENGTH_LONG).show();
                }
            }
//            sr.stopListening();
//            sr.destroy();
//            sr = null;
//            sr = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
            sr.setRecognitionListener(listener);
            sr.startListening(intent);
        }

        @Override
        public void onRmsChanged(float rmsdB) {
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_main);

        intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, getPackageName());

        listener = new MyRecognitionListener();

        if (PermissionChecker.checkSelfPermission(this, android.Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            Log.d(TAG, "パーミッション未許可");
            Log.d(TAG, "パーミッション許可要求");
            ActivityCompat.requestPermissions(this, new String[]{android.Manifest.permission.RECORD_AUDIO}, REQUEST_CODE);
        } else {
            sr = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
            sr.setRecognitionListener(listener);
            sr.startListening(intent);
        }

        iv = (ImageView) findViewById(R.id.imageView);
        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.face_nomal);
        iv.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        iv.setImageBitmap(bmp);
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "on stop");
        super.onStop();
        if (sr != null) {
            sr.destroy();
            sr = null;
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        switch (requestCode) {
            case REQUEST_CODE: {
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    sr = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
                    sr.setRecognitionListener(listener);
                } else {
                    Toast.makeText(this, "パーミッションを許可してください\nアプリケーションを終了します", Toast.LENGTH_LONG).show();
                    this.finish();
                }
            }
        }
    }

    private void releaseBitmap() {
        if (bmp != null) {
            bmp.recycle();
            bmp = null;
        }
    }

    @Override
    protected void onResume() {
        super.onResume();

        IntentFilter filter = new IntentFilter();

        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(mBroadcastReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mBroadcastReceiver);
    }

    private BroadcastReceiver mBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(Intent.ACTION_BATTERY_CHANGED)) {
                int status = intent.getIntExtra("status", 0);
                int health = intent.getIntExtra("health", 0);
                boolean present = intent.getBooleanExtra("present", false);
                level = intent.getIntExtra("level", 0);
                int scale = intent.getIntExtra("scale", 0);
                int icon_small = intent.getIntExtra("icon-small", 0);
                int plugged = intent.getIntExtra("plugged", 0);
                int voltage = intent.getIntExtra("voltage", 0);
                int temperature = intent.getIntExtra("temperature", 0);
                String technology = intent.getStringExtra("technology");

                String statusString = "";

                switch (status) {
                    case BatteryManager.BATTERY_STATUS_UNKNOWN:
                        statusString = "unknown";
                        break;
                    case BatteryManager.BATTERY_STATUS_CHARGING:
                        statusString = "charging";
                        break;
                    case BatteryManager.BATTERY_STATUS_DISCHARGING:
                        statusString = "discharging";
                        break;
                    case BatteryManager.BATTERY_STATUS_NOT_CHARGING:
                        statusString = "not charging";
                        break;
                    case BatteryManager.BATTERY_STATUS_FULL:
                        statusString = "full";
                        break;
                }

                String healthString = "";

                switch (health) {
                    case BatteryManager.BATTERY_HEALTH_UNKNOWN:
                        healthString = "unknown";
                        break;
                    case BatteryManager.BATTERY_HEALTH_GOOD:
                        healthString = "good";
                        break;
                    case BatteryManager.BATTERY_HEALTH_OVERHEAT:
                        healthString = "overheat";
                        break;
                    case BatteryManager.BATTERY_HEALTH_DEAD:
                        healthString = "dead";
                        break;
                    case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE:
                        healthString = "voltage";
                        break;
                    case BatteryManager.BATTERY_HEALTH_UNSPECIFIED_FAILURE:
                        healthString = "unspecified failure";
                        break;
                }

                String acString = "";

                switch (plugged) {
                    case BatteryManager.BATTERY_PLUGGED_AC:
                        acString = "plugged ac";
                        break;
                    case BatteryManager.BATTERY_PLUGGED_USB:
                        acString = "plugged usb";
                        break;
                }

                if (level == 25) {
                    releaseBitmap();
                    bmp = BitmapFactory.decodeResource(getResources(), R.drawable.face_tired);
                    iv.setImageBitmap(bmp);
                    Toast.makeText(getApplicationContext(), "お腹空いた...", Toast.LENGTH_LONG).show();
                }

                Log.v("status", statusString);
                Log.v("health", healthString);
                Log.v("present", String.valueOf(present));
                Log.v("level", String.valueOf(level));
                Log.v("scale", String.valueOf(scale));
                Log.v("icon_small", String.valueOf(icon_small));
                Log.v("plugged", acString);
                Log.v("voltage", String.valueOf(voltage));
                Log.v("temperature", String.valueOf(temperature));
                Log.v("technology", technology);
            }
        }
    };

}
