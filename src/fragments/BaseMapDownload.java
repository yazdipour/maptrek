package mobi.maptrek.fragments;

import android.app.Fragment;
import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.text.format.Formatter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.TextView;
import mobi.maptrek.R;
import mobi.maptrek.maps.maptrek.Index;

public class BaseMapDownload extends Fragment implements OnBackPressedListener {
    private FragmentHolder mFragmentHolder;
    private Index mMapIndex;
    private TextView mMessageView;

    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_basemap_download, container, false);
        this.mMessageView = (TextView) rootView.findViewById(R.id.message);
        return rootView;
    }

    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        FloatingActionButton floatingButton = this.mFragmentHolder.enableActionButton();
        floatingButton.setImageDrawable(getContext().getDrawable(R.drawable.ic_file_download));
        floatingButton.setOnClickListener(new OnClickListener() {
            public void onClick(View v) {
                BaseMapDownload.this.mMapIndex.downloadBaseMap();
                BaseMapDownload.this.mFragmentHolder.disableActionButton();
                BaseMapDownload.this.mFragmentHolder.popCurrent();
            }
        });
    }

    public void onResume() {
        super.onResume();
        long size = this.mMapIndex != null ? this.mMapIndex.getBaseMapSize() : 42991616;
        this.mMessageView.setText(getString(R.string.msgBaseMapDownload, new Object[]{Formatter.formatFileSize(getContext(), size)}));
    }

    public void onAttach(Context context) {
        super.onAttach(context);
        try {
            this.mFragmentHolder = (FragmentHolder) context;
            this.mFragmentHolder.addBackClickListener(this);
        } catch (ClassCastException e) {
            throw new ClassCastException(context.toString() + " must implement FragmentHolder");
        }
    }

    public void onDetach() {
        super.onDetach();
        this.mFragmentHolder.removeBackClickListener(this);
        this.mFragmentHolder = null;
    }

    public boolean onBackClick() {
        this.mFragmentHolder.disableActionButton();
        return false;
    }

    public void setMapIndex(Index mapIndex) {
        this.mMapIndex = mapIndex;
    }
}
