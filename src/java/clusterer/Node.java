package clusterer;


import java.util.ArrayList;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONException;
import weka.core.Instances;

/**
 * Represents a node of the Tree<Pair> class. The Node<Pair> is also a container, and
 * can be thought of as instrumentation to determine the location of the type T
 * in the Tree<Pair>.
 */
public class Node {
    //    private Pair data;
    private String id = null;
    private ArrayList<String> nodeData = null;
    private List<Node> children;
    private ArrayList<Quad> hullIds;
    private Instances inst;
    private int depth, high, low, size;
    
    public Node(String i, ArrayList<String> nd, ArrayList<Quad> h, Instances in, int d, int hi, int lo) {
        id = i;
        nodeData = nd;
        hullIds = h;
        inst = in;
        depth = d;
        high = hi;
        low = lo;
    }
    
    public Node(String i, ArrayList<String> nd, Instances in, int d) {
        id = i;
        nodeData = nd;
        inst = in;
        depth = d;
    }
    
    public Node(String i, ArrayList<String> nd, Instances in, int d, ArrayList<Quad> h, int s) {
        id = i;
        hullIds = h;
        nodeData = nd;
        inst = in;
        depth = d;
        size = s;
    }
    
        public int size() {
        return size;
    }
    
    public int getDepth() {
        return depth;
    }
    
    
    /**
     * Return the children of Node<Pair>. The Tree<Pair> is represented by a single
     * root Node<Pair> whose children are represented by a List<Node<Pair>>. Each of
     * these Node<Pair> elements in the List can have children. The getChildren()
     * method will return the children of a Node<Pair>.
     *
     * @return the children of Node<Pair>
     */
    public List<Node> getChildren() {
        if (children == null) {
            return new ArrayList<Node>();
        }
        return children;
    }
    
    public Node getChild(int i) {
        return children.get(i);
    }
    
    //    /**
    //     * Returns the number of immediate children of this Node<Pair>.
    //     * @return the number of immediate children.
    //     */
    //    public int getNumberOfChildren() {
    //        if (children == null) {
    //            return 0;
    //        }
    //        return children.size();
    //    }
    
    /**
     * Adds a child to the list of children for this Node<Pair>. The addition of
     * the first child will create a new List<Node<Pair>>.
     *
     * @param child a Node<Pair> object to set.
     */
    public void addChild(Node child) {
        if (children == null) {
            children = new ArrayList<Node>();
        }
        children.add(child);
    }
    
    public ArrayList<String> getData() {
        return nodeData;
    }
    
    public Instances getInstances() {
        return inst;
    }
    
    public ArrayList<Quad> getHullIDs() {
        return hullIds;
    }
    
    public JSONArray getHullJSON() {
        JSONArray j = new JSONArray();
        for (Quad q : hullIds) {
            JSONArray inner = new JSONArray();
            try {
                inner.put(q.getxC());
                inner.put(q.getyC());
                inner.put(q.getLon());
                inner.put(q.getLat());
                j.put(inner);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        
        return j;
    }
    
    public void setData(ArrayList<String> d) {
        nodeData = d;
    }
    
    public String getID() {
        return id;
    }
    
    public void setID(String i) {
        id = i;
    }
    
    //    public String toString() {
    //        StringBuilder sb = new StringBuilder();
    //        sb.append("{").append(getData().toString()).append("[");
    //        int i = 0;
    //        for (Node e : getChildren()) {
    //            if (i > 0) {
    //                sb.append(",\n");
    //            }
    //            sb.append(e.getData().toString());
    //            i++;
    //        }
    //        sb.append("]").append("}");
    //        return sb.toString();
    //    }
    
    public int getHigh() {
        return high;
    }
    
    public void setHigh(int high) {
        this.high = high;
    }
    
    public int getLow() {
        return low;
    }
    
    public void setLow(int low) {
        this.low = low;
    }
}