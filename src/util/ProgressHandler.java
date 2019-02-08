package mobi.maptrek.util;

import android.os.Handler;
import android.os.Message;
import android.support.annotation.CallSuper;
import android.widget.ProgressBar;

public class ProgressHandler extends Handler implements ProgressListener {
    public static final int BEGIN_PROGRESS = 1;
    public static final int STOP_PROGRESS = 3;
    public static final int UPDATE_PROGRESS = 2;
    ProgressBar mProgressBar;

    public ProgressHandler(ProgressBar progressBar) {
        this.mProgressBar = progressBar;
    }

    @CallSuper
    public void handleMessage(Message msg) {
        switch (msg.what) {
            case 1:
                this.mProgressBar.setVisibility(0);
                this.mProgressBar.setMax(msg.arg1);
                return;
            case 2:
                this.mProgressBar.setProgress(msg.arg1);
                return;
            case 3:
                this.mProgressBar.setVisibility(8);
                return;
            default:
                return;
        }
    }

    public final void onProgressStarted(int length) {
        sendMessage(obtainMessage(1, length, 0));
    }

    public final void onProgressChanged(int progress) {
        sendMessage(obtainMessage(2, progress, 0));
    }

    public final void onProgressFinished() {
        sendMessage(obtainMessage(3));
    }

    public void onProgressAnnotated(String annotation) {
    }
}
