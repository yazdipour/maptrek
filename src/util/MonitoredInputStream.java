package mobi.maptrek.util;

import android.support.annotation.NonNull;
import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.ConcurrentModificationException;
import java.util.List;

public class MonitoredInputStream extends FilterInputStream {
    private final List<ChangeListener> listeners;
    private volatile long mLastTriggeredLocation;
    private volatile long mLocation;
    private volatile long mMark;
    private final int mThreshold;

    public interface ChangeListener {
        void stateChanged(long j);
    }

    public MonitoredInputStream(InputStream in, int threshold) {
        super(in);
        this.mMark = 0;
        this.mLastTriggeredLocation = 0;
        this.mLocation = 0;
        this.listeners = new ArrayList(1);
        this.mThreshold = threshold;
    }

    public MonitoredInputStream(InputStream in) {
        super(in);
        this.mMark = 0;
        this.mLastTriggeredLocation = 0;
        this.mLocation = 0;
        this.listeners = new ArrayList(1);
        this.mThreshold = 16384;
    }

    public void addChangeListener(ChangeListener l) {
        if (!this.listeners.contains(l)) {
            this.listeners.add(l);
        }
    }

    public void removeChangeListener(ChangeListener l) {
        this.listeners.remove(l);
    }

    public long getProgress() {
        return this.mLocation;
    }

    protected void triggerChanged(long location) {
        if (this.mThreshold <= 0 || Math.abs(location - this.mLastTriggeredLocation) >= ((long) this.mThreshold)) {
            this.mLastTriggeredLocation = location;
            if (this.listeners.size() > 0) {
                try {
                    for (ChangeListener l : this.listeners) {
                        l.stateChanged(location);
                    }
                } catch (ConcurrentModificationException e) {
                    triggerChanged(location);
                }
            }
        }
    }

    public int read() throws IOException {
        int i = super.read();
        if (i != -1) {
            long j = this.mLocation;
            this.mLocation = 1 + j;
            triggerChanged(j);
        }
        return i;
    }

    public int read(@NonNull byte[] b, int off, int len) throws IOException {
        int i = super.read(b, off, len);
        if (i > 0) {
            long j = this.mLocation + ((long) i);
            this.mLocation = j;
            triggerChanged(j);
        }
        return i;
    }

    public long skip(long n) throws IOException {
        long i = super.skip(n);
        if (i > 0) {
            long j = this.mLocation + i;
            this.mLocation = j;
            triggerChanged(j);
        }
        return i;
    }

    public void mark(int readlimit) {
        super.mark(readlimit);
        this.mMark = this.mLocation;
    }

    public void reset() throws IOException {
        super.reset();
        if (this.mLocation != this.mMark) {
            long j = this.mMark;
            this.mLocation = j;
            triggerChanged(j);
        }
    }
}
