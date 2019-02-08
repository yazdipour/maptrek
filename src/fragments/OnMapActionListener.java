package mobi.maptrek.fragments;

import mobi.maptrek.maps.MapFile;

public interface OnMapActionListener {
    void onBeginMapManagement();

    void onFinishMapManagement();

    void onHideMapObjects(boolean z);

    void onManageNativeMaps(boolean z);

    void onMapSelected(MapFile mapFile);

    void onTransparencyChanged(int i);
}
