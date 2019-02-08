package mobi.maptrek.maps;

import android.content.res.AssetManager;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.Set;
import mobi.maptrek.Configuration;
import mobi.maptrek.MapTrek;
import mobi.maptrek.R;
import mobi.maptrek.util.ByteArrayInOutStream;
import org.oscim.theme.ThemeFile;
import org.oscim.theme.XmlRenderThemeMenuCallback;
import org.oscim.theme.XmlRenderThemeStyleLayer;
import org.oscim.theme.XmlRenderThemeStyleMenu;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public enum Themes implements ThemeFile {
    MAPTREK("styles/maptrek.xml"),
    WINTER("styles/winter.xml"),
    NEWTRON("styles/newtron.xml");
    
    public static final float[] MAP_FONT_SIZES = null;
    private static final Logger logger = null;
    private final String mPath;

    static {
        logger = LoggerFactory.getLogger(Themes.class);
        MAP_FONT_SIZES = new float[]{0.3f, 0.5f, 0.7f, 0.9f, 1.1f};
    }

    private Themes(String path) {
        this.mPath = path;
    }

    public XmlRenderThemeMenuCallback getMenuCallback() {
        return new XmlRenderThemeMenuCallback() {
            public Set<String> getCategories(XmlRenderThemeStyleMenu renderThemeStyleMenu) {
                Object style = MapTrek.getApplication().getResources().getStringArray(R.array.mapStyleCodes)[Configuration.getMapStyle()];
                XmlRenderThemeStyleLayer renderThemeStyleLayer = renderThemeStyleMenu.getLayer(style);
                if (renderThemeStyleLayer == null) {
                    Themes.logger.error("Invalid style {}", style);
                    return null;
                }
                Set<String> categories = renderThemeStyleLayer.getCategories();
                for (XmlRenderThemeStyleLayer overlay : renderThemeStyleLayer.getOverlays()) {
                    if (overlay.isEnabled()) {
                        categories.addAll(overlay.getCategories());
                    }
                }
                switch (Configuration.getActivity()) {
                    case 1:
                        categories.add("hiking");
                        return categories;
                    default:
                        return categories;
                }
            }
        };
    }

    public String getRelativePathPrefix() {
        return "";
    }

    public InputStream getRenderThemeAsStream() {
        try {
            AssetManager assets = MapTrek.getApplication().getAssets();
            String dir = this.mPath.substring(0, this.mPath.indexOf(File.separatorChar) + 1);
            BufferedReader reader = new BufferedReader(new InputStreamReader(assets.open(this.mPath)));
            ByteArrayInOutStream ois = new ByteArrayInOutStream(1024);
            BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(ois));
            for (String line = reader.readLine(); line != null; line = reader.readLine()) {
                line = line.trim();
                if (line.startsWith("<xi:include")) {
                    int b = line.indexOf("href=");
                    int e = line.indexOf("\"", b + 6);
                    if (b > 0) {
                        Object src = dir + line.substring(b + 6, e);
                        logger.error("include: {}", src);
                        BufferedReader ibr = new BufferedReader(new InputStreamReader(assets.open(src)));
                        String il = ibr.readLine();
                        if (il != null) {
                            ibr.readLine();
                        }
                        if (il != null) {
                            il = ibr.readLine();
                        }
                        while (il != null) {
                            String nl = ibr.readLine();
                            if (nl != null) {
                                writer.write(il);
                                writer.newLine();
                            }
                            il = nl;
                        }
                        ibr.close();
                    }
                } else {
                    writer.write(line);
                    writer.newLine();
                }
            }
            reader.close();
            writer.close();
            return ois.getInputStream();
        } catch (Throwable e2) {
            logger.error(e2.getMessage(), e2);
            return null;
        }
    }

    public boolean isMapsforgeTheme() {
        return false;
    }

    public void setMenuCallback(XmlRenderThemeMenuCallback xmlRenderThemeMenuCallback) {
    }
}
