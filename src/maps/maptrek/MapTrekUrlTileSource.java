package mobi.maptrek.maps.maptrek;

import org.oscim.tiling.ITileDataSource;
import org.oscim.tiling.source.UrlTileDataSource;
import org.oscim.tiling.source.UrlTileSource;

public class MapTrekUrlTileSource extends UrlTileSource {
    private static final String DEFAULT_PATH = "/{Z}/{X}/{Y}.mvt";
    private static final String DEFAULT_URL = "http://maptrek.mobi:3579/all";

    public static class Builder<T extends Builder<T>> extends org.oscim.tiling.source.UrlTileSource.Builder<T> {
        public Builder() {
            super(MapTrekUrlTileSource.DEFAULT_URL, MapTrekUrlTileSource.DEFAULT_PATH, 8, 17);
        }

        public MapTrekUrlTileSource build() {
            return new MapTrekUrlTileSource(this);
        }
    }

    public static Builder<?> builder() {
        return new Builder();
    }

    protected MapTrekUrlTileSource(Builder<?> builder) {
        super(builder);
    }

    public MapTrekUrlTileSource() {
        this(builder());
    }

    public MapTrekUrlTileSource(String urlString) {
        this((Builder) builder().url(urlString));
    }

    public ITileDataSource getDataSource() {
        return new UrlTileDataSource(this, new MapTrekTileDecoder(), getHttpEngine());
    }
}
