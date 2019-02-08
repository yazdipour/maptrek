package mobi.maptrek.location;

public interface ILocationListener {
    void onGpsStatusChanged();

    void onLocationChanged();
}
