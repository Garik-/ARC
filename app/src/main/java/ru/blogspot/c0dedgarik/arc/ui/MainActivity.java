package ru.blogspot.c0dedgarik.arc.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceView;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;

import ru.blogspot.c0dedgarik.arc.ARCApplication;
import ru.blogspot.c0dedgarik.arc.ARCLog;
import ru.blogspot.c0dedgarik.arc.HttpServer;
import ru.blogspot.c0dedgarik.arc.R;


public class MainActivity extends ActionBarActivity implements View.OnClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private ARCApplication mApplication;
    private HttpServer mHttpServer;
    private SharedPreferences mSharedPreferences;
    private Resources mResources;

    private SurfaceView mSurfaceView;
    private ToggleButton mBtnPower;


    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btnPower: {

                if (mBtnPower.isChecked() && !mHttpServer.wasStarted()) {
                    try {

                        mSurfaceView.setVisibility(View.VISIBLE);

                        mHttpServer.start();
                        mApplication.toast(mResources.getString(R.string.status_on), Toast.LENGTH_LONG);


                    } catch (IOException e) {

                        mBtnPower.setChecked(false);
                        ARCLog.e("HttpServer: %s", e.getMessage());
                    }
                } else {
                    mHttpServer.stop();
                    mSurfaceView.setVisibility(View.GONE);

                    mApplication.toast(mResources.getString(R.string.status_off), Toast.LENGTH_LONG);
                }

                //mBtnPower.setChecked(mHttpServer.wasStarted());
            }
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mBtnPower = (ToggleButton) findViewById(R.id.btnPower);
        mSurfaceView = (SurfaceView) findViewById(R.id.SurfaceView01);

        mApplication = (ARCApplication) getApplication();
        mResources = mApplication.getResources();
        mHttpServer = mApplication.getHttpServer();

        mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(mApplication);

        mApplication.getVideoStream().setPreviewDisplay(mSurfaceView.getHolder());

        String ip = mHttpServer.getLocalIpAddress(true);

        if (null != ip) {
            TextView tv = (TextView) findViewById(R.id.tvStatus);
            tv.setText(ip);
            mBtnPower.setOnClickListener(this);
        }
    }


    @Override
    public void onResume() {
        super.onResume();

        mSharedPreferences.registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    public void onPause() {
        super.onPause();

        mSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onDestroy() {

        if (mHttpServer.wasStarted()) {
            mHttpServer.stop();
        }

        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.

        switch (item.getItemId()) {
            case R.id.action_settings:
                Intent intent = new Intent(this, PrefActivity.class);
                intent.putExtra(PreferenceActivity.EXTRA_SHOW_FRAGMENT, PrefActivity.SettingsFragment.class.getName());
                intent.putExtra(PreferenceActivity.EXTRA_NO_HEADERS, true);

                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        ARCLog.d("preferences change");
    }
}
