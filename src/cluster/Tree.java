package cluster;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import java.util.*;


public class Tree {
    private Node rootElement;
    private String outString = "";
    private JSONObject outJson = new JSONObject();
    private JSONObject globe = new JSONObject();
    private HashMap<String, String> cache = new HashMap<String, String>();
    /**
     * Default ctor.
     */
    public Tree() {
        super();
    }

    /**
     * Return the root Node of the tree.
     *
     * @return the root element.
     */
    public Node getRootElement() {
        return this.rootElement;
    }

    public String getGlobe() {
        return globe.toString();
    }

    /**
     * Set the root Element for the tree.
     *
     * @param rootElement the root element to set.
     */
    public void setRootElement(Node rootElement) {
        this.rootElement = rootElement;
    }

    /**
     * Returns the Tree<Pair> as a List of Node<Pair> objects. The elements of the
     * List are generated from a pre-order traversal of the tree.
     *
     * @return a List<Node<Pair>>.
     */
    public List<Node> toList() {
        List<Node> list = new ArrayList<Node>();
        walk(rootElement, list);
        return list;
    }

    /**
     * Returns a String representation of the Tree. The elements are generated
     * from a pre-order traversal of the Tree.
     *
     * @return the String representation of the Tree.
     */
    public String toString() {
        return toPrettyString(null, 0);
    }

    /**
     * Walks the Tree in pre-order style. This is a recursive method, and is
     * called from the toList() method with the root element as the first
     * argument. It appends to the second argument, which is passed by reference     * as it recurses down the tree.
     *
     * @param element the starting element.
     * @param list    the output of the walk.
     */
    private void walk(Node element, List<Node> list) {
        list.add(element);
        for (Node data : element.getChildren()) {
            walk(data, list);
        }
    }

    // Serialize Instances object
    public String toJSON(Node currentNode, JSONObject currentObject) {
        Random r = new Random();

        try {
            if (currentNode == null) {
                currentObject = new JSONObject();
                currentNode = rootElement;
                currentObject.put("id", currentNode.getID() + "_" + currentNode.getData().size() + "" + r.nextInt(100));
                currentObject.put("name", currentNode.getData().size());
                currentObject.put("high", currentNode.getHigh());
                currentObject.put("low", currentNode.getLow());
                currentObject.put("depth", currentNode.getDepth());

                if (currentNode.getHullIDs().size() > 0) {
                    double[] vals = getLatLngCenter(currentNode.getHullIDs());
                    if(cache.containsKey(vals[0] + "," + vals[1]))
                        currentObject.put("place_name", cache.get(vals[0] + "," + vals[1]));
                        else
                    currentObject.put("place_name", reverseGeocode(vals[0] + "," + vals[1]));
                    if (Math.round(vals[0]) != 0 && Math.round(vals[1]) != 0) {
                        globe.append(Integer.toString(currentNode.getDepth()), Math.round(vals[0]));
                        globe.append(Integer.toString(currentNode.getDepth()), Math.round(vals[1]));
                        globe.append(Integer.toString(currentNode.getDepth()), currentNode.getData().size());
                    }
                }
//                    currentObject.put("data", new JSONArray(currentNode.getData()));
                currentObject.put("hull", currentNode.getHullJSON());
                currentObject.put("size", currentNode.getData().size());
            }

            JSONArray allChildren = new JSONArray();
            for (Node data : currentNode.getChildren()) {
                JSONObject childNode = new JSONObject();
                childNode.put("id", data.getID() + "_" + data.getData().size() + "" + r.nextInt(10000));
                childNode.put("name", data.getData().size());
                childNode.put("high", data.getHigh());
                childNode.put("low", data.getLow());
//                childNode.put("data", new JSONArray(data.getData()));
                childNode.put("size", data.getData().size());
                childNode.put("depth", data.getDepth());

                childNode.put("hull", data.getHullJSON());


                if (data.getHullIDs().size() > 0) {
                    double[] vals = getLatLngCenter(data.getHullIDs());
                    if(cache.containsKey(vals[0] + "," + vals[1]))
                        childNode.put("place_name", cache.get(vals[0] + "," + vals[1]));
                        else
                    childNode.put("place_name", reverseGeocode(vals[0] + "," + vals[1]));
                    if (Math.round(vals[0]) != 0 && Math.round(vals[1]) != 0) {
                        globe.append(Integer.toString(data.getDepth()), Math.round(vals[0]));
                        globe.append(Integer.toString(data.getDepth()), Math.round(vals[1]));
                        globe.append(Integer.toString(data.getDepth()), data.getData().size());
                    }
                }
                allChildren.put(childNode);
                toJSON(data, childNode);
            }
            outJson = currentObject.put("children", allChildren);

        } catch (JSONException j) {
            j.printStackTrace();
            return outJson.toString();
        }

        return outJson.toString();
    }


    public String toJSONAlt(Node currentNode, JSONObject currentObject) {
        Random r = new Random();

        try {
            if (currentNode == null) {
                currentObject = new JSONObject();
                currentNode = rootElement;
                currentObject.put("id", currentNode.getID() + "_" + currentNode.getData().size() + "" + r.nextInt(100));
//                currentObject.put("name", currentNode.getData().size());
//                currentObject.put("high", currentNode.getHigh());
//                currentObject.put("low", currentNode.getLow());
                currentObject.put("depth", currentNode.getDepth());

//                if (currentNode.getHullIDs().size() > 0) {
//                    double[] vals = getLatLngCenter(currentNode.getHullIDs());
//                    if(cache.containsKey(vals[0] + "," + vals[1]))
//                        currentObject.put("place_name", cache.get(vals[0] + "," + vals[1]));
//                        else
//                    currentObject.put("place_name", reverseGeocode(vals[0] + "," + vals[1]));
//                    if (Math.round(vals[0]) != 0 && Math.round(vals[1]) != 0) {
//                        globe.append(Integer.toString(currentNode.getDepth()), Math.round(vals[0]));
//                        globe.append(Integer.toString(currentNode.getDepth()), Math.round(vals[1]));
//                        globe.append(Integer.toString(currentNode.getDepth()), currentNode.getData().size());
//                    }
//                }
                    currentObject.put("data", new JSONArray(currentNode.getData()));
//                currentObject.put("hull", currentNode.getHullJSON());
                currentObject.put("size", currentNode.getData().size());
            }

            JSONArray allChildren = new JSONArray();
            for (Node data : currentNode.getChildren()) {
                JSONObject childNode = new JSONObject();
                childNode.put("id", data.getID() + "_" + data.getData().size() + "" + r.nextInt(10000));
                childNode.put("name", data.getData().size());
//                childNode.put("high", data.getHigh());
//                childNode.put("low", data.getLow());
                childNode.put("data", new JSONArray(data.getData()));
//                childNode.put("size", data.getData().size());
                childNode.put("depth", data.getDepth());

//                childNode.put("hull", data.getHullJSON());


//                if (data.getHullIDs().size() > 0) {
//                    double[] vals = getLatLngCenter(data.getHullIDs());
//                    if(cache.containsKey(vals[0] + "," + vals[1]))
//                        childNode.put("place_name", cache.get(vals[0] + "," + vals[1]));
//                        else
//                    childNode.put("place_name", reverseGeocode(vals[0] + "," + vals[1]));
//                    if (Math.round(vals[0]) != 0 && Math.round(vals[1]) != 0) {
//                        globe.append(Integer.toString(data.getDepth()), Math.round(vals[0]));
//                        globe.append(Integer.toString(data.getDepth()), Math.round(vals[1]));
//                        globe.append(Integer.toString(data.getDepth()), data.getData().size());
//                    }
//                }
                allChildren.put(childNode);
                toJSONAlt(data, childNode);
            }
            outJson = currentObject.put("children", allChildren);

        } catch (JSONException j) {
            j.printStackTrace();
            return outJson.toString();
        }

        return outJson.toString();
    }


    private String toPrettyString(Node current, int depth) {
        if (current == null) {
            current = rootElement;
        }

        for (int i = 0; i < depth; i++)
            outString += "|\t";
//        outString += current.getID() + ", " + current.getData().size() + " Instances " + "High: " + current.getHigh() + ", Low: " + current.getLow() + " Depth: " + current.getDepth() + "\n";
//        outString += current.getID() + ", " + current.getData().size() + " Instances " + current.getData() + "\n";
        outString += current.getID() + ", " + current.getInstances().numInstances() + "\n";
        for (Node data : current.getChildren()) {
            for (int i = 0; i <= depth; i++)
                outString += "|\t";

            int depthNew = depth + 1;
            toPrettyString(data, depthNew);
        }
        return outString;
    }

    private double[] getLatLngCenter(ArrayList<Quad> latlng) {
        double[] returnVal = new double[2];
        double n = (double) latlng.size();
        int lat = 0, lng = 0;
        for (int i = 0; i < n; i++) {
            Quad vert = latlng.get(i);
            lat += vert.getLat();
            lng += vert.getLon();
        }
        returnVal[0] = (lat / n);
        returnVal[1] = (lng / n);

        return returnVal;
    }

    private String reverseGeocode(String latLon) {
        String addressOut = "";
        return "US";
//        JSONObject results = null;
//        try {
//            HttpURLConnection con = (HttpURLConnection) new URL("http://maps.google.com/maps/api/geocode/json?latlng=" + latLon + "&sensor=false").openConnection();
//            con.setRequestMethod("GET");
//            BufferedReader reader = new BufferedReader(new InputStreamReader(con.getInputStream()));
//            String out = "", line = null;
//            while ((line = reader.readLine()) != null) {
//                out += line;
//            }
//            results = new JSONObject(out);
//            System.out.println(results);
//            if(!results.getString("status").equalsIgnoreCase("ZERO_RESULTS")){
//            JSONArray addressDetails = results.getJSONArray("results").getJSONObject(0).getJSONArray("address_components");
//            for(int i = 0; i < addressDetails.length(); i++){
//                JSONArray inner =  addressDetails.getJSONObject(i).getJSONArray("types");
//                for(int j = 0; j < inner.length(); j++){
//                    if(inner.getString(j).equalsIgnoreCase("sublocality") || inner.getString(j).equalsIgnoreCase("locality") || inner.getString(j).equalsIgnoreCase("administrative_area_level_1")){
//                        addressOut += " "  + addressDetails.getJSONObject(i).getString("long_name");
//                    }
//                }
//            }
//                cache.put(latLon, addressOut);
//            }
//            else
//                addressOut = "USA";
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//        try {
//            Thread.sleep(110);
//        } catch (InterruptedException i) {
//        }
//        return addressOut;
    }

}