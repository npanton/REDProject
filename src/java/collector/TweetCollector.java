package collector;
// TODO: close stuff


import java.io.*;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.sql.*;
import java.util.Calendar;
import java.util.Date;
import java.util.logging.Level;
import org.apache.log4j.Logger;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import redutils.DatabaseUtil;
import redutils.GeoUtil;
import redutils.TooManyRequestsException;



/**
 * @author Niall Panton
 *         Date: 30/08/2011
 *         Time: 22:04
 */
public class TweetCollector implements Runnable {
    
    
    private static org.apache.log4j.Logger logger = Logger.getLogger(TweetCollector.class);
    private String tableName, bounds, userName, password, line;
    private int accuracy, rate, smoothieRate, retries = 0;
    private long cleanNext;
    private BufferedWriter graphOut;
    
    
    public String encode(String source) {
        sun.misc.BASE64Encoder enc = new sun.misc.BASE64Encoder();
        return (enc.encode(source.getBytes()));
    }
    
    public TweetCollector(String tableName, String bounds, int accuracy, int reportRate,  String userName, String password) {
        this.tableName = tableName;
        this.bounds = bounds;
        this.accuracy = accuracy;
        cleanNext = System.currentTimeMillis() + 1800000l;
        startGraphs();
        // Create logging information
        try {
            // Graphing information
            graphOut = new BufferedWriter(new FileWriter("rate_graph.csv", true));
            DatabaseUtil.createSmoothieTable();
        } catch (IOException ie) {
            logger.warn("Cannot write rate graph");
        }
        this.userName = userName;
        this.password = password;
    }
    
    public void convertJSON(InputStream is) throws Exception {
        BufferedReader reader = new BufferedReader(new InputStreamReader(is));
        JSONObject json;
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
                logger.fatal("Scrub Geo request ignored");
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
                float[] latLonAcc = GeoUtil.findBoundingBoxArea(json.getJSONObject("place").getJSONObject("bounding_box").getJSONArray("coordinates").getJSONArray(0));
                
                // If the bounding box accuracy is less than the user specified then reject the tweet and don't geocode
                if (latLonAcc[2] < accuracy)
                    return;
                else
                    location = latLonAcc;
                
                //                  //In theory we never geocode, it's a waste of time
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
            smoothieRate++;
            // Log the entire json string separately in the log file
            //            logger.info("\n" + json.toString() + "\n");
            
        } catch (JSONException je) {
            logger.warn("JSON Parsing Exception\nLine was: \n" + line + "\nStack Trace: " + je);
        }
    }
    
    public void processTrack(JSONObject json) {
        try {
            logger.info("Now processed " + json.getJSONObject("limit").getInt("track") + " tweets");
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
            logger.error("Cannot update delete requests\n" + e.toString());
        } catch (ClassNotFoundException c) {
            logger.error("Cannot find jdbc class");
        } catch (JSONException j) {
            logger.error("JSON Exception\n" + j.toString());
        } finally {
            try {
                connect.close();
            } catch (SQLException se) {
                logger.error("Cannot close SQL Exception\n" + se.toString());
            }
        }
    }
    
    private void startGraphs() {
        // TODO: check this is ok in terms of starting the threads but not monitoring them
        try{
            (new Thread(new Grapher())).start();
            (new Thread(new SmoothieMaker())).start();
        }
        catch(Exception e){
            e.printStackTrace();
        }
    }
    
    
    private class SmoothieMaker implements Runnable {
        
        
        // TODO: Make this and other graph rate thread, operate on the parent thread is still alive
        @Override
        public void run() {
            while (true) {
                try {
                    // Eliminate drift
                    Thread.sleep(1000 - (System.currentTimeMillis() % 1000));
                    System.out.println("Rate " + smoothieRate);
                    
                    Connection connect = null;
                    try {
                        // MySQL
                        //Class.forName("com.mysql.jdbc.Driver");
                        // SQLite
                        Class.forName("org.sqlite.JDBC");
                        
                        //set up string
                        //connection = DriverManager.getConnection("jdbc:mysql://localhost:8889/red?" + "user=root&password=root");
                        connect = DriverManager.getConnection("jdbc:sqlite:smoothie.db");
                        
                        PreparedStatement preparedStatement = connect.prepareStatement("insert into  smoothie values (?, ?, ?)");
                        preparedStatement.setLong(1, System.currentTimeMillis());
                        preparedStatement.setTimestamp(2, new Timestamp((new Date()).getTime()));
                        preparedStatement.setLong(3, smoothieRate);
                        preparedStatement.executeUpdate();
                        preparedStatement.close();
                    } catch (SQLException e) {
                        logger.error("Cannot update smoothie graph\n" + e.getMessage());
                    } catch (ClassNotFoundException c) {
                        logger.error("Cannot find jdbc class");
                    } finally {
                        try {
                            connect.close();
                        } catch (SQLException se) {
                            logger.error("Cannot close jdbc connection\n"+se.getMessage());
                        }
                    }
                    
                    smoothieRate = 0;
                    
                } catch (Exception e) {
                    logger.error("Cannot update smoothie graph, exception\n" + e.getMessage());
                }
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
        
        @Override
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
                    logger.error("Cannot update graph, exception\n" + e.getMessage());
                }
            }
        }
        
        
    }
    
    
    private class Cleaner implements Runnable {
        @Override
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
                logger.error("Cannot update delete requests\n" + e.getMessage());
            } catch (ClassNotFoundException c) {
                logger.error("Cannot find jdbc class");
            } finally {
                try {
                    connect.close();
                } catch (SQLException se) {
                    logger.error("Cannot close jdbc connection\n"+se.getMessage());
                }
            }
        }
    }
    
    
    // Sleep and retry after an exception
    public void sleep() {
        //TODO: Handle a high number of retries, give up gracefully
        // Report how long it will be sleeping for
        logger.info("Sleeping: " + (retries + 1) + " minutes");
        try {
            Thread.sleep((retries + 1) * 60000l);
        } catch (InterruptedException ie) {
            logger.warn("Sleep interupted while waiting to retry connection");
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
            preparedStatement.setFloat(6, (float)GeoUtil.getEastings(lat, lon));
            preparedStatement.setFloat(7, (float)GeoUtil.getNorthings(lat, lon));
            preparedStatement.setString(8, tweet);
            preparedStatement.executeUpdate();
            preparedStatement.close();
            
        } catch (SQLException se) {
            logger.error("Cannot update tweets\nOffending tweet" + tweet + "\n" + se.getMessage());
            
        } catch (ClassNotFoundException cnfe) {
            logger.error("Cannot find jdbc class");
        } finally {
            try {
                connect.close();
                preparedStatement.close();
            } catch (SQLException se) {
                logger.error("Cannot close connection\n" + se.getMessage());
            }
            catch(NullPointerException npe){
                logger.error("Null pointer exception\n" + npe.getMessage());
            }
        }
        
    }
    
    
    
    
    @Override
    public void run() {
        try {
            HttpURLConnection con = (HttpURLConnection) new URL("https://stream.twitter.com/1/statuses/filter.json?locations=" + bounds).openConnection();
            // TODO: remove hard coded values
            // TODO: Refactor to make a class field
//            con.setRequestProperty("Authorization", "Basic " + encode(userName + ":" + password));
                        con.setRequestProperty("Authorization", "Basic " + encode("eventdetection" + ":" + "blowerism!"));
            
            con.setReadTimeout(240000);
            con.setRequestMethod("POST");
            logger.info("Response: " + con.getResponseMessage() + ", " + con.getResponseCode());
            //            System.out.println("Response: " + con.getResponseMessage() + ", " + con.getResponseCode());
            // Too many requests
            if (con.getResponseCode() == 420) {
                throw new TooManyRequestsException();
            }
            // Capture stream of tweets
            convertJSON(con.getInputStream());
            // Handle the various exceptions
        } catch (SocketTimeoutException ste) {
            // Lazy exponential backup for retrying lost connections
            logger.info("Connection Lost, Retrying...");
            sleep();
            //ste.printStackTrace();
        } catch (UnknownHostException uhe) {
            // Lazy exponential backup for retrying no connections
            logger.info("No Connection, Retrying...");
            sleep();
            // TODO: remove this line
        } catch (TooManyRequestsException tmre) {
            logger.info("Too Many Requests, Retrying");
            sleep();
        } catch (Exception e) {
            logger.warn("Generic, Retrying. Details: \n" + e.getMessage());
            logger.error(line);
            
            
            JSONObject json;
            try {
                json = new JSONObject(line);
                                processTrack(json);

            } catch (JSONException ex) {
            }

            
            
            
//            sleep();
        }
    }
    
    public static void main(String args[]) {
        if (!DatabaseUtil.checkDatabaseExists("tweets")) {
            DatabaseUtil.createTweetsTable("tweets");
            DatabaseUtil.createDeleteRequestsTable();
        }
        //System.graphOut.println(d.createLocationCacheTable());
        // Create a collector that adds to a db called tweets, collects tweets from the bounds specified, accuracy threshold and reporting rate
        // TODO: bounds and accuracy should relate to each other
        //        TweetCollector t = new TweetCollector("tweets", "-0.54,51.27,0.33,51.76", 4, 1800000);
//        TweetCollector t = new TweetCollector("tweets", "-6.0,49.98,1.9,59.7", 10, 1800000, "realtimeed", "eipein321!");
                TweetCollector t = new TweetCollector("tweets", "-180,-90,180,90", 10, 1800000, "realtimeed", "eipein321!");

        Thread tcThread = new Thread(t);
        tcThread.start();
        
    }
    
    

}
