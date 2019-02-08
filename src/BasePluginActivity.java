package mobi.maptrek;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.content.res.Resources.NotFoundException;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Pair;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.Map;
import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

public class BasePluginActivity extends Activity {
    private AbstractMap<String, Intent> mPluginPreferences = new HashMap();
    private AbstractMap<String, Pair<Drawable, Intent>> mPluginTools = new HashMap();

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
    }

    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    public Map<String, Intent> getPluginsPreferences() {
        return this.mPluginPreferences;
    }

    public Map<String, Pair<Drawable, Intent>> getPluginsTools() {
        return this.mPluginTools;
    }

    public void initializePlugins() {
        Exception e;
        PackageManager packageManager = getPackageManager();
        Intent initializationIntent = new Intent("mobi.maptrek.plugins.action.INITIALIZE");
        for (ResolveInfo plugin : packageManager.queryBroadcastReceivers(initializationIntent, 0)) {
            Intent intent = new Intent();
            intent.setClassName(plugin.activityInfo.packageName, plugin.activityInfo.name);
            intent.setAction(initializationIntent.getAction());
            intent.setFlags(32);
            sendBroadcast(intent);
        }
        for (ResolveInfo plugin2 : packageManager.queryIntentActivities(new Intent("mobi.maptrek.plugins.preferences"), 0)) {
            intent = new Intent();
            intent.setClassName(plugin2.activityInfo.packageName, plugin2.activityInfo.name);
            this.mPluginPreferences.put(plugin2.activityInfo.loadLabel(packageManager).toString(), intent);
        }
        for (ResolveInfo plugin22 : packageManager.queryIntentActivities(new Intent("mobi.maptrek.plugins.tool"), 0)) {
            Drawable icon = null;
            try {
                Resources resources = packageManager.getResourcesForApplication(plugin22.activityInfo.applicationInfo);
                int id = resources.getIdentifier("ic_menu_tool", "drawable", plugin22.activityInfo.packageName);
                if (id != 0) {
                    icon = resources.getDrawable(id, getTheme());
                }
            } catch (NotFoundException e2) {
                e = e2;
                e.printStackTrace();
                intent = new Intent();
                intent.setClassName(plugin22.activityInfo.packageName, plugin22.activityInfo.name);
                this.mPluginTools.put(plugin22.activityInfo.loadLabel(packageManager).toString(), new Pair(icon, intent));
            } catch (NameNotFoundException e3) {
                e = e3;
                e.printStackTrace();
                intent = new Intent();
                intent.setClassName(plugin22.activityInfo.packageName, plugin22.activityInfo.name);
                this.mPluginTools.put(plugin22.activityInfo.loadLabel(packageManager).toString(), new Pair(icon, intent));
            }
            intent = new Intent();
            intent.setClassName(plugin22.activityInfo.packageName, plugin22.activityInfo.name);
            this.mPluginTools.put(plugin22.activityInfo.loadLabel(packageManager).toString(), new Pair(icon, intent));
        }
    }

    @Subscribe
    public void onNewPluginEntry(Pair<String, Pair<Drawable, Intent>> entry) {
        this.mPluginTools.put(entry.first, entry.second);
    }
}
