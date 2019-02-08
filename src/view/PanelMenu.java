package mobi.maptrek.view;

import android.support.annotation.IdRes;
import android.view.MenuItem;

public interface PanelMenu {

    public interface OnPrepareMenuListener {
        void onPrepareMenu(PanelMenu panelMenu);
    }

    MenuItem add(@IdRes int i, int i2, CharSequence charSequence);

    MenuItem findItem(@IdRes int i);

    void removeItem(@IdRes int i);
}
