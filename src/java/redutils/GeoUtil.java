package redutils;

/**
 *
 * @author niallpanton
 */

import clusterer.Quad;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import weka.core.Instance;
import weka.core.Instances;

/**
 * @author Niall Panton
 *         Date: 06/09/2011
 *         Time: 21:13
 */
public class GeoUtil {
    
    
        private static final double a = 6377563.396;
    private static final double b = 6356256.91;
    private static final double e0 = 400000;
    private static final double n0 = -100000;
    private static final double f0 = 0.9996012717;
    private static final double PHI0 = 49.00000000;
    private static final double LAM0 = -2.00000000;
    
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
    
    
    public static float[] findBoundingBoxArea(JSONArray bounds) {
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
//            logger.log(Level.WARNING, "JSON Parsing Exception\nLine was: \n" + bounds + "\nStack Trace: " + je);
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
    
    public static HashSet<Quad> instancesToQuads(Instances in){
    HashSet<Quad> out = new HashSet<Quad>();
    
    for(int i = 0; i < in.numInstances(); i++){
        Instance current = in.instance(i);
        out.add(new Quad(current.value(0), current.value(1), current.value(3), current.value(4)));
    }
    return out;
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
    
    public static String reverseGeocode(String latLon) {
        String addressOut = "";
        return "US";
        //        JSONObject results = null;
        //        try {
        //            HttpURLConnection con = (HttpURLConnection) new URL("http://maps.google.com/maps/api/geocode/json?latlng=" + latLon + "&sensor=false").openConnection();
        //            con.setRequestMethod("GET");
        //            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
        //            String out = "", line = null;
        //            while ((line = reader.readLine()) != null) {
        //                out += line;
        //            }
        //            results = new JSONObject(out);
        //            System.out.println(results);
        //            if(!results.getString("status").equalsIgnoreCase("ZERO_RESULTS")){
        //            JSONArray addressDetails = results.getJSONArray("results").getJSONObject(0).getJSONArray("address_components");
        //            for(int i = 0; i < addressDetails.length(); i++){
        //                JSONArray inner =  addressDetails.getJSONObject(i).getJSONArray("types");
        //                for(int j = 0; j < inner.length(); j++){
        //                    if(inner.getString(j).equalsIgnoreCase("sublocality") || inner.getString(j).equalsIgnoreCase("locality") || inner.getString(j).equalsIgnoreCase("administrative_area_level_1")){
        //                        addressOut += " "  + addressDetails.getJSONObject(i).getString("long_name");
        //                    }
        //                }
        //            }
        //                cache.put(latLon, addressOut);
        //            }
        //            else
        //                addressOut = "USA";
        //        } catch (Exception e) {
        //            e.printStackTrace();
        //        }
        //        try {
        //            Thread.sleep(110);
        //        } catch (InterruptedException i) {
        //        }
        //        return addressOut;
    }

        public static double getLat(double East, double North) {
        //Un-project Transverse Mercator eastings and northings back to latitude.
        // TODO: re-add comments


        double RadPHI0 = PHI0 * (Math.PI / 180);

        double af0 = a * f0;
        double bf0 = b * f0;
        double e2 = (Math.pow(af0, 2) - Math.pow(bf0, 2)) / Math.pow(af0, 2);
        double n = (af0 - bf0) / (af0 + bf0);
        double Et = East - e0;

        double PHId = initialLat(North, n0, af0, RadPHI0, n, bf0);

        double nu = af0 / (Math.sqrt(1 - (e2 * Math.pow((Math.sin(PHId)), 2))));
        double rho = (nu * (1 - e2)) / (1 - Math.pow(e2 * (Math.sin(PHId)), 2));
        double eta2 = (nu / rho) - 1;

        double VII = (Math.tan(PHId)) / (2 * rho * nu);
        double VIII = ((Math.tan(PHId)) / (24 * rho * Math.pow(nu, 3))) * (5 + (3 * Math.pow((Math.tan(PHId)), 2)) + eta2 - (9 * eta2 * Math.pow((Math.tan(PHId)), 2)));
        double IX = ((Math.tan(PHId)) / (720 * rho * Math.pow(nu, 5))) * (61 + (90 * Math.pow((Math.tan(PHId)), 2)) + (45 * Math.pow((Math.tan(PHId)), 4)));

        return (180 / Math.PI) * (PHId - (Math.pow(Et, 2) * VII) + (Math.pow(Et, 4) * VIII) - (Math.pow(Et, 6) * IX));

    }

    public static double initialLat(double North, double n0, double afo, double PHI0, double n, double bfo) {

        double PHI1 = ((North - n0) / afo) + PHI0;
        double M = Marc(bfo, n, PHI0, PHI1);
        double PHI2 = ((North - n0 - M) / afo) + PHI1;


        while (Math.abs(North - n0 - M) > 0.00001) {
            PHI2 = ((North - n0 - M) / afo) + PHI1;
            M = Marc(bfo, n, PHI0, PHI2);
            PHI1 = PHI2;
        }

        return PHI2;

    }

    public static double getLon(double East, double North) {

        double RadPHI0 = PHI0 * (Math.PI / 180);
        double RadLAM0 = LAM0 * (Math.PI / 180);

        double af0 = a * f0;
        double bf0 = b * f0;
        double e2 = (Math.pow(af0, 2) - Math.pow(bf0, 2)) / Math.pow(af0, 2);
        double n = (af0 - bf0) / (af0 + bf0);
        double Et = East - e0;

        double PHId = initialLat(North, n0, af0, RadPHI0, n, bf0);

        double nu = af0 / (Math.sqrt(1 - (e2 * Math.pow((Math.sin(PHId)), 2))));
        double rho = (nu * (1 - e2)) / (1 - Math.pow(e2 * (Math.sin(PHId)), 2));

        double X = Math.pow((Math.cos(PHId)), -1) / nu;
        double XI = (Math.pow((Math.cos(PHId)), -1) / (6 * Math.pow(nu, 3))) * ((nu / rho) + (2 * Math.pow((Math.tan(PHId)), 2)));
        double XII = (Math.pow((Math.cos(PHId)), -1) / (120 * Math.pow(nu, 5))) * (5 + (28 * Math.pow((Math.tan(PHId)), 2)) + (24 * Math.pow((Math.tan(PHId)), 4)));
        double XIIA = (Math.pow((Math.cos(PHId)), -1) / (5040 * Math.pow(nu, 7))) * (61 + (662 * Math.pow((Math.tan(PHId)), 2)) + (1320 * Math.pow((Math.tan(PHId)), 4)) + (720 * Math.pow((Math.tan(PHId)), 6)));

        return (180 / Math.PI) * (RadLAM0 + (Et * X) - (Math.pow(Et, 3) * XI) + (Math.pow(Et, 5) * XII) - (Math.pow(Et, 7) * XIIA));

    }


    public static double getEastings(double lat, double lon) {


        double RadPHI = lat * (Math.PI / 180);
        double RadLAM = lon * (Math.PI / 180);
        double RadLAM0 = LAM0 * (Math.PI / 180);

        double af0 = a * f0;
        double bf0 = b * f0;
        double e2 = (Math.pow(af0, 2) - Math.pow(bf0, 2)) / Math.pow(af0, 2);
        double n = (af0 - bf0) / (af0 + bf0);
        double nu = af0 / (Math.sqrt(1 - (e2 * Math.pow((Math.sin(RadPHI)), 2))));
        double rho = (nu * (1 - e2)) / (1 - Math.pow(e2 * (Math.sin(RadPHI)), 2));
        double eta2 = (nu / rho) - 1;
        double p = RadLAM - RadLAM0;

        double IV = nu * (Math.cos(RadPHI));
        double V = (nu / 6) * Math.pow((Math.cos(RadPHI)), 3) * ((nu / rho) - (Math.pow(Math.tan(RadPHI), 2)));
        double VI = (nu / 120) * Math.pow((Math.cos(RadPHI)), 5) * (5 - (18 * Math.pow((Math.tan(RadPHI)), 2)) + Math.pow((Math.tan(RadPHI)), 4) + (14 * eta2) - (58 * Math.pow((Math.tan(RadPHI)), 2) * eta2));

        return e0 + (p * IV) + (Math.pow(p, 3) * V) + (Math.pow(p, 5) * VI);
    }

    public static double getNorthings(double lat, double lon) {

//Convert angle measures to radians
        double RadPHI = lat * (Math.PI / 180);
        double RadLAM = lon * (Math.PI / 180);
        double RadPHI0 = PHI0 * (Math.PI / 180);
        double RadLAM0 = LAM0 * (Math.PI / 180);

        double af0 = a * f0;
        double bf0 = b * f0;
        double e2 = (Math.pow(af0, 2) - Math.pow(bf0, 2)) / Math.pow(af0, 2);
        double n = (af0 - bf0) / (af0 + bf0);
        double nu = af0 / (Math.sqrt(1 - (e2 * Math.pow((Math.sin(RadPHI)), 2))));
        double rho = (nu * (1 - e2)) / (1 - Math.pow((e2 * (Math.sin(RadPHI))), 2));
        double eta2 = (nu / rho) - 1;
        double p = RadLAM - RadLAM0;
        double M = Marc(bf0, n, RadPHI0, RadPHI);

        double I = M + n0;
        double II = (nu / 2) * (Math.sin(RadPHI)) * (Math.cos(RadPHI));
        double III = ((nu / 24) * (Math.sin(RadPHI)) * Math.pow((Math.cos(RadPHI)), 3)) * (5 - Math.pow((Math.tan(RadPHI)), 2) + (9 * eta2));
        double IIIA = ((nu / 720) * (Math.sin(RadPHI)) * Math.pow((Math.cos(RadPHI)), 5)) * (61 - (58 * Math.pow((Math.tan(RadPHI)), 2)) + Math.pow((Math.tan(RadPHI)), 4));

        return (I + (Math.pow(p, 2) * II) + (Math.pow(p, 4) * III) + (Math.pow(p, 6) * IIIA));
    }

    public static double Marc(double bf0, double n, double PHI0, double PHI) {

        return bf0 * (((1 + n + ((5 / 4) * Math.pow(n, 2)) + ((5 / 4) * Math.pow(n, 3))) * (PHI - PHI0))
                - (((3 * n) + (3 * Math.pow(n, 2)) + ((21 / 8) * Math.pow(n, 3))) * (Math.sin(PHI - PHI0)) * (Math.cos(PHI + PHI0)))
                + ((((15 / 8) * Math.pow(n, 2)) + ((15 / 8) * Math.pow(n, 3))) * (Math.sin(2 * (PHI - PHI0))) * (Math.cos(2 * (PHI + PHI0))))
                - (((35 / 24) * Math.pow(n, 3)) * (Math.sin(3 * (PHI - PHI0))) * (Math.cos(3 * (PHI + PHI0)))));
    }
    
}

