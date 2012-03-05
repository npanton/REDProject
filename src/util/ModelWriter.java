package util;

import weka.core.Instances;
import weka.experiment.InstanceQuery;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;

/**
 * @author Niall Panton
 *         Date: 20/09/2011
 *         Time: 20:48
 */
public class ModelWriter {
    public static void main(String[] args) {
        Instances data = null;
        InstanceQuery query = null;
        try {
            query = new InstanceQuery();
            query.setDatabaseURL("jdbc:mysql://localhost:8889/red");
            // TODO: make generic
            query.setUsername("root");
            query.setPassword("root");
            query.connectToDatabase();
//            query.setQuery("select ID, dateTime, lat, lon, x, y, tweet from red." + tableName + " where dateTime >= " + startDate.getTime() + " and dateTime <= " + endDate.getTime());
            query.setQuery("select * from red.tweets order by rand() limit 10000");
            // setting spare data, hopefully this improves performance, but remember that 0 values are excluded
            //query.setSparseData(true);
            // Retrieve all the instances to be select by the SQL query, add them to an instances object
            data = query.retrieveInstances();

        } catch (Exception e) {
            //TODO: Handle me
        } finally {
            // Finally close off the query
            try {
                query.close();
            } catch (NullPointerException npe) {
                //TODO: Handle me
            }
        }
        try {
            Calendar now = Calendar.getInstance();
            FileWriter fstream = null;

            fstream = new FileWriter(now.get(Calendar.DAY_OF_MONTH) + "-" + (now.get(Calendar.MONTH) + 1) + "-" + now.get(Calendar.YEAR) + "model.arff");

            BufferedWriter out = new BufferedWriter(fstream);
            out.write(data.toString());
            out.close();
        } catch (IOException i) {
            i.printStackTrace();
        }

    }
}
