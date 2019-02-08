package mobi.maptrek.ui;

import android.content.Context;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.CoordinatorLayout.Behavior;
import android.support.design.widget.Snackbar.SnackbarLayout;
import android.util.AttributeSet;
import android.view.View;
import android.widget.RelativeLayout;
import java.util.List;
import mobi.maptrek.R;

public class BottomPanelBehavior extends Behavior<RelativeLayout> {
    private final boolean mIsLandscape;

    public BottomPanelBehavior(Context context, AttributeSet attrs) {
        this.mIsLandscape = context.getResources().getBoolean(R.bool.isLandscape);
    }

    public boolean layoutDependsOn(CoordinatorLayout parent, RelativeLayout child, View dependency) {
        return dependency instanceof SnackbarLayout;
    }

    public boolean onDependentViewChanged(CoordinatorLayout parent, RelativeLayout child, View dependency) {
        if (dependency instanceof SnackbarLayout) {
            float width = 0.0f;
            float shift = 0.0f;
            List<View> dependencies = parent.getDependencies(child);
            int z = dependencies.size();
            for (int i = 0; i < z; i++) {
                View view = (View) dependencies.get(i);
                if ((view instanceof SnackbarLayout) && parent.doViewsOverlap(child, view)) {
                    shift = Math.min(shift, view.getTranslationY() - ((float) view.getHeight()));
                    width = (float) (parent.getWidth() - view.getWidth());
                }
            }
            if (!this.mIsLandscape) {
                child.setTranslationY(shift);
                return true;
            } else if (width < 10.0f) {
                child.setPaddingRelative(0, 0, 0, (int) (-shift));
                return true;
            }
        }
        return false;
    }

    public void onDependentViewRemoved(CoordinatorLayout parent, RelativeLayout child, View dependency) {
        if (dependency instanceof SnackbarLayout) {
            child.setPaddingRelative(0, 0, 0, 0);
            child.setTranslationY(0.0f);
        }
    }
}
