package mobi.maptrek.fragments;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.SharedPreferences.OnSharedPreferenceChangeListener;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import mobi.maptrek.Configuration;
import mobi.maptrek.Configuration.ChangedEvent;
import mobi.maptrek.R;
import org.greenrobot.eventbus.EventBus;

public class Settings extends PreferenceFragment implements OnSharedPreferenceChangeListener {
    public static final String ARG_HILLSHADES_AVAILABLE = "hillshades_available";
    private FragmentHolder mFragmentHolder;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        if (!getArguments().getBoolean(ARG_HILLSHADES_AVAILABLE, false)) {
            ((PreferenceCategory) findPreference("category_advanced")).removePreference(findPreference(Configuration.PREF_HILLSHADES_TRANSPARENCY));
        }
        findPreference("reset_advices").setOnPreferenceClickListener(new OnPreferenceClickListener() {
            public boolean onPreferenceClick(Preference preference) {
                Settings.this.mFragmentHolder.popCurrent();
                return false;
            }
        });
        getPreferenceScreen().getSharedPreferences().registerOnSharedPreferenceChangeListener(this);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_settings, container, false);
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mFragmentHolder = (FragmentHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement FragmentHolder");
        }
    }

    public void onDetach() {
        super.onDetach();
        this.mFragmentHolder = null;
    }

    public void onResume() {
        super.onResume();
        PreferenceScreen preferenceScreen = getPreferenceScreen();
        for (int i = 0; i < preferenceScreen.getPreferenceCount(); i++) {
            Preference preference = preferenceScreen.getPreference(i);
            if (preference instanceof PreferenceGroup) {
                PreferenceGroup preferenceGroup = (PreferenceGroup) preference;
                for (int j = 0; j < preferenceGroup.getPreferenceCount(); j++) {
                    Preference subPref = preferenceGroup.getPreference(j);
                    updatePreference(subPref, subPref.getKey());
                }
            } else {
                updatePreference(preference, preference.getKey());
            }
        }
    }

    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        EventBus.getDefault().post(new ChangedEvent(key));
        updatePreference(findPreference(key), key);
    }

    private void updatePreference(Preference preference, String key) {
        if (preference != null && (preference instanceof ListPreference)) {
            ListPreference listPreference = (ListPreference) preference;
            listPreference.setSummary(listPreference.getEntry());
        }
    }
}
