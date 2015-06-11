package ru.blogspot.c0dedgarik.arc.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.IOException;
import java.util.HashMap;

import ru.blogspot.c0dedgarik.arc.APCApplication;
import ru.blogspot.c0dedgarik.arc.ARCLog;
import ru.blogspot.c0dedgarik.arc.HttpServer;
import ru.blogspot.c0dedgarik.arc.R;


public class MainActivity extends ActionBarActivity implements View.OnClickListener {


    private HashMap<Integer, Integer> mCommands;

    private APCApplication mApplication;
    private HttpServer mHttpServer;
    private ToggleButton mBtnPower;


    @Override
    public void onClick(View v) {
        int id = v.getId();
        switch (id) {
            case R.id.btnPower: {

                if (false == mHttpServer.wasStarted()) {
                    try {
                        mHttpServer.start();
                        mApplication.toast("Web server started", Toast.LENGTH_LONG);

                    } catch (IOException e) {
                        ARCLog.e("HttpServer: %s", e.getMessage());
                    }
                } else {
                    mHttpServer.stop();
                    mApplication.toast("Web server is stopped", Toast.LENGTH_LONG);
                }

                mBtnPower.setChecked(mHttpServer.wasStarted());
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mApplication = (APCApplication) getApplication();


        mHttpServer = mApplication.getHttpServer();


        String ip = mHttpServer.getLocalIpAddress(true);

        if (null != ip) {
            TextView tv = (TextView) findViewById(R.id.tvStatus);
            tv.setText(ip);

            mBtnPower = (ToggleButton) findViewById(R.id.btnPower);
            mBtnPower.setOnClickListener(this);
        }



        /*


        mCommands = new HashMap<Integer, Integer>();
        //mCommands.put(R.id.btnUp, R.raw.up);
        //mCommands.put(R.id.btnDown, R.raw.down);

        Set<Integer> keys = mCommands.keySet();
        for (Integer key : keys) {
            Button btn = (Button) findViewById(key.intValue());
            btn.setOnClickListener(this);
        }*/


    }

    @Override
    protected void onDestroy() {

        if (mHttpServer.wasStarted())
            mHttpServer.stop(); // ??? при смене экрана активити убивается...

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
                startActivity(intent);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }

    }


}
