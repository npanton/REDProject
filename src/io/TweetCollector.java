package io;
// TODO: close stuff


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import util.*;

import java.util.Date;
import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.Calendar;
import java.util.logging.*;


/**
 * @author Niall Panton
 *         Date: 30/08/2011
 *         Time: 22:04
 */
public class TweetCollector implements Runnable {


    // Table
    private String tableName, bounds;
    private int accuracy, rate, retries = 0, reportRate;
    private long cleanNext;
    private final static Logger logger = Logger.getLogger(TweetCollector.class.getName());
    private BufferedWriter graphOut;
    private long id;

    public String encode(String source) {
        sun.misc.BASE64Encoder enc = new sun.misc.BASE64Encoder();
        return (enc.encode(source.getBytes()));
    }

    public TweetCollector(String tableName, String bounds, int accuracy, int reportRate) {
        this.tableName = tableName;
        this.bounds = bounds;
        this.accuracy = accuracy;
        this.reportRate = reportRate;
        cleanNext = System.currentTimeMillis() + 1800000l;
        Grapher g = new Grapher();
        Thread grapherThread = new Thread(g);
        grapherThread.start();
        // Create logging information
        try {


            LogManager lm = LogManager.getLogManager();
            lm.addLogger(logger);
            logger.setLevel(Level.INFO);
            Calendar now = Calendar.getInstance();
            // Create a new log with the date as a part of the name
            String date = ((now.get(Calendar.DAY_OF_MONTH) > 9) ? "" : "0") + now.get(Calendar.DAY_OF_MONTH) + "-" + ((((now.get(Calendar.MONTH) + 1) > 9) ? "" : "0") + (now.get(Calendar.MONTH) + 1)) + "-" + now.get(Calendar.YEAR);
            FileHandler fh = new FileHandler("collector_" + date + ".log");
            fh.setFormatter(new SimpleFormatter());
            logger.addHandler(fh);

            // Graphing information
            graphOut = new BufferedWriter(new FileWriter("rate_graph.csv", true));
//            graphOut.close();


        } catch (IOException ie) {
            System.out.println("Failed to init logger");
            ie.printStackTrace();
        }
    }

    public void convertJSON(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        JSONObject json;
        String line;
        while ((line = reader.readLine()) != null) {


            if (System.currentTimeMillis() >= cleanNext) {

                Cleaner c = new Cleaner();
                Thread cleaner = new Thread(c);
                cleaner.start();
                //TODO: make generic
                cleanNext = System.currentTimeMillis() + 1800000l;
            }


            json = new JSONObject(line);
            if (json.getString("user") != null) {
                processTweet(json);
            } else if (json.getString("limit") != null) {
                processTrack(json);
            } else if (json.getString("delete") != null) {
                processDelete(json);
            } else if (json.getString("scrub_geo") != null) {
                // TODO: handle properly
            }
        }
        is.close();
    }

    public void processTweet(JSONObject json) {
        String line = "";
        try {
            // Connected so reset number of retries
            if (retries > 0)
                retries = 0;

            String tweet = json.getString("text");
//            JSONObject user = json.getJSONObject("user");
//            String userID = user.getString("screen_name");
            long tweetID = json.getLong("id");
            float[] location = new float[3];
            location[2] = 8;
            try {
                JSONArray locObj = json.getJSONObject("geo").getJSONArray("coordinates");
                location[0] = (float) locObj.getDouble(1);
                location[1] = (float) locObj.getDouble(0);
            } catch (Exception e) {

                // Geocode limit 2500, handle, possibly use a bounding box. Read external file in for settings use robots.txt style so order doesn't matter. Mkae bounding box area relate to accuracy so when geocoding fails or w/e we have something to fall back on
                // Use table to store daily query count to google
                // First find the bounding box area, if it is small enough according to the accuracy specified then use that first
                float[] latLonAcc = GeoUtil.findBoundingBoxArea(json.getJSONObject("place").getJSONObject("bounding_box").getJSONArray("coordinates").getJSONArray(0), logger);

                // If the bounding box accuracy is less than the user specified then reject the tweet and don't geocode
                if (latLonAcc[2] < accuracy)
                    return;
                else
                    location = latLonAcc;

//                  //In theory we never geocode
//                  //Deprecated
//                  //Else we need to geocode an address, this could be removed as it may lead to ambiguity,
//                  //cost time in looking up address and not that many tweets return textual locations,
//                  //Alternatively a bounding box is always returned, you could take the centre of this as your co-ords
//                  location = geocode(json.getJSONObject("place").getString("full_name").trim());
//                  //Failing, just skip this tweet
//                  if (location == null)
//                      continue;
            }
            addToDatabase(tweet, tweetID, location[0], location[1], (int) location[2]);
            rate++;
            // Log the entire json string separately in the log file
            logger.log(Level.INFO, "\n" + json.toString() + "\n");

        } catch (JSONException je) {
            logger.log(Level.WARNING, "JSON Parsing Exception\nLine was: \n" + line + "\nStack Trace: " + je);
        }
    }

    public void processTrack(JSONObject json) {
        try {
            logger.log(Level.INFO, "Now processed " + json.getJSONObject("limit").getInt("track") + " tweets");
        } catch (JSONException je) {
//            je.printStackTrace();
        }
    }



    public void processDelete(JSONObject json) {
        Connection connect = null;
        try {
            // MySQL
            //Class.forName("com.mysql.jdbc.Driver");
            // SQLite
            Class.forName("org.sqlite.JDBC");

//            connect = DriverManager.getConnection("jdbc:mysql://localhost:8889?" + "user=root&password=root");
            connect = DriverManager.getConnection("jdbc:sqlite:delete_requests.db");

            PreparedStatement preparedStatement = connect.prepareStatement("SELECT * FROM  `" + tableName + "` WHERE ID = ?");
            preparedStatement.setString(1, json.getJSONObject("delete").getJSONObject("status").getString("id"));
            System.out.println(json.getJSONObject("delete").getJSONObject("status").getString("id"));
            ResultSet tweet = preparedStatement.executeQuery();
            if (tweet.next()) {
                preparedStatement = connect.prepareStatement("DELETE FROM  `" + tableName + "` WHERE ID = ?");
                preparedStatement.setString(1, json.getJSONObject("delete").getJSONObject("status").getString("id"));
                preparedStatement.executeUpdate();
            } else {
                preparedStatement = connect.prepareStatement("insert into `delete_requests` values (default, ?)");
                preparedStatement.setString(1, json.getJSONObject("delete").getJSONObject("status").getString("id"));
                preparedStatement.executeUpdate();
            }
            tweet.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
        } catch (JSONException j) {
            j.printStackTrace();
        } finally {
            try {
                connect.close();
            } catch (SQLException se) {
                // TODO: handle
                se.printStackTrace();
            }
        }
    }


    private class Grapher implements Runnable {
        boolean init = false;

        public long getInitialSleep() {

            Date now = new Date();
            Calendar nowCal = Calendar.getInstance();
            nowCal.setTime(now);
            // Between 0 and 29 minutes
            if (nowCal.get(Calendar.MINUTE) > 0 && nowCal.get(Calendar.MINUTE) < 29) {
                nowCal.set(Calendar.MINUTE, 30);
                nowCal.set(Calendar.SECOND, 0);
                nowCal.set(Calendar.MILLISECOND, 0);

            } else {
                nowCal.set(Calendar.MINUTE, 59);
                nowCal.set(Calendar.SECOND, 59);
                nowCal.set(Calendar.MILLISECOND, 0);
            }
            Date target = nowCal.getTime();
            long initialSleep = target.getTime() - now.getTime();
            System.out.println((initialSleep / 1000));
            System.out.println(now);
            System.out.println(target);

            return initialSleep;
        }

        public void run() {
            while (true) {
                try {
                    if (!init) {
                        init = true;
                        Thread.sleep(getInitialSleep());
                        graphOut.write(rate + ",\t" + new Timestamp(System.currentTimeMillis()).toString() + "\n");
                        rate = 0;
                        graphOut.flush();
                    } else {
                        // Eliminate drift
                        Thread.sleep(1800000 - (System.currentTimeMillis() % 1800000));
                        graphOut.write(rate + ",\t" + new Timestamp(System.currentTimeMillis()).toString() + "\n");
                        rate = 0;
                        graphOut.flush();
                    }
                } catch (Exception e) {
                }
            }
        }


    }


    private class Cleaner implements Runnable {
        public void run() {
            Connection connect = null;
            try {
                // MySQL
                //Class.forName("com.mysql.jdbc.Driver");
                // SQLite
                Class.forName("org.sqlite.JDBC");

                //set up string
                //connection = DriverManager.getConnection("jdbc:mysql://localhost:8889/red?" + "user=root&password=root");
                connect = DriverManager.getConnection("jdbc:sqlite:delete_requests.db");

                PreparedStatement preparedStatement = connect.prepareStatement("SELECT * FROM  `delete_requests`");
                ResultSet requests = preparedStatement.executeQuery();
                while (requests.next()) {
                    preparedStatement = connect.prepareStatement("DELETE FROM  `" + tableName + "` WHERE ID = ?");
                    preparedStatement.setString(1, requests.getString("tweetID"));
                    preparedStatement.executeUpdate();
                }
                requests.close();
                preparedStatement.close();
            } catch (SQLException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException c) {
                c.printStackTrace();
            } finally {
                try {
                    connect.close();
                } catch (SQLException se) {
                    // TODO: handle
                    se.printStackTrace();
                }
            }
        }
    }


    @Deprecated
    public float[] geocode(String location) {
        // Lat Lon float array
        float[] latlon;
        // Check cache for existing location
        latlon = getCache(location);
        // If no location is found then use google to find one
        if (latlon == null) {
            // Create a new geo-coder object
            Geocoder.Location loc;
            try {
                latlon = new float[3];
                // Geo-code location
                loc = (Geocoder.getLocation(location));
                // if accuracy is great enough, use it. Otherwise just log it.
                latlon[0] = Float.parseFloat(loc.lon);
                latlon[1] = Float.parseFloat(loc.lat);
                latlon[2] = loc.accuracy;
                setCache(location, latlon);
                // Add location, lat/lon and accuracy to the database
                try {
                    // sleep to avoid 620 error
                    Thread.sleep(110);
                } catch (Exception ee) {
                    return null;
                }
            } catch (Exception e) {
                e.printStackTrace();
                return null;
            }
            if (loc.accuracy < accuracy)
                return null;
        }
        return latlon;
    }

    @Deprecated
    public float[] getCache(String location) {
        float[] latLon = new float[3];
        try {
            Class.forName("com.mysql.jdbc.Driver");
            Connection connect = DriverManager.getConnection("jdbc:mysql://localhost:8889?" + "user=root&password=root");

            PreparedStatement preparedStatement = connect.prepareStatement("SELECT  `lat` ,  `lon`, `accuracy` FROM  `red`.`location_cache` WHERE  `location` =  ? AND `accuracy` >= ? LIMIT 1");
            preparedStatement.setString(1, location);
            preparedStatement.setInt(2, accuracy);

            ResultSet latLons = preparedStatement.executeQuery();
            if (latLons.next()) {
                latLon[0] = latLons.getFloat("lat");
                latLon[1] = latLons.getFloat("lon");
                latLon[2] = latLons.getInt("accuracy");
            } else
                return null;
            latLons.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
        }

        return latLon;
    }

    @Deprecated
    public void setCache(String location, float[] latLon) {
        try {
            // This will load the MySQL driver, each DB has its own driver
            Class.forName("com.mysql.jdbc.Driver");
            // Setup the connection with the DB
            Connection connectCache = DriverManager.getConnection("jdbc:mysql://localhost:8889/red?" + "user=root&password=root");

            PreparedStatement preparedStatementCache = connectCache.prepareStatement("insert into `red`.`location_cache` values (default, ?, ?, ?, ?)");
            preparedStatementCache.setString(1, location);
            preparedStatementCache.setInt(2, (int) latLon[2]);
            preparedStatementCache.setFloat(3, latLon[0]);
            preparedStatementCache.setFloat(4, latLon[1]);
            preparedStatementCache.executeUpdate();
            preparedStatementCache.close();

        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
        }
    }


    // Sleep and retry after an exception
    public void sleep() {
        //TODO: Handle a high number of retries, give up gracefully
        // Report how long it will be sleeping for
        logger.log(Level.INFO, "Sleeping: " + (retries + 1) + " minutes");
        try {
            Thread.sleep((retries + 1) * 60000l);
        } catch (InterruptedException ie) {
            ie.printStackTrace();
        }
        // Increment the global number of retries
        retries++;
        run();
    }

    public void addToDatabase(String tweet, Long id, float lat, float lon, int accuracy) {
        Connection connect = null;
        PreparedStatement preparedStatement = null;
        try {
            // MySQL
            // Class.forName("com.mysql.jdbc.Driver");
            // SQLite
            Class.forName("org.sqlite.JDBC");

            // MySQL
            // connect = DriverManager.getConnection("jdbc:mysql://localhost:8889/red?" + "user=root&password=root");
            // SQLite
            connect = DriverManager.getConnection("jdbc:sqlite:" + tableName + ".db");

            preparedStatement = connect.prepareStatement("insert into  " + tableName + " values (?, ?, ?, ?, ?, ?, ?, ?)");
            preparedStatement.setLong(1, id);
            preparedStatement.setTimestamp(2, new Timestamp((new Date()).getTime()));
            preparedStatement.setFloat(3, lat);
            preparedStatement.setFloat(4, lon);
            preparedStatement.setInt(5, accuracy);
            preparedStatement.setFloat(6, getX(lat));
            preparedStatement.setFloat(7, getY(lon));
            preparedStatement.setString(8, tweet);
            preparedStatement.executeUpdate();
            preparedStatement.close();

        } catch (SQLException se) {
            se.printStackTrace();
        } catch (ClassNotFoundException cnfe) {
            cnfe.printStackTrace();
        } finally {
            try {
                connect.close();
                preparedStatement.close();
            } catch (SQLException se) {
                // TODO: handle
                se.printStackTrace();
            }
            catch(NullPointerException npe){
                npe.printStackTrace();
            }
        }

    }

    // Convert latitude to its X plain value in Lambert's cylindrical map projection
    private float getX(float lat) {
        float rad = (float) (Math.toRadians(lat));
        return (float) Math.sin(rad);
    }

    // Convert longitude to its Y plain value in Lambert's cylindrical map projection
    private float getY(float lon) {
        return ((float) Math.toRadians(lon)) / 90;
    }


    public void run() {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL("https://stream.twitter.com/1/statuses/filter.json?locations=" + bounds).openConnection();
            // TODO: remove hard coded values
//            con.setRequestProperty("Authorization", "Basic " + encode("realtimeed" + ":" + "eipein321!"));
            con.setRequestProperty("Authorization", "Basic " + encode("eventdetection" + ":" + "blowerism!"));

            con.setReadTimeout(240000);
            con.setRequestMethod("POST");
            logger.log(Level.INFO, "Response: " + con.getResponseMessage() + ", " + con.getResponseCode());
            System.out.println("Response: " + con.getResponseMessage() + ", " + con.getResponseCode());
            // Too many requests
            if (con.getResponseCode() == 420) {
                throw new TooManyRequestsException();
            }
            // Capture stream of tweets
            convertJSON(con.getInputStream());
            // Handle the various exceptions
        } catch (SocketTimeoutException ste) {
            // Lazy exponential backup for retrying lost connections
            logger.log(Level.WARNING, "Connection Lost, Retrying...");
            sleep();
            //ste.printStackTrace();
        } catch (UnknownHostException uhe) {
            // Lazy exponential backup for retrying no connections
            logger.log(Level.WARNING, "No Connection, Retrying...");
            sleep();
            // TODO: remove this line
        } catch (TooManyRequestsException tmre) {
            logger.log(Level.WARNING, "Too Many Requests, Retrying");
            sleep();
        } catch (Exception e) {
            e.printStackTrace();
            logger.log(Level.WARNING, "Generic, Retrying. Details: \n" + e);
            sleep();
        }
    }

    public static void main(String args[]) {
        if (!DatabaseAdmin.checkDatabaseExists("tweets")) {
            DatabaseAdmin.createTweetsTable("tweets");
            DatabaseAdmin.createDeleteRequestsTable();
        }
        //System.graphOut.println(d.createLocationCacheTable());
        // Create a collector that adds to a db called tweets, collects tweets from the bounds specified, accuracy threshold and reporting rate
        // TODO: bounds and accuracy should relate to each other
//        TweetCollector t = new TweetCollector("tweets", "-0.54,51.27,0.33,51.76", 4, 1800000);
        TweetCollector t = new TweetCollector("tweets", "-6.0,49.98,1.9,59.7", 10, 1800000);
        Thread tcThread = new Thread(t);
        tcThread.start();

    }



}
