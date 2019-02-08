package mobi.maptrek.util;

public interface ProgressListener {
    void onProgressAnnotated(String str);

    void onProgressChanged(int i);

    void onProgressFinished();

    void onProgressStarted(int i);
}
