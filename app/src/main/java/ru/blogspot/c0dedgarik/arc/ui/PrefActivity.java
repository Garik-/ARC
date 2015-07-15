package ru.blogspot.c0dedgarik.arc.ui;


import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;

import java.io.IOException;

import ru.blogspot.c0dedgarik.arc.ARCApplication;
import ru.blogspot.c0dedgarik.arc.ARCLog;
import ru.blogspot.c0dedgarik.arc.R;

public class PrefActivity extends PreferenceActivity {

    public static class SettingsFragment extends PreferenceFragment implements SharedPreferences.OnSharedPreferenceChangeListener {

        private SharedPreferences mSharedPreferences;

        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);


            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);

            mSharedPreferences = PreferenceManager.getDefaultSharedPreferences(getActivity());

            try {
                ListPreference lp = (ListPreference) findPreference("video_resolution");
                ARCApplication app = (ARCApplication) getActivity().getApplicationContext();


                String[] cs = app.getVideoStream().getSupportedVideoSizesArray();
                lp.setEntries(cs);
                lp.setEntryValues(cs);
                lp.setValue(mSharedPreferences.getString("video_resolution", "640x480"));

            } catch (IOException e) {
                ARCLog.e(e.getMessage());
            }


            setSummary(mSharedPreferences);
        }

        private void setSummary(SharedPreferences sharedPreferences) {
            EditTextPreference ep = (EditTextPreference) findPreference("port");
            ep.setSummary(sharedPreferences.getString("port", "8080"));

            ep = (EditTextPreference) findPreference("websocket_port");
            ep.setSummary(sharedPreferences.getString("websocket_port", "9090"));

            PreferenceScreen ps = (PreferenceScreen) findPreference("network_settings");
            ps.setSummary(String.format("Порт %s, логин/пароль: не установлены", sharedPreferences.getString("port", "8080")));

            ListPreference lp = (ListPreference) findPreference("video_resolution");
            lp.setSummary(sharedPreferences.getString("video_resolution", "640x480"));
        }

        @Override
        public void onResume() {
            super.onResume();
            // Set up a listener whenever a key changes
            getPreferenceScreen().getSharedPreferences()
                    .registerOnSharedPreferenceChangeListener(this);
        }


        @Override
        public void onPause() {
            super.onPause();
            // Unregister the listener whenever a key changes
            getPreferenceScreen().getSharedPreferences()
                    .unregisterOnSharedPreferenceChangeListener(this);
        }

        public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
            setSummary(sharedPreferences);
        }

    }


}
