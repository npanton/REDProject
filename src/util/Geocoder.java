package util;
import java.io.*;
import java.net.*;
/**
 * @author Niall Panton
 *         Date: 30/08/2011
 *         Time: 22:26
 */

public class Geocoder {
	private final static String ENCODING = "UTF-8";
	private final static String KEY = "xyz";

	public static class Location {
		public String lon, lat;
        public int accuracy;

		private Location (String lat, String lon, int accuracy) {
			this.lon = lon;
			this.lat = lat;
            this.accuracy = accuracy;
		}

		public String toString () { return "Lat: "+lat+", Lon: "+lon + "\n Accuracy: " + accuracy; }
	}

	public static Location getLocation (String address) throws IOException {
		BufferedReader in = new BufferedReader (new InputStreamReader (new URL ("http://maps.google.com/maps/geo?q="+URLEncoder.encode (address, ENCODING)+"&output=csv&key="+KEY).openStream ()));
		String line;
		Location location = null;
		int statusCode = -1;
		while ((line = in.readLine ()) != null) {
			// Format: 200,6,42.730070,-73.690570
            // 4 or less for precision carries a penalty
			statusCode = Integer.parseInt (line.substring (0, 3));
			if (statusCode == 200)
				location = new Location (
				line.substring ("200,6,".length (), line.indexOf (',', "200,6,".length ())),
				line.substring (line.indexOf (',', "200,6,".length ())+1, line.length ()) , Integer.parseInt(line.substring(4, 5)));

		}

		if (location == null) {
			switch (statusCode) {
				case 400: throw new IOException ("Bad Request");
				case 500: throw new IOException ("Unknown error from Google Encoder");
				case 601: throw new IOException ("Missing query");
				case 602: return null;
				case 603: throw new IOException ("Legal problem");
				case 604: throw new IOException ("No route");
				case 610: throw new IOException ("Bad key");
				case 620: throw new IOException ("Too many queries");
			}
		}
		return location;
	}

	public static void main (String[] argv) throws Exception {
		System.out.println (Geocoder.getLocation ("Carlisle, Cumbria, United Kingdom"));
	}
}
