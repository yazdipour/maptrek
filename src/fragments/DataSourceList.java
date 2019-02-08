package mobi.maptrek.fragments;

import android.app.Activity;
import android.app.ListFragment;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.PopupMenu.OnMenuItemClickListener;
import android.widget.TextView;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import mobi.maptrek.DataHolder;
import mobi.maptrek.R;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.source.DataSource;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.source.TrackDataSource;
import mobi.maptrek.data.source.WaypointDataSource;
import mobi.maptrek.data.source.WaypointDbDataSource;
import mobi.maptrek.util.StringFormatter;

public class DataSourceList extends ListFragment {
    public static final String ARG_NATIVE_TRACKS = "nativeTracks";
    private DataSourceListAdapter mAdapter;
    private List<DataSource> mData = new ArrayList();
    private DataHolder mDataHolder;
    private boolean mNativeTracks;
    private OnTrackActionListener mTrackActionListener;

    private class DataSourceListAdapter extends BaseAdapter {
        private int mAccentColor;
        private int mDisabledColor;
        private LayoutInflater mInflater;

        DataSourceListAdapter(Context context) {
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            Activity activity = DataSourceList.this.getActivity();
            this.mAccentColor = activity.getColor(R.color.colorAccentLight);
            this.mDisabledColor = activity.getColor(R.color.colorPrimary);
        }

        public DataSource getItem(int position) {
            return (DataSource) DataSourceList.this.mData.get(position);
        }

        public long getItemId(int position) {
            return (long) ((DataSource) DataSourceList.this.mData.get(position)).hashCode();
        }

        public int getCount() {
            return DataSourceList.this.mData.size();
        }

        public boolean isEnabled(int position) {
            return getItem(position).isLoaded();
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            DataSourceListItemHolder itemHolder;
            DataSource dataSource = getItem(position);
            if (convertView == null) {
                itemHolder = new DataSourceListItemHolder();
                convertView = this.mInflater.inflate(R.layout.list_item_data_source, parent, false);
                itemHolder.name = (TextView) convertView.findViewById(R.id.name);
                itemHolder.description = (TextView) convertView.findViewById(R.id.description);
                itemHolder.filename = (TextView) convertView.findViewById(R.id.filename);
                itemHolder.icon = (ImageView) convertView.findViewById(R.id.icon);
                itemHolder.action = (ImageView) convertView.findViewById(R.id.action);
                convertView.setTag(itemHolder);
            } else {
                itemHolder = (DataSourceListItemHolder) convertView.getTag();
            }
            itemHolder.name.setText(dataSource.name);
            Resources resources = DataSourceList.this.getResources();
            int color = this.mAccentColor;
            if (dataSource instanceof WaypointDbDataSource) {
                int count = ((WaypointDataSource) dataSource).getWaypointsCount();
                itemHolder.description.setText(resources.getQuantityString(R.plurals.waypointsCount, count, new Object[]{Integer.valueOf(count)}));
                itemHolder.filename.setText("");
                itemHolder.icon.setImageResource(R.drawable.ic_points);
                itemHolder.action.setVisibility(8);
                itemHolder.action.setOnClickListener(null);
            } else {
                File file = new File(((FileDataSource) dataSource).path);
                itemHolder.filename.setText(file.getName());
                if (!dataSource.isLoaded()) {
                    itemHolder.description.setText(Formatter.formatShortFileSize(DataSourceList.this.getContext(), file.length()));
                    if (DataSourceList.this.mNativeTracks) {
                        itemHolder.icon.setImageResource(R.drawable.ic_track);
                    } else {
                        itemHolder.icon.setImageResource(R.drawable.ic_dataset);
                    }
                    color = this.mDisabledColor;
                } else if (DataSourceList.this.mNativeTracks) {
                    Track track = (Track) ((FileDataSource) dataSource).tracks.get(0);
                    itemHolder.description.setText(StringFormatter.distanceH((double) track.getDistance()));
                    itemHolder.icon.setImageResource(R.drawable.ic_track);
                    color = track.style.color;
                } else {
                    int waypointsCount = ((FileDataSource) dataSource).waypoints.size();
                    int tracksCount = ((FileDataSource) dataSource).tracks.size();
                    StringBuilder sb = new StringBuilder();
                    if (waypointsCount > 0) {
                        sb.append(resources.getQuantityString(R.plurals.waypointsCount, waypointsCount, new Object[]{Integer.valueOf(waypointsCount)}));
                        if (tracksCount > 0) {
                            sb.append(", ");
                        }
                    }
                    if (tracksCount > 0) {
                        sb.append(resources.getQuantityString(R.plurals.tracksCount, tracksCount, new Object[]{Integer.valueOf(tracksCount)}));
                    }
                    itemHolder.description.setText(sb);
                    if (waypointsCount > 0 && tracksCount > 0) {
                        itemHolder.icon.setImageResource(R.drawable.ic_dataset);
                    } else if (waypointsCount > 0) {
                        itemHolder.icon.setImageResource(R.drawable.ic_points);
                    } else if (tracksCount > 0) {
                        itemHolder.icon.setImageResource(R.drawable.ic_tracks);
                    }
                }
                final boolean shown = dataSource.isVisible();
                if (shown) {
                    itemHolder.action.setImageResource(R.drawable.ic_visibility);
                } else {
                    itemHolder.action.setImageResource(R.drawable.ic_visibility_off);
                }
                itemHolder.action.setVisibility(0);
                final int i = position;
                itemHolder.action.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        DataSourceList.this.mDataHolder.setDataSourceAvailability((FileDataSource) DataSourceListAdapter.this.getItem(i), !shown);
                        DataSourceListAdapter.this.notifyDataSetChanged();
                    }
                });
            }
            Drawable background = itemHolder.icon.getBackground().mutate();
            if (background instanceof ShapeDrawable) {
                ((ShapeDrawable) background).getPaint().setColor(color);
            } else if (background instanceof GradientDrawable) {
                ((GradientDrawable) background).setColor(color);
            }
            return convertView;
        }

        public boolean hasStableIds() {
            return true;
        }
    }

    private static class DataSourceListItemHolder {
        ImageView action;
        TextView description;
        TextView filename;
        ImageView icon;
        TextView name;

        private DataSourceListItemHolder() {
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.list_with_empty_view, container, false);
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mNativeTracks = getArguments().getBoolean(ARG_NATIVE_TRACKS);
        if (this.mNativeTracks) {
            TextView emptyView = (TextView) getListView().getEmptyView();
            if (emptyView != null) {
                emptyView.setText(R.string.msgEmptyTrackList);
            }
        }
        this.mAdapter = new DataSourceListAdapter(getActivity());
        setListAdapter(this.mAdapter);
        getListView().setOnItemLongClickListener(new OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> adapterView, View view, int position, long id) {
                final DataSource dataSource = DataSourceList.this.mAdapter.getItem(position);
                PopupMenu popup = new PopupMenu(DataSourceList.this.getContext(), view);
                popup.inflate(R.menu.context_menu_data_list);
                if (dataSource instanceof WaypointDbDataSource) {
                    popup.getMenu().findItem(R.id.action_delete).setVisible(false);
                }
                popup.setOnMenuItemClickListener(new OnMenuItemClickListener() {
                    public boolean onMenuItemClick(MenuItem item) {
                        switch (item.getItemId()) {
                            case R.id.action_delete:
                                DataSourceList.this.mDataHolder.onDataSourceDelete(dataSource);
                                return true;
                            case R.id.action_share:
                                DataSourceList.this.mDataHolder.onDataSourceShare(dataSource);
                                return true;
                            default:
                                return false;
                        }
                    }
                });
                popup.show();
                return true;
            }
        });
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mDataHolder = (DataHolder) context;
            try {
                this.mTrackActionListener = (OnTrackActionListener) context;
            } catch (ClassCastException e) {
                throw new ClassCastException(context.toString() + " must implement OnTrackActionListener");
            }
        } catch (ClassCastException e2) {
            throw new ClassCastException(context.toString() + " must implement DataHolder");
        }
    }

    public void onDetach() {
        super.onDetach();
        this.mData.clear();
        this.mDataHolder = null;
        this.mTrackActionListener = null;
    }

    public void onResume() {
        super.onResume();
        updateData();
    }

    public void onListItemClick(ListView lv, View v, int position, long id) {
        DataSource source = this.mAdapter.getItem(position);
        if (source.isNativeTrack()) {
            this.mTrackActionListener.onTrackDetails((Track) ((TrackDataSource) source).getTracks().get(0));
            return;
        }
        this.mDataHolder.onDataSourceSelected(source);
    }

    public void updateData() {
        this.mData.clear();
        if (!this.mNativeTracks) {
            this.mData.add(this.mDataHolder.getWaypointDataSource());
        }
        List<FileDataSource> data = this.mDataHolder.getData();
        Collections.sort(data, new Comparator<FileDataSource>() {
            public int compare(FileDataSource lhs, FileDataSource rhs) {
                if (!DataSourceList.this.mNativeTracks) {
                    return lhs.name.compareTo(rhs.name);
                }
                return Long.compare(new File(rhs.path).lastModified(), new File(lhs.path).lastModified());
            }
        });
        for (FileDataSource source : data) {
            if (((!source.isNativeTrack() ? 1 : 0) ^ this.mNativeTracks) != 0) {
                this.mData.add(source);
            }
        }
        this.mAdapter.notifyDataSetChanged();
    }
}
