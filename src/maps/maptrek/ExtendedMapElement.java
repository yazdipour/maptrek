package mobi.maptrek.maps.maptrek;

import org.oscim.core.MapElement;

public class ExtendedMapElement extends MapElement {
    public int buildingColor = 0;
    public int buildingHeight = 0;
    public int buildingMinHeight = 0;
    public MapTrekDataSource database;
    public int elevation = 0;
    boolean hasLabelPosition = true;
    public long id = 0;
    boolean isBuilding = false;
    boolean isBuildingPart = false;
    boolean isContour = false;
    public int kind = 0;
    public int roofColor = 0;

    void clearData() {
        this.id = 0;
        this.layer = 5;
        this.kind = 0;
        this.hasLabelPosition = true;
        this.labelPosition = null;
        this.database = null;
        this.elevation = 0;
        this.buildingHeight = 0;
        this.buildingMinHeight = 0;
        this.buildingColor = 0;
        this.roofColor = 0;
        this.isContour = false;
        this.isBuilding = false;
        this.isBuildingPart = false;
    }
}
