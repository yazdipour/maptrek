package mobi.maptrek;

import android.support.annotation.NonNull;
import java.util.List;
import mobi.maptrek.data.source.DataSource;
import mobi.maptrek.data.source.FileDataSource;
import mobi.maptrek.data.source.WaypointDbDataSource;

public interface DataHolder {
    @NonNull
    List<FileDataSource> getData();

    @NonNull
    WaypointDbDataSource getWaypointDataSource();

    void onDataSourceDelete(@NonNull DataSource dataSource);

    void onDataSourceSelected(@NonNull DataSource dataSource);

    void onDataSourceShare(@NonNull DataSource dataSource);

    void setDataSourceAvailability(FileDataSource fileDataSource, boolean z);
}
