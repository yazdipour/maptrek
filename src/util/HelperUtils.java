package mobi.maptrek.util;

import android.app.Activity;
import android.content.Context;
import android.graphics.Rect;
import android.support.annotation.DrawableRes;
import android.support.annotation.StringRes;
import android.support.design.widget.CoordinatorLayout;
import android.support.design.widget.Snackbar;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import com.getkeepsafe.taptargetview.TapTarget;
import com.getkeepsafe.taptargetview.TapTargetView;
import com.getkeepsafe.taptargetview.TapTargetView.Listener;
import mobi.maptrek.Configuration;
import mobi.maptrek.R;

public class HelperUtils {
    public static void showError(String message, CoordinatorLayout coordinatorLayout) {
        final Snackbar snackbar = Snackbar.make((View) coordinatorLayout, (CharSequence) message, -2);
        snackbar.setAction((int) R.string.actionDismiss, new OnClickListener() {
            public void onClick(View view) {
                snackbar.dismiss();
            }
        });
        ((TextView) snackbar.getView().findViewById(R.id.snackbar_text)).setMaxLines(99);
        snackbar.show();
    }

    public static void showSaveError(Context context, CoordinatorLayout coordinatorLayout, Exception e) {
        showError(context.getString(R.string.msgSaveFailed, new Object[]{e.getMessage()}), coordinatorLayout);
    }

    public static void showAdvice(final long advice, int messageResId, CoordinatorLayout coordinatorLayout) {
        if (Configuration.getAdviceState(advice)) {
            Snackbar snackbar = Snackbar.make((View) coordinatorLayout, messageResId, -2).setAction((int) R.string.actionGotIt, new OnClickListener() {
                public void onClick(View view) {
                    Configuration.setAdviceState(advice);
                }
            });
            ((TextView) snackbar.getView().findViewById(R.id.snackbar_text)).setMaxLines(99);
            snackbar.show();
        }
    }

    public static boolean showTargetedAdvice(Activity activity, long advice, @StringRes int messageResId, View focusOn, @DrawableRes int icon) {
        if (!Configuration.getAdviceState(advice)) {
            return false;
        }
        TapTarget target = TapTarget.forView(focusOn, activity.getString(messageResId));
        target.icon(activity.getDrawable(icon));
        showTargetedAdvice(activity, advice, target);
        return true;
    }

    public static boolean showTargetedAdvice(Activity activity, long advice, @StringRes int messageResId, View focusOn, boolean transparent) {
        if (!Configuration.getAdviceState(advice)) {
            return false;
        }
        TapTarget target;
        if (transparent) {
            Rect r = new Rect();
            focusOn.getGlobalVisibleRect(r);
            target = TapTarget.forBounds(r, activity.getString(messageResId)).transparentTarget(true);
        } else {
            target = TapTarget.forView(focusOn, activity.getString(messageResId));
        }
        showTargetedAdvice(activity, advice, target);
        return true;
    }

    public static boolean showTargetedAdvice(Activity activity, long advice, @StringRes int messageResId, Rect rect) {
        if (!Configuration.getAdviceState(advice)) {
            return false;
        }
        showTargetedAdvice(activity, advice, TapTarget.forBounds(rect, activity.getString(messageResId)).transparentTarget(true));
        return true;
    }

    private static void showTargetedAdvice(Activity activity, final long advice, TapTarget target) {
        target.tintTarget(false);
        TapTargetView.showFor(activity, target, new Listener() {
            public void onOuterCircleClick(TapTargetView view) {
                view.dismiss(false);
            }

            public void onTargetDismissed(TapTargetView view, boolean userInitiated) {
                Configuration.setAdviceState(advice);
            }
        });
    }

    public static boolean needsTargetedAdvice(long advice) {
        return Configuration.getAdviceState(advice);
    }
}
