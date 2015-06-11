package ru.blogspot.c0dedgarik.arc.ui;


import android.os.Bundle;
import android.preference.PreferenceActivity;

import ru.blogspot.c0dedgarik.arc.R;

public class PrefActivity extends PreferenceActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.pref);
    }
}
