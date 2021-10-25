package com.alexsykes.trackmonster.activities;

import android.content.SharedPreferences;
import android.os.Bundle;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.ListPreference;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreference;

import com.alexsykes.trackmonster.R;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);
        if (savedInstanceState == null) {
            getSupportFragmentManager()
                    .beginTransaction()
                    .replace(R.id.settings, new SettingsFragment())
                    .commit();
        }
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayHomeAsUpEnabled(true);
        }
    }

    public static class SettingsFragment extends PreferenceFragmentCompat {
        SharedPreferences prefs;
        boolean useGPSonly;
        boolean trackingOn;

        @Override
        public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
            prefs = PreferenceManager.getDefaultSharedPreferences(getContext());
            setPreferencesFromResource(R.xml.root_preferences, rootKey);
            SharedPreferences.Editor editor = prefs.edit();

            setup();
        }

        private void setup() {
            useGPSonly = prefs.getBoolean("useGPSonly", true);
            trackingOn = prefs.getBoolean("trackingOn", true);

            SwitchPreference useGPSPref = findPreference("useGPSonly");
            assert useGPSPref != null;

            useGPSPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    useGPSonly = Boolean.valueOf(newValue.toString());
                    useGPSPref.setChecked(useGPSonly);
                    return false;
                }
            });

            SwitchPreference trackingOnPref = findPreference("trackingOn");
            assert trackingOnPref != null;

            trackingOnPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    trackingOn = Boolean.valueOf(newValue.toString());
                    trackingOnPref.setChecked(trackingOn);
                    return false;
                }
            });

            ListPreference intervalPref = findPreference("intervalString");
            assert intervalPref != null;

            intervalPref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {
                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    SharedPreferences.Editor editor = prefs.edit();
                    intervalPref.setValue(newValue.toString());
                    int samplingInterval = Integer.parseInt(newValue.toString());
                    editor.putInt("interval", samplingInterval);
                    editor.putString("intervalString", newValue.toString());
                    editor.apply();
                    return false;
                }
            });
        }

    }
}