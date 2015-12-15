package com.example.koktoh.smartpetproject;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

public class MainActivity extends AppCompatActivity {

    private final static String TAG = MainActivity.class.getSimpleName();

    private ImageView iv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        getSupportActionBar().hide();

        setContentView(R.layout.activity_main);

        iv = (ImageView) findViewById(R.id.imageView);
        iv.setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION);
        iv.setImageResource(R.drawable.face_nomal);
    }
}
