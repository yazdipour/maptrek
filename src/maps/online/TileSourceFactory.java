package mobi.maptrek.maps.online;

import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.support.annotation.NonNull;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import mobi.maptrek.maps.online.OnlineTileSource.Builder;
import org.oscim.android.cache.TileCache;

public class TileSourceFactory {
    @NonNull
    public static List<OnlineTileSource> fromPlugin(Context context, PackageManager packageManager, ResolveInfo provider) {
        List<OnlineTileSource> sources = new ArrayList();
        String[] maps = null;
        try {
            Resources resources = packageManager.getResourcesForApplication(provider.activityInfo.applicationInfo);
            int id = resources.getIdentifier("maps", "array", provider.activityInfo.packageName);
            if (id != 0) {
                maps = resources.getStringArray(id);
            }
            if (maps != null) {
                File cacheDir = new File(context.getExternalCacheDir(), "online");
                boolean useCache = cacheDir.mkdir() || cacheDir.isDirectory();
                for (String map : maps) {
                    String name = null;
                    String uri = null;
                    id = resources.getIdentifier(map + "_name", "string", provider.activityInfo.packageName);
                    if (id != 0) {
                        name = resources.getString(id);
                    }
                    id = resources.getIdentifier(map + "_uri", "string", provider.activityInfo.packageName);
                    if (id != 0) {
                        uri = resources.getString(id);
                    }
                    if (!(name == null || uri == null)) {
                        Builder builder = OnlineTileSource.builder(context);
                        builder.name(name);
                        builder.code(map);
                        builder.uri(uri);
                        id = resources.getIdentifier(map + "_license", "string", provider.activityInfo.packageName);
                        if (id != 0) {
                            builder.license(resources.getString(id));
                        }
                        id = resources.getIdentifier(map + "_threads", "integer", provider.activityInfo.packageName);
                        if (id != 0) {
                            builder.threads(resources.getInteger(id));
                        }
                        id = resources.getIdentifier(map + "_minzoom", "integer", provider.activityInfo.packageName);
                        if (id != 0) {
                            builder.zoomMin(resources.getInteger(id));
                        }
                        id = resources.getIdentifier(map + "_maxzoom", "integer", provider.activityInfo.packageName);
                        if (id != 0) {
                            builder.zoomMax(resources.getInteger(id));
                        }
                        OnlineTileSource source = builder.build();
                        if (useCache) {
                            source.setCache(new TileCache(context, cacheDir.getAbsolutePath(), map));
                        }
                        sources.add(source);
                    }
                }
            }
        } catch (NotFoundException e) {
            Exception e2 = e;
            e2.printStackTrace();
            return sources;
        } catch (NameNotFoundException e3) {
            e2 = e3;
            e2.printStackTrace();
            return sources;
        }
        return sources;
    }
}
