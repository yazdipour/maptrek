package mobi.maptrek.util;

import java.util.LinkedHashMap;
import java.util.Map.Entry;
import org.oscim.android.canvas.AndroidGraphics;
import org.oscim.backend.canvas.Bitmap;

public class BitmapCache<K, V extends Bitmap> {
    private final LinkedHashMap<K, V> cache;

    BitmapCache(int maxEntries) {
        final int i = maxEntries;
        this.cache = new LinkedHashMap<K, V>(maxEntries, 0.75f, true) {
            protected boolean removeEldestEntry(Entry<K, V> entry) {
                return size() > i;
            }
        };
    }

    public void put(K key, V value) {
        synchronized (this.cache) {
            this.cache.put(key, value);
        }
    }

    public V get(K key) {
        V bitmap;
        synchronized (this.cache) {
            bitmap = (Bitmap) this.cache.get(key);
            if (bitmap == null || AndroidGraphics.getBitmap(bitmap).isRecycled()) {
                bitmap = null;
            }
        }
        return bitmap;
    }

    public void clear() {
        synchronized (this.cache) {
            for (Bitmap bitmap : this.cache.values()) {
                bitmap.recycle();
            }
            this.cache.clear();
        }
    }
}
