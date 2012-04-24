/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package redutils;

import clusterer.EMCluster;
import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Date;
import org.apache.log4j.Logger;
import weka.core.Instances;
import weka.experiment.InstanceQuery;

/**
 * @author Niall Panton
 *         Date: 01/09/2011
 *         Time: 17:50
 */
public class DatabaseUtil {
    private static org.apache.log4j.Logger log = Logger.getLogger(DatabaseUtil.class);
    
    public static boolean createTweetsTable(String tableName) {
        Connection connection = null;
        try {
            // MySQL
            //Class.forName("com.mysql.jdbc.Driver");
            // SQLite
            Class.forName("org.sqlite.JDBC");
            
            //set up string
            //connection = DriverManager.getConnection("jdbc:mysql://localhost:8889/red?" + "user=root&password=root");
            connection = DriverManager.getConnection("jdbc:sqlite:" + tableName + ".db");
            
            
            Statement stmt = connection.createStatement();
            
            try {
                String drop = "DROP TABLE `" + tableName + "`";
                stmt.executeUpdate(drop);
            } catch (Exception e) {
                //                    TODO: Log the error
            }
            String create = " CREATE TABLE `" + tableName + "` (\n" +
                    "  `ID` bigint(20) NOT NULL,\n" +
                    "  `dateTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
                    "  `lat` float NOT NULL,\n" +
                    "  `lon` float NOT NULL,\n" +
                    "  `accuracy` SMALLINT( 2 ) NOT NULL ,\n " +
                    "  `x` float NOT NULL,\n" +
                    "  `y` float NOT NULL,\n" +
                    "  `tweet` varchar(512) NOT NULL,\n" +
                    "  PRIMARY KEY (`ID`)\n" +
                    ");";
            stmt.executeUpdate(create);
        } catch (SQLException e) {
            log.error(e.toString());
            return false;
        } catch (ClassNotFoundException c) {
            log.error(c.toString());
            return false;
        } finally {
            try {
                connection.close();
            } catch (SQLException se) {
                log.error(se.toString());
            }
            
        }
        return true;
    }
    
    public static boolean createSmoothieTable() {
        Connection connection = null;
        try {
            // MySQL
            // SQLite
            Class.forName("org.sqlite.JDBC");
            
            //set up string
            connection = DriverManager.getConnection("jdbc:sqlite:smoothie.db");
            
            
            Statement stmt = connection.createStatement();
            
            try {
                String drop = "DROP TABLE `smoothie`";
                stmt.executeUpdate(drop);
            } catch (Exception e) {
                //                    TODO: Log the error
            }
            String create = " CREATE TABLE `smoothie` (\n" +
                    "  `ID` bigint(20) NOT NULL,\n" +
                    "  `dateTime` timestamp NOT NULL DEFAULT CURRENT_TIMESTAMP,\n" +
                    "  `count` bigint(20) NOT NULL,\n" +
                    "  PRIMARY KEY (`ID`)\n" +
                    ");";
            stmt.executeUpdate(create);
        } catch (SQLException e) {
            log.error(e.toString());
            return false;
        } catch (ClassNotFoundException c) {
            log.error(c.toString());
            return false;
        } finally {
            try {
                connection.close();
            } catch (SQLException se) {
                log.error(se.toString());
            }
            
        }
        return true;
    }
    
    @Deprecated
    public boolean createLocationCacheTable() {
        
        try {
            Class.forName("com.mysql.jdbc.Driver");
            
            Connection connection = DriverManager.getConnection("jdbc:mysql://localhost:8889/red?" + "user=root&password=root");
            Statement stmt = connection.createStatement();
            
            try {
                String drop = "DROP TABLE `red`.`location_cache`";
                stmt.executeUpdate(drop);
            } catch (Exception e) {
                //                    TODO: Log the error
            }
            
            String create = "CREATE TABLE  `red`.`location_cache` (\n" +
                    "`id` BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY ,\n" +
                    "`location` VARCHAR( 500 ) NOT NULL ,\n" +
                    "`accuracy` SMALLINT( 2 ) NOT NULL ,\n " +
                    "`lat` FLOAT NOT NULL ,\n" +
                    "`lon` FLOAT NOT NULL\n" +
                    ") ENGINE = INNODB;";
            stmt.executeUpdate(create);
        } catch (SQLException e) {
            log.error(e.toString());
            return false;
        } catch (ClassNotFoundException c) {
            log.error(c.toString());
            return false;
        }
        return true;
    }
    
    
    public static boolean createDeleteRequestsTable() {
        Connection connection = null;
        try {
            // MySQL
            //Class.forName("com.mysql.jdbc.Driver");
            // SQLite
            Class.forName("org.sqlite.JDBC");
            
            //set up string
            //connection = DriverManager.getConnection("jdbc:mysql://localhost:8889/red?" + "user=root&password=root");
            connection = DriverManager.getConnection("jdbc:sqlite:delete_requests.db");
            
            
            Statement stmt = connection.createStatement();
            
            try {
                String drop = "DROP TABLE `delete_requests`";
                stmt.executeUpdate(drop);
            } catch (Exception e) {
                //                    TODO: Log the error
            }
            
            String create = "CREATE TABLE  `delete_requests` (\n" +
                    "`ID` BIGINT( 20 ) NOT NULL PRIMARY KEY ,\n" +
                    "`tweetID` BIGINT( 20 ) NOT NULL\n" +
                    ");";
            stmt.executeUpdate(create);
        } catch (SQLException e) {
            log.error(e.toString());
            return false;
        } catch (ClassNotFoundException c) {
            log.error(c.toString());
            return false;
        } finally {
            try {
                connection.close();
            } catch (SQLException se) {
                log.error(se.toString());
            }
        }
        return true;
    }
    
    public static boolean checkDatabaseExists(String fileName) {
        File file = new File(fileName + ".db");
        return file.exists();
    }
    
    public static void main(String[] argv) {
        createTweetsTable("tweets");
        createDeleteRequestsTable();
        
    }
    
    // Read database and convert to instances
    public static Instances findInstances(Date startDate, Date endDate, String tableName) {
        Instances data = null;
        InstanceQuery query = null;
        try {
            Class.forName("org.sqlite.JDBC");
            
            
            query = new InstanceQuery();
            //            query.setDatabaseURL("jdbc:mysql://localhost:8889/red");
            // TODO: check this
            query.setDatabaseURL("jdbc:sqlite:" + tableName + ".db");
            
            // TODO: make generic
            //            query.setUsername("root");
            //            query.setPassword("root");
            query.connectToDatabase();
            //MySQL
            //            query.setQuery("select x,y,ID,lat,lon from " + tableName + " where dateTime >= '" + new Timestamp(startDate.getTime()) + "' and dateTime <= '" + new Timestamp(endDate.getTime()) + "'");
            //SQLite
            query.setQuery("select x,y,ID,lat,lon from " + tableName + " where dateTime >= '" + startDate.getTime() + "' and dateTime <= '" + endDate.getTime() + "' order by random() limit 5000 ");
            //            query.setQuery("select x,y,ID,lat,lon from " + tableName);
            
            // setting spare data, hopefully this improves performance, but remember that 0 values are excluded
            //query.setSparseData(true);
            // Retrieve all the instances to be select by the SQL query, add them to an instances object
            data = query.retrieveInstances();
            log.info("Retived: " + data.numInstances() + " instances");
        } catch (Exception e) {
            log.error(e.toString());
        }
        finally {
            // Finally close off the query
            try{
                query.close();
            }
            catch(NullPointerException npe){
                log.error(npe.toString());
                
            }
        }
        return data;
    }

}
