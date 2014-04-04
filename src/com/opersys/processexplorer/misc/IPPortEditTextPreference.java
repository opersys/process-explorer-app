package com.opersys.processexplorer.misc;

import android.content.Context;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.util.AttributeSet;

/**
 * Date: 01/04/14
 * Time: 11:59 PM
 */
public class IPPortEditTextPreference extends EditTextPreference {

    public IPPortEditTextPreference(Context context) {
        super(context);
        init();
    }

    public IPPortEditTextPreference(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    private void init() {
        setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

            @Override
            public boolean onPreferenceChange(Preference pref, Object newValue) {
                pref.setSummary(getText());
                return true;
            }
        });
    }

    @Override
    public CharSequence getSummary() {
        return super.getText();
    }
}
