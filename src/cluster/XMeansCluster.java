package cluster;

import weka.clusterers.XMeans;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Remove;

import java.util.ArrayList;
import java.util.HashMap;

/**
 * @author Niall Panton
 *         Date: 13/09/2011
 *         Time: 22:17
 */
public class XMeansCluster {


    public static ArrayList<Instances> cluster(Instances instancesIn) {
        XMeans xm = new XMeans();
//        HashMap<Integer, Instances> clustersOut = new HashMap<Integer, Instances>();
          ArrayList<Instances> clustersOut = new ArrayList<Instances>();
        long start = System.currentTimeMillis();

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
//            String[] bounds = calcClusterBounds(instancesIn.numInstances());
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
//            For each cluster, add it to the tree node to be returned as a child leaf node, providing it has instances
            for (int c = 0; c < numClusters; c++) {
                if (idArray.get(c).size() > 0){
//                    clustersOut.put(c, instancesList.get(c));
                    clustersOut.add(instancesList.get(c));

                }
            }

        } catch (Exception xe) {
            xe.printStackTrace();
        }
//        System.out.println("Cluster execution time: " + (System.currentTimeMillis()-start) + "ms\n");
        return clustersOut;
    }
}
