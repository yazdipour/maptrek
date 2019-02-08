package mobi.maptrek.util;

import android.support.annotation.DrawableRes;
import mobi.maptrek.R;
import mobi.maptrek.maps.maptrek.Tags;

public class ResUtils {
    @DrawableRes
    public static int getKindIcon(int kind) {
        if (Tags.isPlace(kind)) {
            return R.drawable.ic_location_city;
        }
        if (Tags.isEmergency(kind)) {
            return R.drawable.ic_local_hospital;
        }
        if (Tags.isAccommodation(kind)) {
            return R.drawable.ic_hotel;
        }
        if (Tags.isFood(kind)) {
            return R.drawable.ic_local_dining;
        }
        if (Tags.isAttraction(kind)) {
            return R.drawable.ic_account_balance;
        }
        if (Tags.isEntertainment(kind)) {
            return R.drawable.ic_local_see;
        }
        if (Tags.isShopping(kind)) {
            return R.drawable.ic_shopping_cart;
        }
        if (Tags.isService(kind)) {
            return R.drawable.ic_local_laundry_service;
        }
        if (Tags.isReligion(kind)) {
            return R.drawable.ic_change_history;
        }
        if (Tags.isEducation(kind)) {
            return R.drawable.ic_school;
        }
        if (Tags.isKids(kind)) {
            return R.drawable.ic_child_care;
        }
        if (Tags.isPets(kind)) {
            return R.drawable.ic_pets;
        }
        if (Tags.isVehicles(kind)) {
            return R.drawable.ic_directions_car;
        }
        if (Tags.isTransportation(kind)) {
            return R.drawable.ic_directions_bus;
        }
        if (Tags.isHikeBike(kind)) {
            return R.drawable.ic_directions_bike;
        }
        if (Tags.isBuilding(kind)) {
            return R.drawable.ic_business;
        }
        if (Tags.isUrban(kind)) {
            return R.drawable.ic_nature_people;
        }
        if (Tags.isRoad(kind)) {
            return R.drawable.ic_drag_handle;
        }
        if (Tags.isBarrier(kind)) {
            return R.drawable.ic_do_not_disturb_on;
        }
        return 0;
    }
}
