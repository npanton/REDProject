package clusterer;

import java.util.ArrayList;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import redutils.GeoUtil;
import weka.core.Instances;


/**
 * @author Niall Panton
 *         Date: 10/09/2011
 *         Time: 18:19
 */
public class HierarchicalCluster {
    private static org.apache.log4j.Logger log = Logger.getLogger(HierarchicalCluster.class);
    private Instances instancesIn;
    private int branchThreshold;
    private Tree tree;
    
    public HierarchicalCluster(Instances instancesIn, int branchThreshold) {
        this.instancesIn = instancesIn;
        this.branchThreshold = branchThreshold;
        tree = new Tree();
    }
    
    public Tree clusterToTree() {
       
        long start = System.currentTimeMillis();
        
        ArrayList<BranchThread> branchThreads = new ArrayList<BranchThread>();
        
        // If we are at depth zero, create a new thread pool for clustering the instances
//        ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        ExecutorService exec = Executors.newFixedThreadPool(1);

        //Test Without multithreading
        // ExecutorService exec = Executors.newFixedThreadPool(1);
        // TODO: does the new ArrayList mean we cannot access tweets ids?
        // TODO: Performance monitor
        long hullStart = System.currentTimeMillis();
        
        
        log.info("Creating top level convex hull");
        GrahamScan g = new GrahamScan(GeoUtil.instancesToQuads(instancesIn));
        Thread t = new Thread(g);
        t.start();
        try {
            t.join();
        } catch (Exception e) {
            log.error("Cannot create convex hull: " + ExceptionUtils.getStackTrace(e));
        }
        log.info("Top level hull created in: " + (System.currentTimeMillis()-start)/1000 + "s");
        
        
        
        Node root = new Node("Root-node0", new ArrayList<String>(), instancesIn, 0, g.getIDs(), instancesIn.numInstances());
//        Node root = new Node("Root-node0", new ArrayList<String>(), instancesIn, 0, null, instancesIn.numInstances());

        tree.setRootElement(root);
        

        Node rootCopy = new Node("Root-node0", new ArrayList<String>(), instancesIn, 0, g.getIDs(), instancesIn.numInstances());
//        Node rootCopy = new Node("Root-node0", new ArrayList<String>(), instancesIn, 0, null, instancesIn.numInstances());

        // Do the initial clustering to create a group of labeled instances
//        Node clusteredInstances = EMCluster.cluster(rootCopy, 0, true);
        Node clusteredInstances = XMeansCluster.cluster(rootCopy, 0, true);

        // Using each cluster, if it is populous enough and there are more than 1 clusters, recursively cluster it
        if (clusteredInstances.getChildren().size() > 1) {
            for (Node cluster : clusteredInstances.getChildren()) {
                if (cluster.getInstances().numInstances() > branchThreshold) {

                    BranchThread b = new BranchThread(cluster);
                    branchThreads.add(b);
                }
            }
            

            // Invoke all branchThreads which will run recursively to build the tree
            try {
                exec.invokeAll(branchThreads);
            } catch (InterruptedException i) {
                log.error("Cannot start sub-clusterer: " + ExceptionUtils.getStackTrace(i));
            }
            // Get sub-branches in turn and add to root
            for (BranchThread branch : branchThreads) {
                if (branch.getBranch().getInstances().numInstances() > branchThreshold) {
                    root.addChild(branch.getBranch());
                }
            }
            exec.shutdown();
        }

        // TODO: logger
        log.info("Tree build time: " + (System.currentTimeMillis() - start) / 1000 + "s\n");
        return tree;
    }
    
protected class BranchThread implements Callable<Integer> {
    
    private Node subTree;
    
    private BranchThread(Node subTree) {
        this.subTree = subTree;
    }
    
    private void buildTree(Node subTree, int depth) {
        // If the instances supplied are populous enough then cluster it
        if (subTree.getInstances().numInstances() > branchThreshold) {
//            Node innerInstances = EMCluster.cluster(subTree, depth, false);
            Node innerInstances = XMeansCluster.cluster(subTree, depth, false);

            // EM allows for a single cluster to be returned so account fo this possibility
            if (innerInstances.getChildren().size() > 1) {
                for (Node cluster : innerInstances.getChildren()) {
                    if (cluster.getInstances().numInstances() > branchThreshold) {
                        buildTree(cluster, ++depth);
                    }
                }
            }
        }
    }
    
    // Return newly created branch to tree node , called by parent class
    public Node getBranch() {
        return subTree;
    }
    
    @Override
    public Integer call() {
        buildTree(subTree, 1);
        return 1;
    }
    
}
    

}
