package mobi.maptrek.fragments;

import android.app.Activity;
import android.app.AlertDialog.Builder;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import java.util.Locale;
import mobi.maptrek.DataHolder;
import mobi.maptrek.R;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.MemoryDataSource;
import mobi.maptrek.util.StringFormatter;
import org.oscim.utils.Osm;

public class LocationShareDialog extends DialogFragment implements OnClickListener {
    public static final String ARG_LATITUDE = "latitude";
    public static final String ARG_LONGITUDE = "longitude";
    public static final String ARG_NAME = "name";
    public static final String ARG_ZOOM = "zoom";
    private final Item[] items = new Item[]{new Item("Copy to clipboard", Integer.valueOf(R.drawable.ic_content_copy)), new Item("Share as text", Integer.valueOf(R.drawable.ic_share)), new Item("Open in map app", Integer.valueOf(R.drawable.ic_launch)), new Item("Share as file", Integer.valueOf(R.drawable.ic_description))};
    private DataHolder mDataHolder;

    public static class Item {
        public final int icon;
        public final String text;

        Item(String text, Integer icon) {
            this.text = text;
            this.icon = icon.intValue();
        }

        public String toString() {
            return this.text;
        }
    }

    static class LocationShareAdapter extends ArrayAdapter<Item> {
        private final float mDensity;

        LocationShareAdapter(@NonNull Context context, int resource, int textViewResourceId, @NonNull Item[] objects) {
            super(context, resource, textViewResourceId, objects);
            this.mDensity = context.getResources().getDisplayMetrics().density;
        }

        @NonNull
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            View v = super.getView(position, convertView, parent);
            TextView tv = (TextView) v.findViewById(16908308);
            Item item = (Item) getItem(position);
            if (item != null) {
                Drawable icon = getContext().getDrawable(item.icon);
                if (icon != null) {
                    icon.setTint(getContext().getColor(R.color.colorPrimaryDark));
                    tv.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null);
                    tv.setCompoundDrawablePadding((int) (16.0f * this.mDensity));
                }
            }
            return v;
        }
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mDataHolder = (DataHolder) context;
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement DataHolder");
        }
    }

    public void onDetach() {
        super.onDetach();
        this.mDataHolder = null;
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        Activity activity = getActivity();
        return new Builder(activity).setAdapter(new LocationShareAdapter(activity, 17367057, 16908308, this.items), this).create();
    }

    public void onClick(DialogInterface dialog, int which) {
        Bundle args = getArguments();
        double latitude = args.getDouble("latitude");
        double longitude = args.getDouble("longitude");
        int zoom = args.getInt("zoom", 14);
        String name = args.getString("name", null);
        switch (which) {
            case 0:
                ClipData clip = ClipData.newPlainText(getString(R.string.coordinates), StringFormatter.coordinates(MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR, latitude, longitude));
                ClipboardManager clipboard = (ClipboardManager) getContext().getSystemService("clipboard");
                if (clipboard != null) {
                    clipboard.setPrimaryClip(clip);
                    return;
                }
                return;
            case 1:
                StringBuilder location = new StringBuilder();
                location.append(String.format(Locale.US, "%.6f %.6f", new Object[]{Double.valueOf(latitude), Double.valueOf(longitude)}));
                if (name != null) {
                    location.append(MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR).append(name);
                }
                location.append(" <").append(Osm.makeShortLink(latitude, longitude, zoom)).append(">");
                Intent intent = new Intent("android.intent.action.SEND");
                intent.setType("text/plain");
                intent.putExtra("android.intent.extra.TEXT", location.toString());
                startActivity(Intent.createChooser(intent, getString(R.string.share_location_intent_title)));
                return;
            case 2:
                Intent mapIntent = new Intent("android.intent.action.VIEW", Uri.parse(String.format(Locale.getDefault(), "geo:%f,%f?z=%d", new Object[]{Double.valueOf(latitude), Double.valueOf(longitude), Integer.valueOf(zoom)})));
                if (getContext().getPackageManager().queryIntentActivities(mapIntent, 65536).size() > 0) {
                    startActivity(mapIntent);
                    return;
                }
                return;
            case 3:
                MemoryDataSource dataSource = new MemoryDataSource();
                dataSource.name = name;
                if (name == null) {
                    name = StringFormatter.coordinates(MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR, latitude, longitude);
                }
                dataSource.waypoints.add(new Waypoint(name, latitude, longitude));
                this.mDataHolder.onDataSourceShare(dataSource);
                return;
            default:
                return;
        }
    }
}
