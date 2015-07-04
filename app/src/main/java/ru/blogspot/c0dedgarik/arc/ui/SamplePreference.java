package ru.blogspot.c0dedgarik.arc.ui;

import android.content.Context;
import android.preference.Preference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import ru.blogspot.c0dedgarik.arc.R;


public class SamplePreference extends Preference {

    public SamplePreference(Context context) {
        super(context);
    }

    @Override
    protected View onCreateView(ViewGroup parent) {
        LayoutInflater li = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        return li.inflate(R.layout.sample_preference, parent, false);
    }
}
