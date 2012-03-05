package util;

import org.json.JSONArray;
import org.json.JSONException;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author Niall Panton
 *         Date: 06/09/2011
 *         Time: 21:13
 */
public class GeoUtil {

    // Twitter's clues to the tweets locality vary between 'admin' for whole cities and city for neighbourhoods, so its results can't be used for accuracy assignment
    // Examples:
    // Country >2600
    // 384046km England Wales, Bit of Scotland - 1

    // Large City, or a group of cities 400-2600
    // 2642km greater london - 2
    // 921km Leeds, Bradford + surrounding - 2

    // City or Small City + Metropolitan area 150-400 km/sq for 3
    // 398km Cardiff + surrounding - 3
    // 338km Aberdeenshire - 3

    // Small city, or group of towns 50-150 km/sq for 3/4
    // 145km Coventry + surrounding  - 3/4
    // 114km Staines, Ashford - 3/4

    // Group of districts/boroughs of a city 25-50 km/sq for 4
    // 49km Kensington, Tottenham - 4


    public static float[] findBoundingBoxArea(JSONArray bounds, Logger logger) {
        double h, w;
        float[] latLonAcc = null;
        try {
//            pretty debug google maps output
//            System.out.println(bounds.getJSONArray(0).getDouble(1) + ", "+ bounds.getJSONArray(0).getDouble(0) +"\n" +bounds.getJSONArray(2).getDouble(1)+", "+bounds.getJSONArray(2).getDouble(0));

            latLonAcc = findBoundingBoxCentre(bounds.getJSONArray(0).getDouble(1), bounds.getJSONArray(0).getDouble(0), bounds.getJSONArray(2).getDouble(1), bounds.getJSONArray(2).getDouble(0));

            h = findBoundingLatLonDistance(bounds.getJSONArray(1).getDouble(1), bounds.getJSONArray(1).getDouble(0), bounds.getJSONArray(2).getDouble(1), bounds.getJSONArray(2).getDouble(0));
            w = findBoundingLatLonDistance(bounds.getJSONArray(0).getDouble(1), bounds.getJSONArray(0).getDouble(0), bounds.getJSONArray(1).getDouble(1), bounds.getJSONArray(1).getDouble(0));
            latLonAcc[2] = (float)assignAccuracy((int)(h * w));
        } catch (JSONException je) {
            logger.log(Level.WARNING, "JSON Parsing Exception\nLine was: \n" + bounds + "\nStack Trace: " + je);
        }
        return latLonAcc;
    }

    // TODO: make numbers less magic, or at least validate them
    public static int assignAccuracy(int area) {
        if (area > 2700) {
            return 1;
        }
        if (area > 400 && area <= 2700) {
            return 2;
        }
        if (area > 150 && area <= 400) {
            return 3;
        }
        else if (area > 50 && area <= 150) {
            return 3;
        }
        else if (area > 25 && area <= 50) {
            return 4;
        }
        else if (area >= 0 && area <= 25) {
            return 5;
        }
        else
            return 1;
    }

    private static float[] findBoundingBoxCentre(double latSouthWest, double lonSouthWest, double latNorthEast, double lonNorthEast) {
        float[] latLon = new float[3];
        latSouthWest += Math.abs(latSouthWest - latNorthEast) / 2;
        lonSouthWest += Math.abs(lonSouthWest - lonNorthEast) / 2;
        latLon[0] = (float) lonSouthWest;
        latLon[1] = (float) latSouthWest;
        return latLon;
    }

    public static double findBoundingLatLonDistance(double lat1, double lon1, double lat2, double lon2) {

        double R = 6371; // km
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        lat1 = Math.toRadians(lat1);
        lat2 = Math.toRadians(lat2);

        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) + Math.sin(dLon / 2) * Math.sin(dLon / 2) * Math.cos(lat1) * Math.cos(lat2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c;
    }

}
