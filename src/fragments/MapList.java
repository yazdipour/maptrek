package mobi.maptrek.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.SeekBar.OnSeekBarChangeListener;
import android.widget.Switch;
import android.widget.TextView;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import mobi.maptrek.R;
import mobi.maptrek.maps.MapFile;
import mobi.maptrek.view.BitmapTileMapPreviewView;
import org.oscim.core.GeoPoint;
import org.oscim.tiling.source.sqlite.SQLiteTileSource;

public class MapList extends Fragment {
    public static final String ARG_HIDE_OBJECTS = "hide";
    public static final String ARG_LATITUDE = "lat";
    public static final String ARG_LONGITUDE = "lon";
    public static final String ARG_TRANSPARENCY = "transparency";
    public static final String ARG_ZOOM_LEVEL = "zoom";
    private MapFile mActiveMap;
    private TextView mEmptyView;
    private FragmentHolder mFragmentHolder;
    private Switch mHideSwitch;
    private LayoutInflater mInflater;
    private OnMapActionListener mListener;
    private GeoPoint mLocation;
    private LinearLayout mMapList;
    private View mMapListHeader;
    private ArrayList<MapFile> mMaps = new ArrayList();
    private SeekBar mTransparencySeekBar;
    private int mZoomLevel;

    private class MapComparator implements Comparator<MapFile>, Serializable {
        private MapComparator() {
        }

        public int compare(MapFile o1, MapFile o2) {
            return o1.name.compareTo(o2.name);
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        ViewGroup rootView = (ViewGroup) inflater.inflate(R.layout.fragment_map_list, container, false);
        this.mEmptyView = (TextView) rootView.findViewById(16908292);
        this.mMapListHeader = rootView.findViewById(R.id.mapListHeader);
        this.mHideSwitch = (Switch) rootView.findViewById(R.id.hideSwitch);
        this.mTransparencySeekBar = (SeekBar) rootView.findViewById(R.id.transparencySeekBar);
        this.mMapList = (LinearLayout) rootView.findViewById(R.id.mapList);
        this.mHideSwitch.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MapList.this.mListener.onHideMapObjects(isChecked);
            }
        });
        this.mTransparencySeekBar.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (fromUser) {
                    MapList.this.mListener.onTransparencyChanged(progress);
                }
            }

            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        return rootView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        Bundle arguments = getArguments();
        double latitude = arguments.getDouble("lat");
        double longitude = arguments.getDouble("lon");
        this.mZoomLevel = arguments.getInt("zoom");
        boolean hideObjects = arguments.getBoolean(ARG_HIDE_OBJECTS);
        int transparency = arguments.getInt(ARG_TRANSPARENCY);
        this.mLocation = new GeoPoint(latitude, longitude);
        this.mMapList.removeAllViews();
        if (this.mMaps.size() == 0) {
            this.mEmptyView.setVisibility(0);
            this.mMapListHeader.setVisibility(8);
        } else {
            this.mEmptyView.setVisibility(8);
            this.mMapListHeader.setVisibility(0);
            Iterator it = this.mMaps.iterator();
            while (it.hasNext()) {
                addMap((MapFile) it.next());
            }
        }
        this.mHideSwitch.setChecked(hideObjects);
        this.mTransparencySeekBar.setProgress(transparency);
    }

    public void onDestroyView() {
        super.onDestroyView();
        this.mMapList.removeAllViews();
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mListener = (OnMapActionListener) context;
            this.mFragmentHolder = (FragmentHolder) context;
            this.mInflater = (LayoutInflater) context.getSystemService("layout_inflater");
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement OnMapActionListener");
        }
    }

    public void onDetach() {
        super.onDetach();
        this.mFragmentHolder = null;
        this.mListener = null;
    }

    public void onDestroy() {
        super.onDestroy();
        this.mMaps.clear();
    }

    public void setMaps(Collection<MapFile> maps, MapFile active) {
        this.mMaps.clear();
        for (MapFile map : maps) {
            this.mMaps.add(map);
        }
        this.mActiveMap = active;
        Collections.sort(this.mMaps, new MapComparator());
    }

    private void addMap(final MapFile mapFile) {
        View mapView = this.mInflater.inflate(R.layout.list_item_map, this.mMapList, false);
        TextView name = (TextView) mapView.findViewById(R.id.name);
        final BitmapTileMapPreviewView map = (BitmapTileMapPreviewView) mapView.findViewById(R.id.map);
        View indicator = mapView.findViewById(R.id.indicator);
        name.setText(mapFile.name);
        map.setTileSource(mapFile.tileSource, this.mActiveMap == mapFile);
        if (mapFile.boundingBox.contains(this.mLocation)) {
            int zoomLevel = mapFile.tileSource.getZoomLevelMax();
            int minZoomLevel = mapFile.tileSource.getZoomLevelMin();
            if (mapFile.tileSource instanceof SQLiteTileSource) {
                minZoomLevel = ((SQLiteTileSource) mapFile.tileSource).sourceZoomMin;
            }
            if (mapFile.tileSource.getOption("path") == null || (this.mZoomLevel < zoomLevel && this.mZoomLevel > minZoomLevel)) {
                zoomLevel = this.mZoomLevel;
            }
            map.setLocation(this.mLocation, zoomLevel);
        } else {
            map.setLocation(mapFile.boundingBox.getCenterPoint(), mapFile.tileSource.getZoomLevelMax());
        }
        if (mapFile == this.mActiveMap) {
            indicator.setBackgroundColor(getResources().getColor(R.color.colorAccent, getContext().getTheme()));
        } else {
            indicator.setBackgroundColor(0);
        }
        mapView.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                map.setShouldNotCloseDataSource();
                MapList.this.mListener.onMapSelected(mapFile);
                MapList.this.mFragmentHolder.popCurrent();
            }
        });
        this.mMapList.addView(mapView);
    }
}
