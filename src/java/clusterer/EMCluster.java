package clusterer;

import java.util.ArrayList;
import java.util.Random;
import org.apache.log4j.Logger;
import redutils.GeoUtil;
import weka.clusterers.EM;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.InterquartileRange;
import weka.filters.unsupervised.attribute.Remove;

/**
 * @author Niall Panton
 *         Date: 13/09/2011
 *         Time: 23:01
 */
public class EMCluster {
    private static org.apache.log4j.Logger log = Logger.getLogger(EMCluster.class);
    
    // If init is useful, use depth value instead
    public static Node cluster(Node currentNode, int nodeId, boolean init) {
        log.info("Clustering node: " + nodeId);
        long start = System.currentTimeMillis();
        Instances instancesIn = currentNode.getInstances();
        EM em = new EM();
        //        HashMap<Integer, Instances> clustersOut = new HashMap<Integer, Instances>();
        ArrayList<Instances> clustersOut = new ArrayList<Instances>();
        //TODO: debug
        //        long start = System.currentTimeMillis();
        //        int numInstIn = currentNode.getInstances().numInstances();
        try {
            // Remove the id attribute for clustering
            // TODO: some have default values and are not needed
            String[] options = new String[2];
            options[0] = "-R";
            options[1] = "3-5";
            Remove remove = new Remove();
            remove.setOptions(options);
            
            remove.setInputFormat(instancesIn);
            // Execute the filter
            Instances instances = Filter.useFilter(instancesIn, remove);
            
            
            
            // Set  clustering options
            em.setMaxIterations(5);
            //em.setNumClusters(-1);
            em.setNumClusters(4);
            
            // TODO set granularity based on depth
            if(init){
                em.setMinStdDev(50000d);
            }
            else{
                em.setMinStdDev(50000d);
            }
            Random r = new Random();
            em.setSeed(r.nextInt());
            log.debug("Building clusters");
            // Build the Expectation Maximisation clusterer
            em.buildClusterer(instances);
            log.debug("Clusters built");
            log.info("Cluster execution time: " + (System.currentTimeMillis()-start) + "ms for cluster id: "+ nodeId +"t");
            start = System.currentTimeMillis();
            // Get the number of newly created clusters and the actual cluster assignments
            int[] clusters = em.getInstances();
            int numClusters = em.numberOfClusters();
            
            
            // Create an array of Instances to store the Instances clustered
            ArrayList<ArrayList<String>> idArray = new ArrayList<ArrayList<String>>();
            ArrayList<Instances> instancesList = new ArrayList<Instances>();
            
            // create numClusters many child nodes for this current (parent) node
            for (int c = 0; c < numClusters; c++) {
                idArray.add(new ArrayList<String>());
                instancesList.add(new Instances(instancesIn, -1));
            }
            // Incrementally build nodes at this point, using the new labeled cluster results
            for (int i = 0; i < instances.numInstances(); i++) {
                idArray.get(clusters[i]).add(instancesIn.instance(i).stringValue(instancesIn.instance(i).attribute(2)));
                instancesList.get(clusters[i]).add(instancesIn.instance(i));
            }
            // For each cluster, add it to the tree node to be returned as a child leaf node, providing it has instances
            for (int c = 0; c < numClusters; c++) {
                if (idArray.get(c).size() > 0){
                    //                    System.out.println("Instances in: " + instancesList.get(c).numInstances() + " Depth: " + nodeId + " Clusters: " + numClusters + "\n\n");
                    
                    Instances filtered = fileterOutliers(instancesList.get(c));
                    
                    
                    //GrahamScan g = new GrahamScan(GeoUtil.instancesToQuads(instancesList.get(c)));
                    GrahamScan g = new GrahamScan(GeoUtil.instancesToQuads(filtered));
                    
                    Thread t = new Thread(g);
                    t.start();
                    t.join();
                    currentNode.addChild(new Node(nodeId + "-node" + nodeId + "" + c, idArray.get(c), filtered, nodeId, g.getIDs(), filtered.numInstances()));
                    
                    //currentNode.addChild(new Node(nodeId + "-node" + nodeId + "" + c, idArray.get(c), instancesList.get(c), nodeId, g.getIDs(), instancesList.get(c).numInstances()));
                    
                    clustersOut.add(instancesList.get(c));
                }
            }
            
        } catch (Exception xe) {
            log.error("Could not cluster instances: " + xe.getMessage());
        }
        System.out.println("All execution time: " + (System.currentTimeMillis()-start) + "ms\t" +"\n Clusters out: " + currentNode.getChildren().size());
        
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
            log.error("Could not filter instances ");
        }
        log.info("Instances in to filter: " + instancesOut.numInstances());
        return instancesOut;
    }

}
