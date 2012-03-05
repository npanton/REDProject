package cluster;

import weka.clusterers.EM;
import weka.clusterers.XMeans;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Niall Panton
 *         Date: 13/09/2011
 *         Time: 23:01
 */
public class EMCluster {

    public static Node cluster(Node currentNode, int nodeId) {
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


            // Set the bounds for the number of clusters to cluster to
            // TODO: replace this
//            String[] bounds = calcClusterBounds(instancesIn.numInstances());
            // Set all other clustering options
            em.setMaxIterations(100);
            em.setNumClusters(-1);
            em.setMinStdDev(0.0001d);
            em.setSeed(100);
            // Build the Expectation Maximisation clusterer
            em.buildClusterer(instances);

            // Get the number of newly created clusters and the actual cluster assignments
            int[] clusters = em.getInstances();
            int numClusters = em.numberOfClusters();


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
//            For each cluster, add it to the tree node to be returned as a child leaf node, providing it has instances
            for (int c = 0; c < numClusters; c++) {
                if (idArray.get(c).size() > 0){
                    currentNode.addChild(new Node(nodeId + "-node" + nodeId + "" + c, idArray.get(c), instancesList.get(c), nodeId));
                    clustersOut.add(instancesList.get(c));
                }
            }

        } catch (Exception xe) {
            xe.printStackTrace();
        }
//        System.out.println("Cluster execution time: " + (System.currentTimeMillis()-start) + "ms\tNode size: "+ numInstIn +"\n");
        return currentNode;
    }

}
