package com.example.koktoh.smartpetproject;

import android.Manifest;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.net.Uri;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.speech.RecognitionListener;
import android.speech.RecognizerIntent;
import android.speech.SpeechRecognizer;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.PermissionChecker;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.appindexing.Action;
import com.google.android.gms.appindexing.AppIndex;
import com.google.android.gms.common.api.GoogleApiClient;

import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.CameraBridgeViewBase;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfPoint;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.imgproc.Imgproc;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Timer;
import java.util.TimerTask;

public class MainActivity extends AppCompatActivity implements View.OnTouchListener, CameraBridgeViewBase.CvCameraViewListener2 {

    private final static String TAG = MainActivity.class.getSimpleName();

    private final int REQUEST_CODE = 59;
    private SpeechRecognizer sr;
    private MyRecognitionListener listener;
    private Intent intent;

    private ImageView iv;
    private Bitmap bmp;

    private int level;

    private BluetoothGattCharacteristic characteristicTx = null;
    private RBLService mBluetoothLeService;
    private BluetoothAdapter mBluetoothAdapter;
    private BluetoothDevice mDevice = null;
    private String mDeviceAddress;

    private boolean flag = true;
    private boolean connState = false;
    private boolean scanFlag = false;
    private boolean motorMove = false;

    private static final int REQUEST_ENABLE_BT = 1;
    private static final long SCAN_PERIOD = 2000;

    final private static char[] hexArray = {'0', '1', '2', '3', '4', '5', '6',
            '7', '8', '9', 'a', 'b', 'c', 'd', 'e', 'f'};

    private Mat mRgba;
    private Mat mRgbaT;
    private Mat mRgbaF;
    private Scalar mBlobColorHsv;
    private ColorBlobDetector mDetector;
    private Scalar CONTOUR_COLOR;
    private int centerX;

    private boolean playing = false;

    private CameraBridgeViewBase mOpenCvCameraView;

    private AudioManager mAudioManager;
    private int currentVolume = 0;
    boolean mute = false;

    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS: {
                    Log.i(TAG, "OpenCV loaded successfully");
                    mOpenCvCameraView.enableView();
                    mOpenCvCameraView.setOnTouchListener(MainActivity.this);
                }
                break;
                default: {
                    super.onManagerConnected(status);
                }
                break;
            }
        }
    };

    static {
        if (!OpenCVLoader.initDebug()) {
            Log.d(TAG, "Failed OpenCVLoader.initDebug");
        }
    }

    public MainActivity() {
        Log.i(TAG, "Instantiated new " + this.getClass());
    }

    private final ServiceConnection mServiceConnection = new ServiceConnection() {

        @Override
        public void onServiceConnected(ComponentName componentName,
                                       IBinder service) {
            Log.d(TAG, "mBluetoothLeService set");
            mBluetoothLeService = ((RBLService.LocalBinder) service)
                    .getService();
            Log.d(TAG, "mBluetoothLeService seted");
            if (!mBluetoothLeService.initialize()) {
                Log.e(TAG, "Unable to initialize Bluetooth");
                finish();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName componentName) {
            mBluetoothLeService = null;
        }
    };

    private final BroadcastReceiver mGattUpdateReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            final String action = intent.getAction();

            if (RBLService.ACTION_GATT_DISCONNECTED.equals(action)) {
                Toast.makeText(getApplicationContext(), "Disconnected",
                        Toast.LENGTH_SHORT).show();
                connState = false;
            } else if (RBLService.ACTION_GATT_SERVICES_DISCOVERED
                    .equals(action)) {
                Toast.makeText(getApplicationContext(), "Connected",
                        Toast.LENGTH_SHORT).show();

                getGattService(mBluetoothLeService.getSupportedGattService());
            } else if (RBLService.ACTION_DATA_AVAILABLE.equals(action)) {
            } else if (RBLService.ACTION_GATT_RSSI.equals(action)) {
            }
        }
    };
    /**
     * ATTENTION: This was auto-generated to implement the App Indexing API.
     * See https://g.co/AppIndexing/AndroidStudio for more information.
     */
    private GoogleApiClient client;

    @Override
    public void onStart() {
        super.onStart();

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.connect();
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.koktoh.smartpetproject/http/host/path")
        );
        AppIndex.AppIndexApi.start(client, viewAction);
    }

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

            Log.e(TAG, "Recognizerエラー リスナ再登録");
            releaseRecognize();

            startRecognize();
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
            muteAudio();
        }

        @Override
        public void onResults(Bundle results) {
            Log.d(TAG, "on results");
            unmuteAudio();

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
            } else if (resultList.contains("遊ぼう") || resultList.contains("あそぼう")) {
                playing = true;
                releaseBitmap();
                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.face_happy);
                iv.setImageBitmap(bmp);
                Toast.makeText(getApplicationContext(), "いいよ！", Toast.LENGTH_LONG).show();
            } else if (resultList.contains("終わり") || resultList.contains("おわり")) {
                playing = false;
                releaseBitmap();
                bmp = BitmapFactory.decodeResource(getResources(), R.drawable.face_nomal);
                iv.setImageBitmap(bmp);
                Toast.makeText(getApplicationContext(), "楽しかった！", Toast.LENGTH_LONG).show();
            } else if (resultList.contains("お休み") || resultList.contains("おやすみ") || resultList.contains("じゃあね") || resultList.contains("ばいばい") || resultList.contains("バイバイ") || resultList.contains("さようなら") || resultList.contains("さよなら")) {
                Toast.makeText(getApplicationContext(), "おやすみ", Toast.LENGTH_LONG).show();
                finish();
            } else if (resultList.contains("前") || resultList.contains("まえ")) {
                runMotor("F", 500);
            } else if (resultList.contains("後ろ") || resultList.contains("うしろ")) {
                runMotor("B", 500);
            } else if (resultList.contains("右") || resultList.contains("みぎ")) {
                runMotor("R", 500);
            } else if (resultList.contains("左") || resultList.contains("ひだり")) {
                runMotor("L", 500);
            } else if (resultList.contains("接続")) {
                if (connState == false) {
                    connectLeDevice();
                }
            }

            startRecognize();
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

        List<String> permissionList = new ArrayList<>();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (this.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.ACCESS_COARSE_LOCATION);
            }

            if (this.checkSelfPermission(Manifest.permission.BLUETOOTH) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.BLUETOOTH);
            }

            if (this.checkSelfPermission(Manifest.permission.BLUETOOTH_ADMIN) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.BLUETOOTH_ADMIN);
            }

            if (this.checkSelfPermission(Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.CAMERA);
            }

            if (PermissionChecker.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                permissionList.add(Manifest.permission.RECORD_AUDIO);
            } else {
                sr = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
                sr.setRecognitionListener(listener);
                sr.startListening(intent);
            }

            if (permissionList.size() > 0) {
                requestPermissions(permissionList.toArray(new String[permissionList.size()]), REQUEST_CODE);
            }
        }

        iv = (ImageView) findViewById(R.id.imageView);
        bmp = BitmapFactory.decodeResource(getResources(), R.drawable.face_nomal);
        iv.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        iv.setImageBitmap(bmp);

        if (!getPackageManager().hasSystemFeature(
                PackageManager.FEATURE_BLUETOOTH_LE)) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
                    .show();
            finish();
        }

        final BluetoothManager mBluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
        mBluetoothAdapter = mBluetoothManager.getAdapter();
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Ble not supported", Toast.LENGTH_SHORT)
                    .show();
            finish();
            return;
        }

        Intent gattServiceIntent = new Intent(MainActivity.this,
                RBLService.class);
        Log.d(TAG, gattServiceIntent.toString());
        bindService(gattServiceIntent, mServiceConnection, BIND_AUTO_CREATE);

        mOpenCvCameraView = (CameraBridgeViewBase) findViewById(R.id.javaCameraView);
        mOpenCvCameraView.setCameraIndex(CameraBridgeViewBase.CAMERA_ID_FRONT);
        mOpenCvCameraView.setCvCameraViewListener(this);

//        iv.setVisibility(View.INVISIBLE);

        mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client = new GoogleApiClient.Builder(this).addApi(AppIndex.API).build();
    }

    @Override
    protected void onStop() {
        Log.d(TAG, "on stop");
        super.onStop();
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        Action viewAction = Action.newAction(
                Action.TYPE_VIEW, // TODO: choose an action type.
                "Main Page", // TODO: Define a title for the content shown.
                // TODO: If you have web page content that matches this app activity's content,
                // make sure this auto-generated web page URL is correct.
                // Otherwise, set the URL to null.
                Uri.parse("http://host/path"),
                // TODO: Make sure this auto-generated app deep link URI is correct.
                Uri.parse("android-app://com.example.koktoh.smartpetproject/http/host/path")
        );
        AppIndex.AppIndexApi.end(client, viewAction);
        if (sr != null) {
            sr.destroy();
            sr = null;
        }

        flag = false;
        unregisterReceiver(mGattUpdateReceiver);
        // ATTENTION: This was auto-generated to implement the App Indexing API.
        // See https://g.co/AppIndexing/AndroidStudio for more information.
        client.disconnect();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        if (mServiceConnection != null) {
            unbindService(mServiceConnection);
        }

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
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

        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableBtIntent = new Intent(
                    BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);
        }

        registerReceiver(mGattUpdateReceiver, makeGattUpdateIntentFilter());

        mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);

        startRecognize();
    }

    @Override
    protected void onPause() {
        super.onPause();

        unregisterReceiver(mBroadcastReceiver);

        if (mOpenCvCameraView != null) {
            mOpenCvCameraView.disableView();
        }

        releaseRecognize();
    }

    private void muteAudio() {
        if (!mute) {
            currentVolume = mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0);
            mute = true;
        }
    }

    private void unmuteAudio() {
        if (mute) {
            mAudioManager.setStreamVolume(AudioManager.STREAM_MUSIC, currentVolume, 0);
            mute = false;
        }
    }

    private void startRecognize() {
        if (sr == null) {
            sr = SpeechRecognizer.createSpeechRecognizer(getApplicationContext());
        }
        sr.setRecognitionListener(listener);
        sr.startListening(intent);
    }

    private void releaseRecognize() {
        if (sr != null) {
            sr.stopListening();
            sr.destroy();
            sr = null;
        }
    }

    private void runMotor(String command, long period) {
        motorMove = true;
        Log.i(TAG, "motorMove:" + motorMove);

        if (characteristicTx.setValue(command)) {
            mBluetoothLeService.writeCharacteristic(characteristicTx);
            Log.d(TAG, "Send command:" + command);
        }

        try {
            Thread.sleep(period);
            Log.d(TAG, "Sleep " + period);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if (characteristicTx.setValue("S")) {
            mBluetoothLeService.writeCharacteristic(characteristicTx);
            Log.d(TAG, "Stop");
        }

        motorMove = false;
        Log.i(TAG, "motorMove:" + motorMove);
    }

    private void startReadRssi() {
        new Thread() {
            public void run() {

                while (flag) {
                    mBluetoothLeService.readRssi();
                    try {
                        sleep(500);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }

            ;
        }.start();
    }

    private void getGattService(BluetoothGattService gattService) {
        if (gattService == null)
            return;

        connState = true;
        startReadRssi();

        characteristicTx = gattService
                .getCharacteristic(RBLService.UUID_BLE_SHIELD_TX);

        BluetoothGattCharacteristic characteristicRx = gattService
                .getCharacteristic(RBLService.UUID_BLE_SHIELD_RX);
        mBluetoothLeService.setCharacteristicNotification(characteristicRx,
                true);
        mBluetoothLeService.readCharacteristic(characteristicRx);
    }

    private static IntentFilter makeGattUpdateIntentFilter() {
        final IntentFilter intentFilter = new IntentFilter();

        intentFilter.addAction(RBLService.ACTION_GATT_CONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_DISCONNECTED);
        intentFilter.addAction(RBLService.ACTION_GATT_SERVICES_DISCOVERED);
        intentFilter.addAction(RBLService.ACTION_DATA_AVAILABLE);
        intentFilter.addAction(RBLService.ACTION_GATT_RSSI);

        return intentFilter;
    }

    private void connectLeDevice() {
        scanLeDevice();

        Timer mTimer = new Timer();
        mTimer.schedule(new TimerTask() {

            @Override
            public void run() {
                if (mDevice != null) {
                    Log.d(TAG, "mDevice exists");
                    mDeviceAddress = mDevice.getAddress();
                    mBluetoothLeService.connect(mDeviceAddress);
                    scanFlag = true;
                } else {
                    Log.d(TAG, "mDevice not exists");
                    runOnUiThread(new Runnable() {
                        public void run() {
                            Toast toast = Toast
                                    .makeText(
                                            MainActivity.this,
                                            "Couldn't search Ble Shiled device!",
                                            Toast.LENGTH_SHORT);
                            toast.setGravity(0, 0, Gravity.CENTER);
                            toast.show();
                        }
                    });
                }
            }
        }, SCAN_PERIOD);

        System.out.println(connState);
        if (connState == false) {
            mBluetoothLeService.connect(mDeviceAddress);
        } else {
            mBluetoothLeService.disconnect();
            mBluetoothLeService.close();
            connState = false;
        }
    }

    private void scanLeDevice() {
        new Thread() {

            @Override
            public void run() {
                mBluetoothAdapter.startLeScan(mLeScanCallback);

                try {
                    Thread.sleep(SCAN_PERIOD);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                mBluetoothAdapter.stopLeScan(mLeScanCallback);
            }
        }.start();
    }

    private BluetoothAdapter.LeScanCallback mLeScanCallback = new BluetoothAdapter.LeScanCallback() {

        @Override
        public void onLeScan(final BluetoothDevice device, final int rssi,
                             final byte[] scanRecord) {

            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    byte[] serviceUuidBytes = new byte[16];
                    String serviceUuid = "";
                    for (int i = 32, j = 0; i >= 17; i--, j++) {
                        serviceUuidBytes[j] = scanRecord[i];
                    }
                    serviceUuid = bytesToHex(serviceUuidBytes);
                    if (stringToUuidString(serviceUuid).equals(
                            RBLGattAttributes.BLE_SHIELD_SERVICE
                                    .toUpperCase(Locale.ENGLISH))) {
                        mDevice = device;
                    }
                }
            });
        }
    };

    private String bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];
        int v;
        for (int j = 0; j < bytes.length; j++) {
            v = bytes[j] & 0xFF;
            hexChars[j * 2] = hexArray[v >>> 4];
            hexChars[j * 2 + 1] = hexArray[v & 0x0F];
        }
        return new String(hexChars);
    }

    private String stringToUuidString(String uuid) {
        StringBuffer newString = new StringBuffer();
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(0, 8));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(8, 12));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(12, 16));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(16, 20));
        newString.append("-");
        newString.append(uuid.toUpperCase(Locale.ENGLISH).substring(20, 32));

        return newString.toString();
    }

    public void onCameraViewStarted(int width, int height) {
        mRgba = new Mat(height, width, CvType.CV_8UC4);
        mRgbaF = new Mat(height, width, CvType.CV_8UC4);
        mRgbaT = new Mat(width, width, CvType.CV_8UC4);
        mDetector = new ColorBlobDetector();
        CONTOUR_COLOR = new Scalar(0, 255, 0, 255);
//        mBlobColorHsv = new Scalar(30, 250, 200, 0);    // 黄色
        mBlobColorHsv = new Scalar(240, 240, 200, 0);   // 赤
        mDetector.setHsvColor(mBlobColorHsv);
        centerX = width / 2;
    }

    public void onCameraViewStopped() {
        mRgba.release();
    }

    public boolean onTouch(View v, MotionEvent event) {
        return false;
    }

    ;

    public Mat onCameraFrame(CameraBridgeViewBase.CvCameraViewFrame inputFrame) {
        mRgba = inputFrame.rgba();

        Core.transpose(mRgba, mRgbaT);
        Imgproc.resize(mRgbaT, mRgbaF, mRgbaF.size(), 0, 0, 0);
        Core.flip(mRgbaF, mRgba, 0);

        if (playing) {
            try {
                mDetector.process(mRgba);
                List<MatOfPoint> contours = mDetector.getContours();
                Log.i(TAG, "Contours count: " + contours.size());
                Imgproc.drawContours(mRgba, contours, -1, CONTOUR_COLOR);

                MatOfPoint mop = contours.get(0);
                double xMin = mop.get(0, 0)[0];
                double xMax = mop.get(0, 0)[0];
                double yMin = mop.get(0, 0)[1];
                double yMax = mop.get(0, 0)[1];
                for (int i = 0; i < mop.rows(); i++) {
                    for (int j = 0; j < mop.cols(); j++) {
                        double[] temp = mop.get(i, j);
                        if (temp[0] < xMin) {
                            xMin = temp[0];
                        }
                        if (temp[0] > xMax) {
                            xMax = temp[0];
                        }
                        if (temp[1] < yMin) {
                            yMin = temp[1];
                        }
                        if (temp[1] > yMax) {
                            yMax = temp[1];
                        }
                    }
                }

                double x = xMin + (xMax - xMin) / 2;
                double y = yMin + (yMax - yMin) / 2;

                Point p = new Point(x, y);
                Scalar color = new Scalar(0, 0, 255, 255);
                Core.circle(mRgba, p, 20, color, 2);

                double distance = centerX - x;
                Log.i(TAG, "distance:" + distance);
                Core.line(mRgba, p, new Point(centerX, y), color, 2);

                double area = mDetector.getArea();
                Log.i(TAG, "area:" + area);

                System.out.println(!motorMove);
                if (connState && !motorMove) {
                    if (distance < -100) {
                        Log.d(TAG, "right");
                        runMotor("R", 25);
                    } else if (distance > 100) {
                        Log.d(TAG, "left");
                        runMotor("L", 25);
                    } else {
                        if (area < 1000) {
                            runMotor("F", 25);
                        } else if (area > 2000) {
                            runMotor("B", 25);
                        }
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, e.getMessage());
            }
        }

        return mRgba;
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
