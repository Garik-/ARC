package ru.blogspot.c0dedgarik.arc.ui;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import java.io.IOException;

import ru.blogspot.c0dedgarik.arc.ARCApplication;
import ru.blogspot.c0dedgarik.arc.ARCLog;
import ru.blogspot.c0dedgarik.arc.R;

public class PrefActivity extends PreferenceActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref);

        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

        try {
            ListPreference lp = (ListPreference) findPreference("video_resolution");
            ARCApplication app = (ARCApplication) this.getApplicationContext();


            String[] cs = app.getVideoStream().getSupportedVideoSizesArray();
            lp.setEntries(cs);
            lp.setEntryValues(cs);
            lp.setValue(sp.getString("video_resolution", "640x480"));

        } catch (IOException e) {
            ARCLog.e(e.getMessage());
        }


        setSummary(sp);

    }


    private void getSupportedVideoSizes() {

    }

    @Override
    protected void onResume() {
        super.onResume();
        // Set up a listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .registerOnSharedPreferenceChangeListener(this);
    }

    private void setSummary(SharedPreferences sp) {
        EditTextPreference ep = (EditTextPreference) findPreference("port");
        ep.setSummary(sp.getString("port", "8080"));

        ep = (EditTextPreference) findPreference("websocket_port");
        ep.setSummary(sp.getString("websocket_port", "9090"));

        PreferenceScreen ps = (PreferenceScreen) findPreference("network_settings");
        ps.setSummary(String.format("Порт %s, логин/пароль: не установлены", sp.getString("port", "8080")));

        ListPreference lp = (ListPreference) findPreference("video_resolution");
        lp.setSummary(sp.getString("video_resolution", "640x480"));
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Unregister the listener whenever a key changes
        getPreferenceScreen().getSharedPreferences()
                .unregisterOnSharedPreferenceChangeListener(this);
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        setSummary(sharedPreferences);
    }
}
