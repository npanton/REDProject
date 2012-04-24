package clusterer;

import java.util.ArrayList;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import redutils.GeoUtil;
import weka.clusterers.XMeans;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.InterquartileRange;
import weka.filters.unsupervised.attribute.Remove;

/**
 * @author Niall Panton
 *         Date: 13/09/2011
 *         Time: 22:17
 */
public class XMeansCluster {
    private static org.apache.log4j.Logger log = Logger.getLogger(XMeansCluster.class);

    public static Node cluster(Node currentNode, int nodeId, boolean init) {
        XMeans xm = new XMeans();
          ArrayList<Instances> clustersOut = new ArrayList<Instances>();
        long start = System.currentTimeMillis();
                Instances instancesIn = currentNode.getInstances();

        try {
            // Remove the id attribute for clustering
            String[] options = new String[2];
            // TODO: make generic is applicable
            options[0] = "-R";
            options[1] = "3-5";
            Remove remove = new Remove();
            remove.setOptions(options);

            remove.setInputFormat(instancesIn);
            // Execute the filter
            Instances instances = Filter.useFilter(instancesIn, remove);


            // Set the bounds for the number of clusters to cluster to
            // TODO: replace this
            String[] bounds = decideBonds(instancesIn.numInstances());
            // Set all other clustering options
            // TODO: some have default values and are not needed
            String[] clusterOptions = new String[20];
            clusterOptions[0] = "-L";
            clusterOptions[1] = "3";
            clusterOptions[2] = "-H";
            clusterOptions[3] = "5";
            clusterOptions[4] = "-D";
            clusterOptions[5] = "\"weka.core.EuclideanDistance\"";
            clusterOptions[6] = "-R";
            clusterOptions[7] = "first-last";
            clusterOptions[8] = "-M";
            clusterOptions[9] = "1000";
            clusterOptions[10] = "-J";
            clusterOptions[11] = "1000";
            clusterOptions[12] = "-C";
            clusterOptions[13] = "0.5";
            clusterOptions[14] = "-B";
            clusterOptions[15] = "1.0";
            clusterOptions[16] = "-last";
            clusterOptions[17] = "1";
            clusterOptions[18] = "-S";
            clusterOptions[19] = "-1";

            // Build the XMeans clusters
            xm.setOptions(clusterOptions);
            xm.buildClusterer(instances);
            // Get the number of newly created clusters and the actual cluster assignments
            int[] clusters = xm.getClusters();
            int numClusters = xm.numberOfClusters();


            // Create an array of Instances to store the Instances clustered
            ArrayList<ArrayList<String>> idArray = new ArrayList<ArrayList<String>>();
            ArrayList<Instances> instancesList = new ArrayList<Instances>();

            // create numClusters many child nodes for this current node
            for (int c = 0; c < numClusters; c++) {
                idArray.add(new ArrayList<String>());
                instancesList.add(new Instances(instancesIn, -1));
            }
            // Incrementally build nodes at this point
            for (int i = 0; i < instances.numInstances(); i++) {
                idArray.get(clusters[i]).add(instancesIn.instance(i).stringValue(instancesIn.instance(i).attribute(2)));
                instancesList.get(clusters[i]).add(instancesIn.instance(i));
            }
            //For each cluster, add it to the tree node to be returned as a child leaf node, providing it has instances
            for (int c = 0; c < numClusters; c++) {
                if (idArray.get(c).size() > 0){

                      // TODO: Fix me
                    //Instances filtered = fileterOutliers(instancesList.get(c));
                    //GrahamScan g = new GrahamScan(GeoUtil.instancesToQuads(instancesList.get(c)));
                    log.info("Instances as Set: " + GeoUtil.instancesToQuads(instancesList.get(c)).size() );
                    log.info("Instances as Lst: " + (instancesList.get(c)).numInstances() );

                    GrahamScan g = new GrahamScan(GeoUtil.instancesToQuads(instancesList.get(c)));
                    
                    Thread t = new Thread(g);
                    t.start();
                    t.join();
                    currentNode.addChild(new Node(nodeId + "-node" + nodeId + "" + c, idArray.get(c), instancesList.get(c), nodeId, g.getIDs(), instancesList.get(c).numInstances()));
                    
                    
                    clustersOut.add(instancesList.get(c));

                }
            }

        } catch (Exception xe) {
            log.error("Could not cluster instances: " + ExceptionUtils.getStackTrace(xe));
        }
        log.debug("Cluster execution time: " + (System.currentTimeMillis()-start) + "ms\n");
        return currentNode;
    }
       public static Instances fileterOutliers(Instances in){
        log.info("Instances in to filter: " + in.numInstances());
        Instances instances;
        Instances instancesOut = null;
        try {
            // TODO: Chack new code
            InterquartileRange ir = new InterquartileRange();
            ir.setAttributeIndices("1-2");
            ir.setExtremeValuesAsOutliers(true);
            ir.setInputFormat(in);
            // Execute the filter
            instances = Filter.useFilter(in, ir);
            for(int i = 0; i < instances.numInstances(); i++){
                if(instances.instance(i).value(5) == 1.0d){
                    instances.delete(i);
                }
            }
            
            String[] options = new String[2];
            options[0] = "-R";
            options[1] = "6-7";
            Remove remove = new Remove();
            remove.setOptions(options);
            
            remove.setInputFormat(instances);
            // Execute the filter
            instancesOut = Filter.useFilter(instances, remove);
            
            
        } catch (Exception ex) {
            log.error("Could not filter instances:\n" + ExceptionUtils.getStackTrace(ex));
        }
        log.info("Instances in to filter: " + instancesOut.numInstances());
        return instancesOut;
    }

    private static String[] decideBonds(int numInstances) {
        String[] out = new String[2];
        out[0] = "4";
        out[1] = "4";
        return out;
    }
 
}
