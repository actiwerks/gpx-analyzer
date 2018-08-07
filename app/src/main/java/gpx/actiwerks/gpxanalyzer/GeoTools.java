package gpx.actiwerks.gpxanalyzer;

import java.math.BigDecimal;
import java.math.RoundingMode;

public class GeoTools {

    private static final double DEGREES_TO_KM_MULTIPLIER = 1.853159616;

    public static double distance(double lat1, double lon1, double lat2, double lon2) {
        double theta = lon1 - lon2;
        double dist = Math.sin(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) + Math.cos(deg2rad(lat1)) * Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(theta));
        dist = Math.acos(dist);
        dist = rad2deg(dist);
        dist = dist * 60 * DEGREES_TO_KM_MULTIPLIER;
        if (Double.isNaN(dist)) {
            return 0.0;
        }
        return dist;
    }

    public static double bearing(double lat1, double lon1, double lat2, double lon2) {
        double y = Math.sin(deg2rad(lon2 - lon1)) * Math.cos(deg2rad(lat2));
        double x = Math.cos(deg2rad(lat1)) * Math.sin(deg2rad(lat2)) - Math.sin(deg2rad(lat1)) *
                Math.cos(deg2rad(lat2)) * Math.cos(deg2rad(lon2 - lon1));
        double bearing = Math.atan2(y, x);
        bearing = rad2deg(bearing);
        return bearing;
    }

    private static double deg2rad(double deg) {
        return (deg * Math.PI / 180.0);
    }


    private static double rad2deg(double rad) {
        return (rad * 180 / Math.PI);
    }

    public static double round(double value, int places) {
        if (places < 0) throw new IllegalArgumentException();

        BigDecimal bd = new BigDecimal(value);
        bd = bd.setScale(places, RoundingMode.HALF_UP);
        return bd.doubleValue();
    }

}
