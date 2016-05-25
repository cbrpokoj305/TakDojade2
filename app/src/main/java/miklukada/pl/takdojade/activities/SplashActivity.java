package miklukada.pl.takdojade.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;

import miklukada.pl.takdojade.R;

public class SplashActivity extends Activity {

    private final String TAG = getClass().getSimpleName();
    private Handler mHandler;
    private final int SPLASH_TIME = 500;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mHandler = new Handler();
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                Intent mapIntent = new Intent(SplashActivity.this, MapActivityTwo.class);
                startActivity(mapIntent);
                finish();
            }
        },SPLASH_TIME);
    }
}
