package clusterer;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Calendar;
import java.util.Date;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import redutils.DatabaseUtil;
import redutils.DateUtil;
import weka.core.Instances;

/**
 * @author Niall Panton
 *         Date: 10/09/2011
 *         Time: 17:49
 */
public class ClusterController implements Runnable {
    private static org.apache.log4j.Logger log = Logger.getLogger(ClusterController.class);
    
    private int stepType, stepAmount;
    
    public ClusterController(int stepAmount, int stepType) {
        this.stepAmount = stepAmount;
        this.stepType = stepType;
    }
    
    
    @Override
    public void run() {
        // Calculate current date +/- the time amount specified for the user to create a start and end date for queries
        Date now = new Date();
        Date then = DateUtil.alterDate(now, stepType, stepAmount);
        // retrieve instances
        // TODO: make attributes retrieved generic
        Instances instances = DatabaseUtil.findInstances(then, now, "tweets");
        // TODO: possibly use word vector to process tweets into clustering
        // TODO: or make tweet clustering then second pass
        // When using xmeans use EM's number of clusters for speed then use EM periodically
        // XMeansCluster.cluster(instances);
        // System.out.println(XMeansCluster.cluster(instances));
        // TODO: Make threshold something like 1 person per sq/km
        HierarchicalCluster h = new HierarchicalCluster(instances, 50);
        Tree t = h.clusterToTree();
        toFile(t.toJSONAlt(null, null), "test.json");
        // TODO: fix exit properly, possibly fixed
        // System.exit(0);
    }
    
    private void toFile(String resultJSON, String fileName) {
        try {
            Calendar now = Calendar.getInstance();
            FileWriter fstream;
            if (fileName.equals(""))
                fstream = new FileWriter(now.get(Calendar.DAY_OF_MONTH) + "-" + (now.get(Calendar.MONTH) + 1) + "-" + now.get(Calendar.YEAR) + ".json");
            else
                fstream = new FileWriter(fileName);
            
            BufferedWriter out = new BufferedWriter(fstream);
            out.write(resultJSON);
            out.close();
        } catch (IOException i) {
            log.error("Could not write JSON file: " + ExceptionUtils.getStackTrace(i));
        }
    }
    
    public static void main(String[] args) {
//        ClusterController cc = new ClusterController(-20, 3);
                ClusterController cc = new ClusterController(-11, 4);
        Thread t = new Thread(cc);
        t.start();
    }

}
