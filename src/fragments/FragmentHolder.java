package mobi.maptrek.fragments;

import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.FloatingActionButton;

public interface FragmentHolder {
    void addBackClickListener(OnBackPressedListener onBackPressedListener);

    void disableActionButton();

    void disableListActionButton();

    FloatingActionButton enableActionButton();

    FloatingActionButton enableListActionButton();

    CoordinatorLayout getCoordinatorLayout();

    String getStatsString();

    void popAll();

    void popCurrent();

    void removeBackClickListener(OnBackPressedListener onBackPressedListener);
}
