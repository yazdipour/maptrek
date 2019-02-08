package mobi.maptrek.plugin;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.Pair;
import org.greenrobot.eventbus.EventBus;

public class PluginEntryReceiver extends BroadcastReceiver {
    public void onReceive(Context context, Intent intent) {
        Bundle extras = intent.getExtras();
        Intent pluginIntent = new Intent();
        pluginIntent.setClassName(extras.getString("packageName"), extras.getString("activityName"));
        EventBus.getDefault().post(new Pair(extras.getString("name"), new Pair(null, pluginIntent)));
    }
}
