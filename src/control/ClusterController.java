package control;

import cluster.*;
import util.DatabaseAdmin;
import util.DateUtil;
import weka.core.Instances;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Niall Panton
 *         Date: 10/09/2011
 *         Time: 17:49
 */
public class ClusterController implements Runnable {

    private int stepType, stepAmount;

    public ClusterController(int stepAmount, int stepType) {
        this.stepAmount = stepAmount;
        this.stepType = stepType;
    }


    public void run() {
        // Calculate current date +/- the time amount specified for the user to create a start and end date for queries
        Date now = new Date();
        Date then = DateUtil.alterDate(now, stepType, stepAmount);
        // retrieve instances
        // TODO: make attributes retrieved generic
        Instances instances = DatabaseAdmin.findInstances(then, now, "tweets");
        // TODO: possibly use word vector to process tweets into clustering
        // TODO: or make tweet clustering then second pass
        // When using xmeans use EM's number of clusters for speed then use EM periodically
//        XMeansCluster.cluster(instances);
//        System.out.println(XMeansCluster.cluster(instances));
        // TODO: Make threshold something like 1 person per sq/km
        HierarchicalCluster h = new HierarchicalCluster(instances, 50);
        Tree t = h.clusterToTree();
        System.out.println(t.toString());
        toFile(t.toJSONAlt(null, null), "");
//        // TODO: fix exit properly
        System.exit(0);
    }

    private void toFile(String resultJSON, String fileName) {
        try {
            Calendar now = Calendar.getInstance();
            FileWriter fstream = null;
            if (fileName.equals(""))
                fstream = new FileWriter(now.get(Calendar.DAY_OF_MONTH) + "-" + (now.get(Calendar.MONTH) + 1) + "-" + now.get(Calendar.YEAR) + ".json");
            else
                fstream = new FileWriter(fileName);

            BufferedWriter out = new BufferedWriter(fstream);
            out.write(resultJSON);
            out.close();
        } catch (IOException i) {
            i.printStackTrace();
        }
    }

    public static void main(String[] args) {
        ClusterController cc = new ClusterController(-5, 1);
        Thread t = new Thread(cc);
        t.start();
    }

}
