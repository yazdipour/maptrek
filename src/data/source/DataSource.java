package mobi.maptrek.data.source;

import android.database.Cursor;
import java.util.HashSet;
import java.util.Set;

public abstract class DataSource {
    public static final int TYPE_TRACK = 1;
    public static final int TYPE_WAYPOINT = 0;
    private boolean loadable = true;
    private boolean loaded = false;
    private final Set<DataSourceUpdateListener> mListeners = new HashSet();
    public String name;
    private boolean visible = false;

    public @interface DataType {
    }

    public abstract Cursor getCursor();

    @DataType
    public abstract int getDataType(int i);

    public abstract boolean isNativeTrack();

    public boolean isLoaded() {
        return this.loaded;
    }

    public void setLoaded() {
        this.loaded = true;
    }

    public boolean isLoadable() {
        return this.loadable;
    }

    public void setLoadable(boolean loadable) {
        this.loadable = loadable;
    }

    public boolean isVisible() {
        return this.visible;
    }

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public void addListener(DataSourceUpdateListener listener) {
        this.mListeners.add(listener);
    }

    public void removeListener(DataSourceUpdateListener listener) {
        this.mListeners.remove(listener);
    }

    public void notifyListeners() {
        for (DataSourceUpdateListener listener : this.mListeners) {
            listener.onDataSourceUpdated();
        }
    }
}
