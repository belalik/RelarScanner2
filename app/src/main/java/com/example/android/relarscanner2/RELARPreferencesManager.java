package com.example.android.relarscanner2;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * Utility class to provide access to data stored in Shared Preferences.
 * Using the GSON library, we 're able to store the list of steps as a JSON String in Stored Prefs too.
 */
public class RELARPreferencesManager {

    public static final String LIST_OF_STEPS = "steps list";

    static RELARPreferencesManager _instance;

    Context context;
    SharedPreferences sharedPref;
    SharedPreferences.Editor sharedPrefEditor;

    public static RELARPreferencesManager instance (Context context) {
        if (_instance == null) {
            _instance = new RELARPreferencesManager();
            _instance.configSessionUtils(context);
        }
        return _instance;
    }

    public static RELARPreferencesManager instance() {
        return _instance;
    }

    public void configSessionUtils(Context context) {
        this.context = context;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPrefEditor = sharedPref.edit();
    }

    public void storeValueString(String key, String value) {
        sharedPrefEditor.putString(key, value);
        sharedPrefEditor.commit();
    }

    public String fetchValueString(String key) {
        return sharedPref.getString(key, null);
    }

    public boolean fetchBoolean(String key) {
        return sharedPref.getBoolean(key, false);
    }

    /**
     * Only used while testing - this method will erase everything from shared prefs,
     * hence "emulating" the first run of the app (no list saved).
     *
     */
    public void deleteAllPreferences() {
        sharedPrefEditor.clear();
        sharedPrefEditor.apply();
    }

}
