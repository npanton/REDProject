package cluster;

import com.sun.jndi.dns.DnsName;
import weka.core.Instances;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


/**
 * @author Niall Panton
 *         Date: 10/09/2011
 *         Time: 18:19
 */
public class HierarchicalCluster {

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
        ExecutorService exec = Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors());
        //Test Without multithreading
//        ExecutorService exec = Executors.newFixedThreadPool(1);
        // TODO: does the new ArrayList mean we cannot access tweets ids?
        Node root = new Node("Root-node0", new ArrayList<String>(), instancesIn, 0);
        tree.setRootElement(root);
        // Do the initial clustering to create a group of labeled instances
        Node clusteredInstances = EMCluster.cluster(root, 0);

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
                // TODO: handle with logger
                i.printStackTrace();
            }
            // TODO: get sub-branches in turn and add to root
            for (BranchThread branch : branchThreads) {
                if (branch.getBranch().getInstances().numInstances() > branchThreshold) {
                    root.addChild(branch.getBranch());
                }
            }
        }
        // TODO: logger
        System.out.println("Tree build time: " + (System.currentTimeMillis() - start) / 1000 + "s\n");
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
                Node clusteredInstances = EMCluster.cluster(subTree, depth);
                // EM allows for a single cluster to be returned so account fo this possibility
                if (clusteredInstances.getChildren().size() > 1) {
                    for (Node cluster : clusteredInstances.getChildren()) {
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

        public Integer call() {
            buildTree(subTree, 1);
            return 1;
        }

    }


}
