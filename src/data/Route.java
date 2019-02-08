package mobi.maptrek.data;

import java.util.ArrayList;
import java.util.List;
import mobi.maptrek.util.Geo;

public class Route {
    public String description;
    public double distance;
    private Waypoint lastWaypoint;
    public String name;
    public boolean removed;
    public boolean show;
    private final ArrayList<Waypoint> waypoints;
    public int width;

    public Route() {
        this("", "", false);
    }

    public Route(String name, String description, boolean show) {
        this.removed = false;
        this.waypoints = new ArrayList();
        this.name = name;
        this.description = description;
        this.show = show;
        this.distance = 0.0d;
    }

    public List<Waypoint> getWaypoints() {
        return this.waypoints;
    }

    public void addWaypoint(Waypoint waypoint) {
        if (this.lastWaypoint != null) {
            this.distance += this.lastWaypoint.coordinates.vincentyDistance(waypoint.coordinates);
        }
        this.lastWaypoint = waypoint;
        this.waypoints.add(this.lastWaypoint);
    }

    public void addWaypoint(int pos, Waypoint waypoint) {
        this.waypoints.add(pos, waypoint);
        this.lastWaypoint = (Waypoint) this.waypoints.get(this.waypoints.size() - 1);
        this.distance = distanceBetween(0, this.waypoints.size() - 1);
    }

    public Waypoint addWaypoint(String name, double lat, double lon) {
        Waypoint waypoint = new Waypoint(name, lat, lon);
        addWaypoint(waypoint);
        return waypoint;
    }

    public void insertWaypoint(Waypoint waypoint) {
        if (this.waypoints.size() < 2) {
            addWaypoint(waypoint);
            return;
        }
        int after = this.waypoints.size() - 1;
        double xtk = Double.MAX_VALUE;
        synchronized (this.waypoints) {
            for (int i = 0; i < this.waypoints.size() - 1; i++) {
                double distance = waypoint.coordinates.vincentyDistance(((Waypoint) this.waypoints.get(i + 1)).coordinates);
                double cxtk1 = Math.abs(Geo.xtk(distance, ((Waypoint) this.waypoints.get(i)).coordinates.bearingTo(((Waypoint) this.waypoints.get(i + 1)).coordinates), waypoint.coordinates.bearingTo(((Waypoint) this.waypoints.get(i + 1)).coordinates)));
                double bearing2 = waypoint.coordinates.bearingTo(((Waypoint) this.waypoints.get(i)).coordinates);
                if (Math.abs(Geo.xtk(distance, ((Waypoint) this.waypoints.get(i + 1)).coordinates.bearingTo(((Waypoint) this.waypoints.get(i)).coordinates), bearing2)) != Double.POSITIVE_INFINITY && cxtk1 < xtk) {
                    xtk = cxtk1;
                    after = i;
                }
            }
        }
        this.waypoints.add(after + 1, waypoint);
        this.lastWaypoint = (Waypoint) this.waypoints.get(this.waypoints.size() - 1);
        this.distance = distanceBetween(0, this.waypoints.size() - 1);
    }

    public Waypoint insertWaypoint(String name, double lat, double lon) {
        Waypoint waypoint = new Waypoint(name, lat, lon);
        insertWaypoint(waypoint);
        return waypoint;
    }

    public void insertWaypoint(int after, Waypoint waypoint) {
        this.waypoints.add(after + 1, waypoint);
        this.lastWaypoint = (Waypoint) this.waypoints.get(this.waypoints.size() - 1);
        this.distance = distanceBetween(0, this.waypoints.size() - 1);
    }

    public Waypoint insertWaypoint(int after, String name, double lat, double lon) {
        Waypoint waypoint = new Waypoint(name, lat, lon);
        insertWaypoint(after, waypoint);
        return waypoint;
    }

    public void removeWaypoint(Waypoint waypoint) {
        this.waypoints.remove(waypoint);
        if (this.waypoints.size() > 0) {
            this.lastWaypoint = (Waypoint) this.waypoints.get(this.waypoints.size() - 1);
            this.distance = distanceBetween(0, this.waypoints.size() - 1);
        }
    }

    public Waypoint getWaypoint(int index) throws IndexOutOfBoundsException {
        return (Waypoint) this.waypoints.get(index);
    }

    public int length() {
        return this.waypoints.size();
    }

    public void clear() {
        synchronized (this.waypoints) {
            this.waypoints.clear();
        }
        this.lastWaypoint = null;
        this.distance = 0.0d;
    }

    public double distanceBetween(int first, int last) {
        double dist = 0.0d;
        synchronized (this.waypoints) {
            for (int i = first; i < last; i++) {
                dist += ((Waypoint) this.waypoints.get(i)).coordinates.vincentyDistance(((Waypoint) this.waypoints.get(i + 1)).coordinates);
            }
        }
        return dist;
    }

    public double course(int prev, int next) {
        double bearingTo;
        synchronized (this.waypoints) {
            bearingTo = ((Waypoint) this.waypoints.get(prev)).coordinates.bearingTo(((Waypoint) this.waypoints.get(next)).coordinates);
        }
        return bearingTo;
    }
}
