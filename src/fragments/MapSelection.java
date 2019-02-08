package mobi.maptrek.fragments;

import android.app.AlertDialog.Builder;
import android.app.Fragment;
import android.content.Context;
import android.content.DialogInterface;
import android.content.res.Resources;
import android.graphics.Rect;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewTreeObserver.OnGlobalLayoutListener;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.TextView;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import mobi.maptrek.Configuration;
import mobi.maptrek.R;
import mobi.maptrek.maps.maptrek.Index;
import mobi.maptrek.maps.maptrek.Index.ACTION;
import mobi.maptrek.maps.maptrek.Index.IndexStats;
import mobi.maptrek.maps.maptrek.Index.MapStateListener;
import mobi.maptrek.util.HelperUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MapSelection extends Fragment implements OnBackPressedListener, MapStateListener {
    private static final long HILLSHADE_CACHE_TIMEOUT = 5184000000L;
    private static final long INDEX_CACHE_EXPIRATION = 5184000000L;
    private static final long INDEX_CACHE_TIMEOUT = 86400000;
    private static final Logger logger = LoggerFactory.getLogger(MapSelection.class);
    private File mCacheFile;
    private int mCounter;
    private TextView mCounterView;
    private CheckBox mDownloadBasemap;
    private View mDownloadCheckboxHolder;
    private CheckBox mDownloadHillshades;
    private FloatingActionButton mFloatingButton;
    private FragmentHolder mFragmentHolder;
    private ImageButton mHelpButton;
    private File mHillshadeCacheFile;
    private View mHillshadesCheckboxHolder;
    private boolean mIsDownloadingIndex;
    private OnMapActionListener mListener;
    private Index mMapIndex;
    private TextView mMessageView;
    private Resources mResources;
    private TextView mStatusView;

    private class LoadMapIndex extends AsyncTask<Void, Integer, Boolean> {
        private int mDivider;
        private int mProgress;

        private LoadMapIndex() {
        }

        protected void onPreExecute() {
            MapSelection.this.mStatusView.setVisibility(0);
            MapSelection.this.mStatusView.setText(R.string.msgEstimateDownloadSize);
            this.mProgress = 0;
        }

        protected Boolean doInBackground(Void... params) {
            HttpURLConnection urlConnection;
            long now = System.currentTimeMillis();
            boolean validCache = MapSelection.this.mCacheFile.lastModified() + MapSelection.INDEX_CACHE_TIMEOUT > now;
            boolean validHillshadeCache = MapSelection.this.mHillshadeCacheFile.lastModified() + 5184000000L > now;
            this.mDivider = validHillshadeCache ? 1 : 2;
            boolean loaded = false;
            if (!validCache) {
                try {
                    urlConnection = null;
                    try {
                        urlConnection = (HttpURLConnection) new URL(Index.getIndexUri().toString() + "?" + MapSelection.this.mFragmentHolder.getStatsString()).openConnection();
                        InputStream in = urlConnection.getInputStream();
                        File tmpFile = new File(MapSelection.this.mCacheFile.getAbsoluteFile() + "_tmp");
                        loadMapIndex(in, new FileOutputStream(tmpFile));
                        loaded = tmpFile.renameTo(MapSelection.this.mCacheFile);
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                        }
                    } catch (Throwable e) {
                        MapSelection.logger.error("Failed to download map index", e);
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                        }
                    } catch (Throwable th) {
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                        }
                    }
                } catch (Throwable e2) {
                    MapSelection.logger.error("Failed to load map index", e2);
                    MapSelection.this.mCacheFile.delete();
                    return Boolean.valueOf(false);
                }
            }
            if (!loaded) {
                loadMapIndex(new FileInputStream(MapSelection.this.mCacheFile), null);
            }
            loaded = false;
            if (!validHillshadeCache) {
                try {
                    urlConnection = null;
                    try {
                        urlConnection = (HttpURLConnection) new URL(Index.getHillshadeIndexUri().toString()).openConnection();
                        in = urlConnection.getInputStream();
                        tmpFile = new File(MapSelection.this.mHillshadeCacheFile.getAbsoluteFile() + "_tmp");
                        loadHillshadesIndex(in, new FileOutputStream(MapSelection.this.mHillshadeCacheFile));
                        loaded = tmpFile.renameTo(MapSelection.this.mHillshadeCacheFile);
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                        }
                    } catch (Throwable e22) {
                        MapSelection.logger.error("Failed to download hillshades index", e22);
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                        }
                    } catch (Throwable th2) {
                        if (urlConnection != null) {
                            urlConnection.disconnect();
                        }
                    }
                } catch (Throwable e222) {
                    MapSelection.logger.error("Failed to load hillshades index", e222);
                    MapSelection.this.mHillshadeCacheFile.delete();
                    return Boolean.valueOf(false);
                }
            }
            if (!loaded) {
                loadHillshadesIndex(new FileInputStream(MapSelection.this.mHillshadeCacheFile), null);
            }
            return Boolean.valueOf(true);
        }

        private void loadMapIndex(InputStream in, OutputStream out) throws IOException {
            short date;
            int size;
            DataInputStream data = new DataInputStream(new BufferedInputStream(in));
            DataOutputStream dataOut = null;
            if (out != null) {
                dataOut = new DataOutputStream(new BufferedOutputStream(out));
            }
            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    date = data.readShort();
                    size = data.readInt();
                    if (dataOut != null) {
                        dataOut.writeShort(date);
                        dataOut.writeInt(size);
                    }
                    MapSelection.this.mMapIndex.setNativeMapStatus(x, y, date, (long) size);
                    int p = (int) ((((double) ((x * 128) + y)) / 163.84d) / ((double) this.mDivider));
                    if (p > this.mProgress) {
                        this.mProgress = p;
                        publishProgress(new Integer[]{Integer.valueOf(this.mProgress)});
                    }
                }
            }
            date = data.readShort();
            size = data.readInt();
            MapSelection.this.mMapIndex.setBaseMapStatus(date, size);
            if (dataOut != null) {
                dataOut.writeShort(date);
                dataOut.writeInt(size);
                dataOut.close();
            }
        }

        private void loadHillshadesIndex(InputStream in, OutputStream out) throws IOException {
            DataInputStream data = new DataInputStream(new BufferedInputStream(in));
            DataOutputStream dataOut = null;
            if (out != null) {
                dataOut = new DataOutputStream(new BufferedOutputStream(out));
            }
            for (int x = 0; x < 128; x++) {
                for (int y = 0; y < 128; y++) {
                    byte version = data.readByte();
                    int size = data.readInt();
                    if (dataOut != null) {
                        dataOut.writeByte(version);
                        dataOut.writeInt(size);
                    }
                    MapSelection.this.mMapIndex.setHillshadeStatus(x, y, version, (long) size);
                    int p = (int) ((((double) ((x * 128) + y)) / 163.84d) / ((double) this.mDivider));
                    if (p > this.mProgress) {
                        this.mProgress = p;
                        publishProgress(new Integer[]{Integer.valueOf(this.mProgress)});
                    }
                }
            }
            if (dataOut != null) {
                dataOut.close();
            }
        }

        protected void onPostExecute(Boolean result) {
            boolean expired = false;
            MapSelection.this.mIsDownloadingIndex = false;
            if (result.booleanValue()) {
                if (MapSelection.this.mCacheFile.lastModified() + 5184000000L < System.currentTimeMillis()) {
                    expired = true;
                }
                MapSelection.this.mMapIndex.setHasDownloadSizes(true, expired);
                MapSelection.this.updateUI(MapSelection.this.mMapIndex.getMapStats());
                return;
            }
            MapSelection.this.mStatusView.setText(R.string.msgIndexDownloadFailed);
        }

        protected void onProgressUpdate(Integer... values) {
            if (MapSelection.this.isVisible()) {
                MapSelection.this.mStatusView.setText(MapSelection.this.getString(R.string.msgEstimateDownloadSizePlaceholder, new Object[]{MapSelection.this.getString(R.string.msgEstimateDownloadSize), values[0]}));
            }
        }
    }

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);
    }

    public void onResume() {
        super.onResume();
        updateUI(this.mMapIndex.getMapStats());
        if (!this.mMapIndex.hasDownloadSizes() && this.mCacheFile.exists()) {
            this.mIsDownloadingIndex = true;
            new LoadMapIndex().execute(new Void[0]);
        }
    }

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        final View rootView = inflater.inflate(R.layout.fragment_map_selection, container, false);
        this.mHillshadesCheckboxHolder = rootView.findViewById(R.id.hillshadesCheckboxHolder);
        this.mDownloadHillshades = (CheckBox) rootView.findViewById(R.id.downloadHillshades);
        this.mDownloadHillshades.setChecked(Configuration.getHillshadesEnabled());
        this.mDownloadHillshades.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MapSelection.this.mMapIndex.accountHillshades(isChecked);
                MapSelection.this.updateUI(MapSelection.this.mMapIndex.getMapStats());
            }
        });
        this.mDownloadCheckboxHolder = rootView.findViewById(R.id.downloadCheckboxHolder);
        this.mDownloadBasemap = (CheckBox) rootView.findViewById(R.id.downloadBasemap);
        this.mDownloadBasemap.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                MapSelection.this.updateUI(MapSelection.this.mMapIndex.getMapStats());
            }
        });
        this.mMessageView = (TextView) rootView.findViewById(R.id.message);
        this.mMessageView.setText(this.mResources.getQuantityString(R.plurals.itemsSelected, 0, new Object[]{Integer.valueOf(0)}));
        this.mStatusView = (TextView) rootView.findViewById(R.id.status);
        this.mCounterView = (TextView) rootView.findViewById(R.id.count);
        this.mHelpButton = (ImageButton) rootView.findViewById(R.id.helpButton);
        this.mHelpButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                Builder builder = new Builder(MapSelection.this.getActivity());
                builder.setMessage(R.string.msgMapSelectionExplanation);
                builder.setPositiveButton(R.string.ok, null);
                builder.create().show();
            }
        });
        if (HelperUtils.needsTargetedAdvice(32768)) {
            rootView.getViewTreeObserver().addOnGlobalLayoutListener(new OnGlobalLayoutListener() {
                public void onGlobalLayout() {
                    rootView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                    if (MapSelection.this.mMapIndex.getMapDatabaseSize() > 4194304) {
                        Rect r = new Rect();
                        MapSelection.this.mCounterView.getGlobalVisibleRect(r);
                        r.left = r.right - (r.width() / 3);
                        HelperUtils.showTargetedAdvice(MapSelection.this.getActivity(), 32768, R.string.advice_active_maps_size, r);
                    }
                }
            });
        }
        return rootView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        this.mListener.onBeginMapManagement();
        this.mFloatingButton = this.mFragmentHolder.enableActionButton();
        this.mFloatingButton.setImageResource(R.drawable.ic_file_download);
        this.mFloatingButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                if (MapSelection.this.mDownloadBasemap.isChecked()) {
                    MapSelection.this.mMapIndex.downloadBaseMap();
                }
                if (MapSelection.this.mCounter > 0) {
                    MapSelection.this.mListener.onManageNativeMaps(MapSelection.this.mDownloadHillshades.isChecked());
                }
                if (MapSelection.this.mDownloadBasemap.isChecked() || MapSelection.this.mCounter > 0) {
                    MapSelection.this.mListener.onFinishMapManagement();
                }
                MapSelection.this.mFragmentHolder.disableActionButton();
                MapSelection.this.mFragmentHolder.popCurrent();
            }
        });
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mListener = (OnMapActionListener) context;
            try {
                this.mFragmentHolder = (FragmentHolder) context;
                this.mFragmentHolder.addBackClickListener(this);
                this.mResources = getResources();
                File cacheDir = context.getExternalCacheDir();
                this.mCacheFile = new File(cacheDir, "mapIndex");
                this.mHillshadeCacheFile = new File(cacheDir, "hillshadeIndex");
                this.mMapIndex.addMapStateListener(this);
            } catch (ClassCastException e) {
                throw new ClassCastException(context.toString() + " must implement FragmentHolder");
            }
        } catch (ClassCastException e2) {
            throw new ClassCastException(context.toString() + " must implement OnMapActionListener");
        }
    }

    public void onDetach() {
        super.onDetach();
        this.mMapIndex.removeMapStateListener(this);
        this.mFragmentHolder.removeBackClickListener(this);
        this.mFragmentHolder = null;
        this.mListener = null;
        this.mResources = null;
    }

    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
    }

    public boolean onBackClick() {
        this.mFragmentHolder.disableActionButton();
        this.mListener.onFinishMapManagement();
        return false;
    }

    public void onMapSelected(final int x, final int y, ACTION action, IndexStats stats) {
        if (action == ACTION.CANCEL) {
            Builder builder = new Builder(getActivity());
            builder.setTitle(R.string.msgCancelDownload);
            builder.setPositiveButton(R.string.yes, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    MapSelection.this.mMapIndex.cancelDownload(x, y);
                }
            });
            builder.setNegativeButton(R.string.no, new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface dialog, int which) {
                    dialog.dismiss();
                }
            });
            builder.create().show();
            return;
        }
        updateUI(stats);
        if (action == ACTION.DOWNLOAD && !this.mMapIndex.hasDownloadSizes() && !this.mIsDownloadingIndex) {
            this.mIsDownloadingIndex = true;
            new LoadMapIndex().execute(new Void[0]);
        }
    }

    public void setMapIndex(Index mapIndex) {
        this.mMapIndex = mapIndex;
        this.mMapIndex.accountHillshades(Configuration.getHillshadesEnabled());
    }

    private void updateUI(IndexStats stats) {
        if (isVisible()) {
            if (this.mMapIndex.isBaseMapOutdated()) {
                this.mDownloadBasemap.setText(getString(R.string.downloadBasemap, new Object[]{Formatter.formatFileSize(getContext(), this.mMapIndex.getBaseMapSize())}));
                this.mDownloadCheckboxHolder.setVisibility(0);
            }
            this.mCounter = stats.download + stats.remove;
            this.mMessageView.setText(this.mResources.getQuantityString(R.plurals.itemsSelected, this.mCounter, new Object[]{Integer.valueOf(this.mCounter)}));
            if (this.mFloatingButton != null) {
                if (this.mDownloadBasemap.isChecked() || stats.download > 0) {
                    this.mFloatingButton.setImageResource(R.drawable.ic_file_download);
                    this.mFloatingButton.setVisibility(0);
                    this.mHelpButton.setVisibility(4);
                    this.mHillshadesCheckboxHolder.setVisibility(0);
                } else if (stats.remove > 0) {
                    this.mFloatingButton.setImageResource(R.drawable.ic_delete);
                    this.mFloatingButton.setVisibility(0);
                    this.mHelpButton.setVisibility(4);
                    this.mHillshadesCheckboxHolder.setVisibility(8);
                } else {
                    this.mFloatingButton.setVisibility(8);
                    this.mHelpButton.setVisibility(0);
                    this.mHillshadesCheckboxHolder.setVisibility(8);
                }
            }
            if (stats.downloadSize > 0) {
                this.mStatusView.setVisibility(0);
                this.mStatusView.setText(getString(R.string.msgDownloadSize, new Object[]{Formatter.formatFileSize(getContext(), stats.downloadSize)}));
            } else if (!this.mIsDownloadingIndex) {
                this.mStatusView.setVisibility(8);
            }
            StringBuilder stringBuilder = new StringBuilder();
            if (stats.loaded > 0) {
                stringBuilder.append(this.mResources.getQuantityString(R.plurals.loadedAreas, stats.loaded, new Object[]{Integer.valueOf(stats.loaded)}));
                stringBuilder.append(" (");
                stringBuilder.append(Formatter.formatFileSize(getContext(), this.mMapIndex.getMapDatabaseSize()));
                stringBuilder.append(")");
            }
            if (stats.downloading > 0) {
                if (stringBuilder.length() > 0) {
                    stringBuilder.append(", ");
                }
                stringBuilder.append(this.mResources.getQuantityString(R.plurals.downloading, stats.downloading, new Object[]{Integer.valueOf(stats.downloading)}));
            }
            if (stringBuilder.length() > 0) {
                this.mCounterView.setVisibility(0);
                this.mCounterView.setText(stringBuilder);
                return;
            }
            this.mCounterView.setVisibility(8);
        }
    }

    public void onHasDownloadSizes() {
    }

    public void onStatsChanged() {
        getActivity().runOnUiThread(new Runnable() {
            public void run() {
                MapSelection.this.updateUI(MapSelection.this.mMapIndex.getMapStats());
            }
        });
    }

    public void onHillshadeAccountingChanged(boolean account) {
    }
}
