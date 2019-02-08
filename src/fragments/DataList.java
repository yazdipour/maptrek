package mobi.maptrek.fragments;

import android.app.ListFragment;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Rect;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.os.Bundle;
import android.support.annotation.ColorInt;
import android.support.design.widget.FloatingActionButton;
import android.util.SparseBooleanArray;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.AbsListView.MultiChoiceModeListener;
import android.widget.CursorAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import com.fasterxml.jackson.core.util.MinimalPrettyPrinter;
import java.util.HashSet;
import mobi.maptrek.Configuration;
import mobi.maptrek.DataHolder;
import mobi.maptrek.R;
import mobi.maptrek.data.Track;
import mobi.maptrek.data.Waypoint;
import mobi.maptrek.data.source.DataSource;
import mobi.maptrek.data.source.DataSource.DataType;
import mobi.maptrek.data.source.DataSourceUpdateListener;
import mobi.maptrek.data.source.MemoryDataSource;
import mobi.maptrek.data.source.TrackDataSource;
import mobi.maptrek.data.source.WaypointDataSource;
import mobi.maptrek.data.source.WaypointDbDataSource;
import mobi.maptrek.fragments.CoordinatesInputDialog.Builder;
import mobi.maptrek.fragments.CoordinatesInputDialog.CoordinatesInputDialogCallback;
import mobi.maptrek.util.HelperUtils;
import mobi.maptrek.util.JosmCoordinatesParser;
import mobi.maptrek.util.JosmCoordinatesParser.Result;
import mobi.maptrek.util.StringFormatter;
import org.oscim.core.GeoPoint;

public class DataList extends ListFragment implements DataSourceUpdateListener, CoordinatesInputDialogCallback {
    public static final String ARG_HEIGHT = "hgt";
    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lon";
    public static final String ARG_NO_EXTRA_SOURCES = "msg";
    private DataListAdapter mAdapter;
    private GeoPoint mCoordinates;
    private DataHolder mDataHolder;
    private DataSource mDataSource;
    private FloatingActionButton mFloatingButton;
    private FragmentHolder mFragmentHolder;
    private boolean mIsMultiDataSource;
    private String mLineSeparator = System.getProperty("line.separator");
    private MultiChoiceModeListener mMultiChoiceModeListener = new MultiChoiceModeListener() {
        public void onItemCheckedStateChanged(ActionMode mode, int position, long id, boolean checked) {
            ListView listView = DataList.this.getListView();
            int count = listView.getCheckedItemCount();
            mode.setTitle(DataList.this.getResources().getQuantityString(R.plurals.itemsSelected, count, new Object[]{Integer.valueOf(count)}));
            int start = listView.getFirstVisiblePosition();
            int j = listView.getLastVisiblePosition();
            for (int i = start; i <= j; i++) {
                if (position == i) {
                    listView.getAdapter().getView(i, listView.getChildAt(i - start), listView);
                    return;
                }
            }
        }

        public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
            switch (item.getItemId()) {
                case R.id.action_delete:
                    DataList.this.deleteSelectedItems();
                    mode.finish();
                    return true;
                case R.id.action_share:
                    DataList.this.shareSelectedItems();
                    mode.finish();
                    return true;
                default:
                    return false;
            }
        }

        public boolean onCreateActionMode(ActionMode mode, Menu menu) {
            mode.getMenuInflater().inflate(R.menu.context_menu_waypoint_list, menu);
            if (DataList.this.mFloatingButton != null) {
                DataList.this.mFloatingButton.setVisibility(8);
            }
            return true;
        }

        public void onDestroyActionMode(ActionMode mode) {
            if (DataList.this.mFloatingButton != null) {
                DataList.this.mFloatingButton.setVisibility(0);
            }
        }

        public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
            return false;
        }
    };
    private OnTrackActionListener mTrackActionListener;
    private OnWaypointActionListener mWaypointActionListener;

    private class DataListAdapter extends CursorAdapter {
        private static final int STATE_REGULAR_CELL = 2;
        private static final int STATE_SECTIONED_CELL = 1;
        private static final int STATE_UNKNOWN = 0;
        @ColorInt
        private int mAccentColor;
        private int[] mCellStates;
        private LayoutInflater mInflater;

        DataListAdapter(Context context, Cursor cursor, int flags) {
            super(context, cursor, flags);
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
            this.mAccentColor = DataList.this.getResources().getColor(R.color.colorAccentLight, context.getTheme());
            this.mCellStates = cursor == null ? null : new int[cursor.getCount()];
        }

        public void changeCursor(Cursor cursor) {
            super.changeCursor(cursor);
            this.mCellStates = cursor == null ? null : new int[cursor.getCount()];
        }

        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            int viewType = getItemViewType(cursor.getPosition());
            View view = null;
            if (viewType == 0) {
                view = this.mInflater.inflate(R.layout.list_item_waypoint, parent, false);
            } else if (viewType == 1) {
                view = this.mInflater.inflate(R.layout.list_item_track, parent, false);
            }
            if (view != null) {
                ItemHolder holder = new ItemHolder();
                holder.separator = (TextView) view.findViewById(R.id.separator);
                holder.name = (TextView) view.findViewById(R.id.name);
                holder.distance = (TextView) view.findViewById(R.id.distance);
                holder.icon = (ImageView) view.findViewById(R.id.icon);
                holder.viewButton = (ImageView) view.findViewById(R.id.view);
                view.setTag(holder);
            }
            return view;
        }

        public void bindView(View view, Context context, Cursor cursor) {
            int position = cursor.getPosition();
            int viewType = getItemViewType(position);
            ItemHolder holder = (ItemHolder) view.getTag();
            boolean needSeparator = false;
            switch (this.mCellStates[position]) {
                case 1:
                    needSeparator = true;
                    break;
                case 2:
                    needSeparator = false;
                    break;
                default:
                    if (DataList.this.mIsMultiDataSource && position == 0) {
                        needSeparator = true;
                    } else if (DataList.this.mIsMultiDataSource && getItemViewType(position - 1) != viewType) {
                        needSeparator = true;
                    }
                    this.mCellStates[position] = needSeparator ? 1 : 2;
                    break;
            }
            if (needSeparator) {
                holder.separator.setText(DataList.this.getText(viewType == 0 ? R.string.waypoints : R.string.tracks));
                holder.separator.setVisibility(0);
            } else {
                holder.separator.setVisibility(8);
            }
            boolean isChecked = DataList.this.getListView().isItemChecked(position);
            boolean hasChecked = DataList.this.getListView().getCheckedItemCount() > 0;
            int icon = R.drawable.ic_info_outline;
            int color = R.color.colorPrimaryDark;
            String distance;
            if (viewType == 0) {
                Waypoint waypoint = ((WaypointDataSource) DataList.this.mDataSource).cursorToWaypoint(cursor);
                distance = StringFormatter.distanceH(DataList.this.mCoordinates.vincentyDistance(waypoint.coordinates)) + MinimalPrettyPrinter.DEFAULT_ROOT_VALUE_SEPARATOR + StringFormatter.angleH(DataList.this.mCoordinates.bearingTo(waypoint.coordinates));
                holder.name.setText(waypoint.name);
                holder.distance.setText(distance);
                final Waypoint waypoint2 = waypoint;
                holder.viewButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        DataList.this.mWaypointActionListener.onWaypointView(waypoint2);
                        DataList.this.mFragmentHolder.disableListActionButton();
                        DataList.this.mFragmentHolder.popAll();
                    }
                });
                icon = R.drawable.ic_point;
                color = waypoint.style.color;
            } else if (viewType == 1) {
                Track track = ((TrackDataSource) DataList.this.mDataSource).cursorToTrack(cursor);
                distance = StringFormatter.distanceH((double) track.getDistance());
                holder.name.setText(track.name);
                holder.distance.setText(distance);
                final Track track2 = track;
                holder.viewButton.setOnClickListener(new OnClickListener() {
                    public void onClick(View v) {
                        DataList.this.mTrackActionListener.onTrackView(track2);
                        DataList.this.mFragmentHolder.disableListActionButton();
                        DataList.this.mFragmentHolder.popAll();
                    }
                });
                icon = R.drawable.ic_track;
                color = track.style.color;
            }
            if (hasChecked) {
                holder.viewButton.setVisibility(8);
            } else {
                holder.viewButton.setVisibility(0);
            }
            if (isChecked) {
                icon = R.drawable.ic_done;
                color = this.mAccentColor;
            }
            holder.icon.setImageResource(icon);
            Drawable background = holder.icon.getBackground().mutate();
            if (background instanceof ShapeDrawable) {
                ((ShapeDrawable) background).getPaint().setColor(color);
            } else if (background instanceof GradientDrawable) {
                ((GradientDrawable) background).setColor(color);
            }
        }

        @DataType
        public int getItemViewType(int position) {
            return DataList.this.mDataSource.getDataType(position);
        }

        public int getViewTypeCount() {
            return 2;
        }
    }

    private static class ItemHolder {
        TextView distance;
        ImageView icon;
        TextView name;
        TextView separator;
        ImageView viewButton;

        private ItemHolder() {
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.list_with_empty_view, container, false);
        if (HelperUtils.needsTargetedAdvice(512)) {
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                public void onGlobalLayout() {
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if (DataList.this.mAdapter.getCount() > 0) {
                        View view = DataList.this.getListView().getChildAt(0).findViewById(R.id.view);
                        Rect r = new Rect();
                        view.getGlobalVisibleRect(r);
                        HelperUtils.showTargetedAdvice(DataList.this.getActivity(), 512, R.string.advice_view_data_item, r);
                    }
                }
            });
        }
        return rootView;
    }

    public void onDestroy() {
        super.onDestroy();
        this.mFloatingButton = null;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle arguments = getArguments();
        double latitude = arguments.getDouble("lat");
        double longitude = arguments.getDouble("lon");
        boolean noExtraSources = arguments.getBoolean("msg");
        int minHeight = arguments.getInt(ARG_HEIGHT, 0);
        if (savedInstanceState != null) {
            latitude = savedInstanceState.getDouble("lat");
            longitude = savedInstanceState.getDouble("lon");
        }
        this.mCoordinates = new GeoPoint(latitude, longitude);
        TextView emptyView = (TextView) getListView().getEmptyView();
        if (emptyView != null) {
            StringBuilder stringBuilder = new StringBuilder();
            stringBuilder.append(getString(R.string.msgEmptyWaypointList));
            if (noExtraSources) {
                stringBuilder.append(this.mLineSeparator);
                stringBuilder.append(this.mLineSeparator);
                stringBuilder.append(getString(R.string.msgNoFileDataSources));
            }
            emptyView.setText(stringBuilder.toString());
        }
        this.mAdapter = new DataListAdapter(getActivity(), this.mDataSource.getCursor(), 0);
        setListAdapter(this.mAdapter);
        ListView listView = getListView();
        listView.setChoiceMode(3);
        listView.setMultiChoiceModeListener(this.mMultiChoiceModeListener);
        View rootView = getView();
        if (rootView != null && minHeight > 0) {
            rootView.setMinimumHeight(minHeight);
        }
        if (noExtraSources) {
            listView.addFooterView(((LayoutInflater) getActivity().getSystemService("layout_inflater")).inflate(R.layout.list_footer_data_source, listView, false), null, false);
        }
        if (this.mDataSource instanceof WaypointDbDataSource) {
            this.mFloatingButton = this.mFragmentHolder.enableListActionButton();
            this.mFloatingButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_add_location));
            this.mFloatingButton.setOnClickListener(new OnClickListener() {
                public void onClick(View v) {
                    new Builder().setCallbacks(DataList.this).setTitle(DataList.this.getString(R.string.titleCoordinatesInput)).create().show(DataList.this.getFragmentManager(), "pointCoordinatesInput");
                }
            });
        }
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mWaypointActionListener = (OnWaypointActionListener) context;
            try {
                this.mTrackActionListener = (OnTrackActionListener) context;
                try {
                    this.mDataHolder = (DataHolder) context;
                    this.mFragmentHolder = (FragmentHolder) context;
                } catch (ClassCastException e) {
                    throw new ClassCastException(context.toString() + " must implement DataHolder");
                }
            } catch (ClassCastException e2) {
                throw new ClassCastException(context.toString() + " must implement OnTrackActionListener");
            }
        } catch (ClassCastException e3) {
            throw new ClassCastException(context.toString() + " must implement OnWaypointActionListener");
        }
    }

    public void onDetach() {
        super.onDetach();
        this.mDataSource.removeListener(this);
        this.mWaypointActionListener = null;
        this.mTrackActionListener = null;
        this.mFragmentHolder.disableListActionButton();
        this.mFragmentHolder = null;
        this.mDataHolder = null;
    }

    public void onResume() {
        super.onResume();
        CoordinatesInputDialog coordinatesInput = (CoordinatesInputDialog) getFragmentManager().findFragmentByTag("pointCoordinatesInput");
        if (coordinatesInput != null) {
            coordinatesInput.setCallback(this);
        }
    }

    public void onListItemClick(ListView lv, View v, int position, long id) {
        Cursor cursor = (Cursor) this.mAdapter.getItem(position);
        int itemType = this.mDataSource.getDataType(position);
        if (itemType == 0) {
            this.mWaypointActionListener.onWaypointDetails(((WaypointDataSource) this.mDataSource).cursorToWaypoint(cursor), true);
        } else if (itemType == 1) {
            this.mTrackActionListener.onTrackDetails(((TrackDataSource) this.mDataSource).cursorToTrack(cursor));
        }
    }

    public void setDataSource(DataSource dataSource) {
        this.mDataSource = dataSource;
        boolean z = (this.mDataSource instanceof WaypointDataSource) && (this.mDataSource instanceof TrackDataSource) && ((WaypointDataSource) this.mDataSource).getWaypointsCount() > 0 && ((TrackDataSource) this.mDataSource).getTracksCount() > 0;
        this.mIsMultiDataSource = z;
        this.mDataSource.addListener(this);
    }

    public void onDataSourceUpdated() {
        if (this.mAdapter != null) {
            this.mAdapter.changeCursor(this.mDataSource.getCursor());
            boolean z = (this.mDataSource instanceof WaypointDataSource) && (this.mDataSource instanceof TrackDataSource) && ((WaypointDataSource) this.mDataSource).getWaypointsCount() > 0 && ((TrackDataSource) this.mDataSource).getTracksCount() > 0;
            this.mIsMultiDataSource = z;
        }
    }

    private void shareSelectedItems() {
        HashSet<Waypoint> waypoints = new HashSet();
        HashSet<Track> tracks = new HashSet();
        populateSelectedItems(waypoints, tracks);
        MemoryDataSource dataSource = new MemoryDataSource();
        dataSource.waypoints.addAll(waypoints);
        dataSource.tracks.addAll(tracks);
        this.mDataHolder.onDataSourceShare(dataSource);
    }

    private void deleteSelectedItems() {
        HashSet<Waypoint> waypoints = new HashSet();
        HashSet<Track> tracks = new HashSet();
        populateSelectedItems(waypoints, tracks);
        if (waypoints.size() > 0) {
            this.mWaypointActionListener.onWaypointsDelete(waypoints);
        }
        if (tracks.size() > 0) {
            this.mTrackActionListener.onTracksDelete(tracks);
        }
    }

    private void populateSelectedItems(HashSet<Waypoint> waypoints, HashSet<Track> tracks) {
        SparseBooleanArray positions = getListView().getCheckedItemPositions();
        for (int position = 0; position < this.mAdapter.getCount(); position++) {
            if (positions.get(position)) {
                Cursor cursor = (Cursor) this.mAdapter.getItem(position);
                int type = this.mDataSource.getDataType(position);
                if (type == 0) {
                    waypoints.add(((WaypointDataSource) this.mDataSource).cursorToWaypoint(cursor));
                } else if (type == 1) {
                    tracks.add(((TrackDataSource) this.mDataSource).cursorToTrack(cursor));
                }
            }
        }
    }

    public void onTextInputPositiveClick(String id, String inputText) {
        boolean errors = false;
        for (String line : inputText.split(this.mLineSeparator)) {
            if (line.length() != 0) {
                try {
                    Result result = JosmCoordinatesParser.parseWithResult(line);
                    String name = null;
                    if (result.offset < line.length()) {
                        name = line.substring(result.offset, line.length()).trim();
                    }
                    if (name == null || "".equals(name)) {
                        name = getString(R.string.waypoint_name, new Object[]{Integer.valueOf(Configuration.getPointsCounter())});
                    }
                    this.mWaypointActionListener.onWaypointCreate(result.coordinates, name, true, false);
                } catch (IllegalArgumentException e) {
                    errors = true;
                }
            }
        }
        if (errors) {
            HelperUtils.showError(getString(R.string.msgParseMultipleCoordinatesFailed), this.mFragmentHolder.getCoordinatorLayout());
        }
    }

    public void onTextInputNegativeClick(String id) {
    }
}
