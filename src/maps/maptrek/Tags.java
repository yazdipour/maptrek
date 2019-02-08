package mobi.maptrek.maps.maptrek;

import android.support.annotation.NonNull;
import android.support.v4.os.EnvironmentCompat;
import org.oscim.core.Tag;

public class Tags {
    static final int ATTRIB_OFFSET = 1024;
    static final int MAX_KEY = (keys.length - 1);
    public static final int MAX_VALUE = (values.length - 1);
    static final String[] keys = new String[]{Tag.KEY_BUILDING, Tag.KEY_HIGHWAY, "natural", Tag.KEY_LANDUSE, "waterway", "power", Tag.KEY_AMENITY, "oneway", Tag.KEY_REF, "barrier", "tracktype", "access", "place", "railway", "bridge", "leisure", "service", "tourism", "boundary", "tunnel", "religion", "shop", "man_made", Tag.KEY_AREA, Tag.KEY_BUILDING_PART, "population", "fee", "generator:source", "historic", "aeroway", "admin_level", "piste:type", "piste:difficulty", "ford", "emergency", "aerialway", "mountain_pass", "capital", "route", "icao", "iata", "station", "contour", "construction", "cutting", "embankment", "intermittent", "lock", "sport", "surface", "toll", "tower:type", "wetland", "maritime", "winter_road", "ice_road", "4wd_only", "sac_scale", "trail_visibility", "osmc:symbol", "network", "route:network", "information"};
    public static final int[] kindZooms = new int[]{15, 16, 16, 14, 16, 17, 17, 16, 18, 18, 18, 16, 17, 17, 17, 16};
    public static final String[] kinds = new String[]{"kind_emergency", "kind_accommodation", "kind_food", "kind_attraction", "kind_entertainment", "kind_shopping", "kind_service", "kind_religion", "kind_education", "kind_kids", "kind_pets", "kind_vehicles", "kind_transportation", "kind_hikebike", "kind_urban", "kind_barrier"};
    public static final String[] values = new String[]{"elevation_major", "elevation_medium", "elevation_minor", "reserved", "reserved", "reserved", "reserved", "reserved", "reserved", "reserved", Tag.VALUE_YES, "residential", "house", "service", "track", "stream", "unclassified", "tower", "tree", "1", "water", "wood", "footway", "path", "tertiary", "private", "farmland", "secondary", "garage", "parking_aisle", "fence", "parking", "grass", "apartments", "meadow", "primary", "wall", "bus_stop", "rail", "scrub", "industrial", "wetland", "administrative", "grade3", "ditch", "grade2", "hut", "locality", "school", "gate", "pitch", "village", "hamlet", "river", "detached", "grade4", "traffic_signals", "cycleway", "hedge", "place_of_worship", Tag.KEY_ROOF, "living_street", "shed", "grade1", "trunk", "commercial", "steps", "restaurant", "motorway", "grade5", "christian", "park", "drain", "farmyard", Tag.VALUE_NO, "level_crossing", "motorway_link", "orchard", "pedestrian", "peak", "cemetery", "retail", "line", "construction", "information", "garages", "terrace", "reservoir", "garden", "playground", "grassland", "fuel", "generator", "cliff", "hotel", "supermarket", "riverbank", "vineyard", "road", "trunk_link", "cafe", "canal", "greenhouse", "primary_link", "fast_food", "pier", "bollard", "isolated_dwelling", "bank", "post_box", "farm_auxiliary", "pharmacy", "kindergarten", "church", "secondary_link", "barn", "abandoned", "allotments", "wind", "hospital", "lift_gate", "heath", "toilets", "shelter", "basin", "cutline", "island", "6", "sports_centre", "drinking_water", "memorial", "post_office", "warehouse", "pub", "dam", "hairdresser", "taxiway", "retaining_wall", "tertiary_link", "attraction", "quarry", "bakery", "neighbourhood", "atm", "bar", "sand", "muslim", "car_repair", "viewpoint", "suburb", "cabin", "university", "beach", "ruins", "farm", "picnic_site", "police", "telephone", "civic", "town", "platform", "manufacture", "station", "solar", "crossing", "guest_house", "-1", "fire_station", "disused", "spring", "village_green", "camp_site", "tram", "scree", "car", "fountain", "doctors", "nordic", "bridleway", "office", "brownfield", "artwork", "library", "museum", "cycle_barrier", "downhill", "collapsed", "static_caravan", "public", "protected_area", "4", "recreation_ground", "common", "college", "block", "monument", "greenhouse_horticulture", "nature_reserve", "tram_stop", "hangar", "runway", "doityourself", "bus_station", "phone", "subway", "mall", "stadium", "aerodrome", "easy", "glacier", "chapel", "castle", "toll_booth", "helipad", "motel", "buddhist", "narrow_gauge", "military", "semidetached_house", "golf_course", "halt", "hostel", "intermediate", "bungalow", "entrance", "bridge", "theatre", "slipway", "cave_entrance", "bicycle_rental", "apron", "city_wall", "veterinary", "5", "light_rail", "subway_entrance", "2", "landfill", "cinema", "caravan_site", "dormitory", "train_station", "storage_tank", "transportation", "city", "mosque", "damaged", "stable", "pet", "national_park", "plant_nursery", "semi", "shingle", "ferry", "hindu", "trullo", "0", "8", "transformer_tower", "alpine_hut", "houseboat", "bunker", "drag_lift", "advanced", "shinto", "preserved", "marsh", "mud", "water_park", "oil", "lighthouse", "shop", "chair_lift", "hydro", "bureau_de_change", "chain", "slurry_tank", "windmill", "jewish", "terminal", "novice", "pajaru", "cowshed", "dog_park", "zoo", "silo", "residence", "miniature", "duplex", "factory", "temple", "border_control", "dock", "6", "volcano", "4", "agricultural", "gas", "7", "2", "tank", "kiosk", "5", "region", "3", "carport", "dwelling_house", "wilderness_hut", "3", "pavilion", "chalet", "allotment_house", "store", "coal", "support", "10", "monorail", "ruin", "summer_cottage", "boathouse", "grandstand", "mobile_home", "9", "outbuilding", "anexo", "sled", "glasshouse", "storage", "state", "taoist", "elevator", "wayside_shrine", "cathedral", "beach_hut", "residences", "bangunan", Tag.KEY_BUILDING, "semi-detached", "biomass", "expert", "education", "column", "magic_carpet", "home", "tent", "other", "flats", "veranda", "apartment", "government", "government_office", "gondola", "cable_car", "shrine", "funicular", "sport", "proposed", "biogas", "hall", "clinic", "utility", "monastery", "community_group_office", "prison", "gazebo", "houses", "destroyed", "container", "offices", "base", "canopy", "manor", "part", "conservatory", "sikh", "freeride", "townhouse", "verdieping", "window", "nuclear", "summer_house", "general", "substation", EnvironmentCompat.MEDIA_UNKNOWN, "semi_detached", "bell_tower", "diesel", "healthcare", "depot", "basilica", "power_substation", "clubhouse", Tag.VALUE_FLAT, "foundation", "biofuel", "electricity", "synagogue", "room", "unit", "marketplace", "interval", "power", "some", "balcony", "gasometer", "villa", "village_office", "photovoltaic", "1", "family_house", "public_building", "prefab_container", "convent", "chimney", "storage_tank", "multifaith", "hay_barn", "electricity_network", "digester", "voodoo", "business", "building_concrete", "wayside_chapel", "generic_building", "arbour", "barne", "airport", "brewery", "condominium", "floor", "technical", "toilet", "data_center", "stables", "undefined", "railway_station", "train", "works", "prefabricated", "trailer_park", "porch", "farmhouse", "country", "historic", "minor", "pillar", "spiritualist", "condominiums", "default", "household", "stall", "wine_cellar", "varies", "waste", "cultural", "geothermal", "bandstand", "club_house", "palace", "sports_hall", "cottage", "riding_school", "railway", "kitchen", "gym", "0", "air_shaft", "stands", "livestock", "embassy", "none", "ship", "tech_cab", "shops", "free", "terraced_house", "kiln", "barrack", "fossil", "mixed_use", Tag.KEY_AMENITY, "presbytery", "townhall", "services", "ramp", "demolished", "sea", "parish", "maisonette", "row_house", "parish_hall", "bahai", "baishin", "allotment", "musalla", "tribune", "derelict", "boat", "gymnasium", "cooling_tower", "guardhouse", "mink_shed", "shack", "tier", "stairs", "heliport", "pumping_station", "mortuary", "water_tank", "barracks", "tomb", "tumulus", "weir", "way", "yurta", "attached", "viaduct", "cellar", "different", "recreation", "parish_church", "aviary", "dovecote", "health_post", "stand", "religious", "mixed", "patio", "water_tower", "sauna", "convenience", "department_store", "greengrocer", "hardware", "alcohol", "outdoor", "gift", "toys", "variety_store", "jewelry", "books", "confectionery", "bicycle", "beverages", "copyshop", "photo", "laundry", "dry_cleaning", "ice_cream", "bare_rock", "lock_gate", "unpaved", "dirt", "hiking", "bicycle", "mtb", "iwn", "nwn", "rwn", "lwn", "icn", "ncn", "rcn", "lcn", "t1", "t2", "t3", "t4", "t5", "t6", "excellent", "good", "bad", "horrible", "saddle", "waterfall", "guidepost", "map", "skitour", "hike", "ice_skate", "sleigh", "snow_park", "sports"};

    @NonNull
    static String getKindName(int kind) {
        if (isPlace(kind)) {
            return "kind_place";
        }
        if (isEmergency(kind)) {
            return kinds[0];
        }
        if (isAccommodation(kind)) {
            return kinds[1];
        }
        if (isFood(kind)) {
            return kinds[2];
        }
        if (isAttraction(kind)) {
            return kinds[3];
        }
        if (isEntertainment(kind)) {
            return kinds[4];
        }
        if (isShopping(kind)) {
            return kinds[5];
        }
        if (isService(kind)) {
            return kinds[6];
        }
        if (isReligion(kind)) {
            return kinds[7];
        }
        if (isEducation(kind)) {
            return kinds[8];
        }
        if (isKids(kind)) {
            return kinds[9];
        }
        if (isPets(kind)) {
            return kinds[10];
        }
        if (isVehicles(kind)) {
            return kinds[11];
        }
        if (isTransportation(kind)) {
            return kinds[12];
        }
        if (isHikeBike(kind)) {
            return kinds[13];
        }
        if (isBuilding(kind)) {
            return "kind_building";
        }
        if (isUrban(kind)) {
            return kinds[14];
        }
        if (isRoad(kind)) {
            return "kind_road";
        }
        if (isBarrier(kind)) {
            return kinds[15];
        }
        return "";
    }

    public static boolean isPlace(int kind) {
        return (kind & 1) > 0;
    }

    public static boolean isRoad(int kind) {
        return (kind & 2) > 0;
    }

    public static boolean isBuilding(int kind) {
        return (kind & 4) > 0;
    }

    public static boolean isEmergency(int kind) {
        return (kind & 8) > 0;
    }

    public static boolean isAccommodation(int kind) {
        return (kind & 16) > 0;
    }

    public static boolean isFood(int kind) {
        return (kind & 32) > 0;
    }

    public static boolean isAttraction(int kind) {
        return (kind & 64) > 0;
    }

    public static boolean isEntertainment(int kind) {
        return (kind & 128) > 0;
    }

    public static boolean isShopping(int kind) {
        return (kind & 256) > 0;
    }

    public static boolean isService(int kind) {
        return (kind & 512) > 0;
    }

    public static boolean isReligion(int kind) {
        return (kind & 1024) > 0;
    }

    public static boolean isEducation(int kind) {
        return (kind & 2048) > 0;
    }

    public static boolean isKids(int kind) {
        return (kind & 4096) > 0;
    }

    public static boolean isPets(int kind) {
        return (kind & 8192) > 0;
    }

    public static boolean isVehicles(int kind) {
        return (kind & 16384) > 0;
    }

    public static boolean isTransportation(int kind) {
        return (32768 & kind) > 0;
    }

    public static boolean isHikeBike(int kind) {
        return (65536 & kind) > 0;
    }

    public static boolean isUrban(int kind) {
        return (131072 & kind) > 0;
    }

    public static boolean isBarrier(int kind) {
        return (262144 & kind) > 0;
    }
}
