package mobi.maptrek.maps.online;

import android.content.ContentProviderClient;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.RemoteException;
import org.oscim.core.Tile;
import org.oscim.tiling.TileSource.OpenResult;
import org.oscim.tiling.source.OkHttpEngine.OkHttpFactory;
import org.oscim.tiling.source.bitmap.BitmapTileSource;

public class OnlineTileSource extends BitmapTileSource {
    public static final String[] TILE_COLUMNS = new String[]{"TILE"};
    public static final String TILE_TYPE = "vnd.android.cursor.item/vnd.mobi.maptrek.maps.online.provider.tile";
    private final String mCode;
    private final Context mContext;
    private final String mLicense;
    private final String mName;
    private ContentProviderClient mProviderClient;
    private final int mThreads;
    private final String mUri;

    public static class Builder<T extends Builder<T>> extends org.oscim.tiling.source.bitmap.BitmapTileSource.Builder<T> {
        protected String code;
        private Context context;
        protected String license;
        protected String name;
        protected int threads;
        protected String uri;

        protected Builder(Context context) {
            this.context = context;
            this.url = "http://maptrek.mobi/";
            httpFactory(new OkHttpFactory());
        }

        public OnlineTileSource build() {
            return new OnlineTileSource(this);
        }

        public T name(String name) {
            this.name = name;
            return (Builder) self();
        }

        public T code(String code) {
            this.code = code;
            return (Builder) self();
        }

        public T uri(String uri) {
            this.uri = uri;
            return (Builder) self();
        }

        public T license(String license) {
            this.license = license;
            return (Builder) self();
        }

        public T threads(int threads) {
            this.threads = threads;
            return (Builder) self();
        }
    }

    public static Builder<?> builder(Context context) {
        return new Builder(context);
    }

    protected OnlineTileSource(Builder<?> builder) {
        super(builder);
        this.mContext = builder.context;
        this.mName = builder.name;
        this.mCode = builder.code;
        this.mUri = builder.uri;
        this.mLicense = builder.license;
        this.mThreads = builder.threads;
    }

    public OpenResult open() {
        this.mProviderClient = this.mContext.getContentResolver().acquireContentProviderClient(Uri.parse(this.mUri));
        if (this.mProviderClient != null) {
            return OpenResult.SUCCESS;
        }
        return new OpenResult("Failed to get provider for uri: " + this.mUri);
    }

    public void close() {
        if (this.mProviderClient != null) {
            this.mProviderClient.release();
            this.mProviderClient = null;
        }
    }

    public String getTileUrl(Tile tile) {
        String tileUrl = null;
        if (this.mProviderClient == null) {
            return null;
        }
        try {
            Cursor cursor = this.mProviderClient.query(Uri.parse(this.mUri + "/" + tile.zoomLevel + "/" + tile.tileX + "/" + tile.tileY), TILE_COLUMNS, null, null, null);
            if (cursor != null) {
                cursor.moveToFirst();
                tileUrl = cursor.getString(0);
                cursor.close();
            }
        } catch (RemoteException e) {
            e.printStackTrace();
        }
        return tileUrl;
    }

    public String getName() {
        return this.mName;
    }
}
