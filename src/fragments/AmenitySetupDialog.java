package mobi.maptrek.fragments;

import android.app.Activity;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.res.Resources;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import mobi.maptrek.R;
import mobi.maptrek.maps.maptrek.Tags;

public class AmenitySetupDialog extends DialogFragment {
    private AmenitySetupDialogCallback mCallback;

    public interface AmenitySetupDialogCallback {
        void onAmenityKindVisibilityChanged();
    }

    private class AmenitySetupListAdapter extends BaseAdapter {
        private Context mContext;
        private LayoutInflater mInflater;

        AmenitySetupListAdapter(Context context) {
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mContext = context;
        }

        public Pair<String, Integer> getItem(int position) {
            Resources resources = this.mContext.getResources();
            int id = resources.getIdentifier(Tags.kinds[position], "string", AmenitySetupDialog.this.getActivity().getPackageName());
            return new Pair(id != 0 ? resources.getString(id) : Tags.kinds[position], Integer.valueOf(Tags.kindZooms[position] - 14));
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public int getCount() {
            return Tags.kinds.length;
        }

        public View getView(final int position, View convertView, ViewGroup parent) {
            AmenitySetupListItemHolder itemHolder;
            Pair<String, Integer> group = getItem(position);
            if (convertView == null) {
                itemHolder = new AmenitySetupListItemHolder();
                convertView = this.mInflater.inflate(R.layout.list_item_amenity_setup, parent, false);
                itemHolder.name = (TextView) convertView.findViewById(R.id.name);
                itemHolder.zoom = (Spinner) convertView.findViewById(R.id.zoom);
                convertView.setTag(itemHolder);
            } else {
                itemHolder = (AmenitySetupListItemHolder) convertView.getTag();
            }
            itemHolder.name.setText((CharSequence) group.first);
            ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(this.mContext, R.array.zooms_array, 17367048);
            adapter.setDropDownViewResource(17367049);
            itemHolder.zoom.setAdapter(adapter);
            itemHolder.zoom.setSelection(((Integer) group.second).intValue());
            itemHolder.zoom.setOnItemSelectedListener(new OnItemSelectedListener() {
                public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long id) {
                    boolean changed = Tags.kindZooms[position] != pos + 14;
                    Tags.kindZooms[position] = pos + 14;
                    if (changed && AmenitySetupDialog.this.mCallback != null) {
                        AmenitySetupDialog.this.mCallback.onAmenityKindVisibilityChanged();
                    }
                }

                public void onNothingSelected(AdapterView<?> adapterView) {
                }
            });
            return convertView;
        }

        public boolean hasStableIds() {
            return true;
        }
    }

    private static class AmenitySetupListItemHolder {
        TextView name;
        Spinner zoom;

        private AmenitySetupListItemHolder() {
        }
    }

    public static class Builder {
        private AmenitySetupDialogCallback mCallback;
        private String mTitle;

        public Builder setTitle(String title) {
            this.mTitle = title;
            return this;
        }

        public Builder setCallback(AmenitySetupDialogCallback callback) {
            this.mCallback = callback;
            return this;
        }

        public AmenitySetupDialog create() {
            AmenitySetupDialog dialogFragment = new AmenitySetupDialog();
            Bundle args = new Bundle();
            if (this.mTitle != null) {
                args.putString("title", this.mTitle);
            }
            dialogFragment.setCallback(this.mCallback);
            dialogFragment.setArguments(args);
            return dialogFragment;
        }
    }

    public Dialog onCreateDialog(Bundle savedInstanceState) {
        String title = getArguments().getString("title", null);
        Activity activity = getActivity();
        View dialogView = activity.getLayoutInflater().inflate(R.layout.dialog_list, null);
        ((ListView) dialogView.findViewById(16908298)).setAdapter(new AmenitySetupListAdapter(getActivity()));
        android.app.AlertDialog.Builder dialogBuilder = new android.app.AlertDialog.Builder(activity);
        if (title != null) {
            dialogBuilder.setTitle(title);
        }
        dialogBuilder.setPositiveButton(R.string.ok, new OnClickListener() {
            public void onClick(DialogInterface dialog, int which) {
            }
        });
        dialogBuilder.setView(dialogView);
        return dialogBuilder.create();
    }

    public void setCallback(AmenitySetupDialogCallback callback) {
        this.mCallback = callback;
    }
}
