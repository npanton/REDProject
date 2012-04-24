package clusterer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Random;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.log4j.Logger;

public class GrahamScan implements Runnable {
    private static org.apache.log4j.Logger log = Logger.getLogger(GrahamScan.class);
    private int pNum;
    private int num;
    private ArrayList<Quad> hullCandidates;
    private ArrayList<Quad> hull;
    
    private class pData implements Comparable<pData> {
        int index;
        double angle;
        double distance;
        
        pData(int i, double a, double d) {
            index = i;
            angle = a;
            distance = d;
        }
        
        // for sorting
        @Override
        public int compareTo(pData p) {
            if (this.angle < p.angle)
                return -1;
            else if (this.angle > p.angle)
                return 1;
            else {
                if (this.distance < p.distance)
                    return -1;
                else if (this.distance > p.distance)
                    return 1;
            }
            return 0;
        }
    }
    
    public ArrayList<Quad> getIDs() {
        return hull;
    }
    
    public GrahamScan(HashSet<Quad> h) {
        hullCandidates = new ArrayList<Quad>(h);
        pNum = hullCandidates.size();
    }
    
    public double angle(int o, int a) {
        return Math.atan((double) (hullCandidates.get(a).getyC() - hullCandidates.get(o).getyC()) / (double) (hullCandidates.get(a).getxC() - hullCandidates.get(o).getxC()));
    }
    
    public double distance(int a, int b) {
        return ((hullCandidates.get(b).getxC() - hullCandidates.get(a).getxC()) * (hullCandidates.get(b).getxC() - hullCandidates.get(a).getxC()) + (hullCandidates.get(b).getyC() - hullCandidates.get(a).getyC()) * (hullCandidates.get(b).getyC() - hullCandidates.get(a).getyC()));
    }
    
    public double ccw(int p1, int p2, int p3) {
        return (hullCandidates.get(p2).getxC() - hullCandidates.get(p1).getxC()) * (hullCandidates.get(p3).getyC() - hullCandidates.get(p1).getyC()) - (hullCandidates.get(p2).getyC() - hullCandidates.get(p1).getyC()) * (hullCandidates.get(p3).getxC() - hullCandidates.get(p1).getxC());
    }
    
    public void swap(int[] stack, int a, int b) {
        int tmp = stack[a];
        stack[a] = stack[b];
        stack[b] = tmp;
    }
    
    @Override
    public void run() {
        // convex hull routine
        // (0) find the lowest point
        int min = 0;
        for (int i = 1; i < pNum; i++) {
            if (hullCandidates.get(i).getyC() == hullCandidates.get(min).getyC()) {
                if (hullCandidates.get(i).getxC() < hullCandidates.get(min).getxC())
                    min = i;
            } else if (hullCandidates.get(i).getyC() < hullCandidates.get(min).getyC())
                min = i;
        }
        
        ArrayList<pData> al = new ArrayList<pData>();
        double ang;
        double dist;
        // (1) calculate angle and distance from base
        for (int i = 0; i < pNum; i++) {
            if (i == min)
                continue;
            ang = angle(min, i);
            if (ang < 0)
                ang += Math.PI;
            dist = distance(min, i);
            al.add(new pData(i, ang, dist));
        }
        // (2) sort by angle and distance
        Collections.sort(al);
        
        // (3) create stack
        int stack[] = new int[pNum + 1];
        int j = 2;
        for (int i = 0; i < pNum; i++) {
            if (i == min)
                continue;
            pData data = al.get(j - 2);
            stack[j++] = data.index;
        }
        stack[0] = stack[pNum];
        stack[1] = min;
        
        int M = 2;
        
        try {
            //        System.out.println("M on ENTRY: " + M);
            for (int i = 3; i <= pNum; i++) {
                //            System.out.println("Start Loop: " + M);
                
                while (((M - 1) > 0) && ccw(stack[M - 1], stack[M], stack[i]) <= 0) {
                    M--;
                    
                }
                M++;
                
                swap(stack, i, M);
            }
        } catch (Exception e) {
            log.error("Error while generating hull: " + ExceptionUtils.getStackTrace(e));
        }
        
        // assign border points
        num = M;
        hull = new ArrayList<Quad>();
        for (int i = 0; i < num; i++) {
            try{
            hull.add(hullCandidates.get(stack[i + 1]));
            }
            catch(ArrayIndexOutOfBoundsException aioob){
                log.warn("Array out of bounds, trying to continue\nInformation: ");
                log.warn("Index: " + i + ", Plus one: " + (i+1) );
                log.warn("Stack size: " + stack.length);
                log.warn("Candidates size: " + hullCandidates.size());
                hull.add(hullCandidates.get(stack[i]));
                continue;
            }
        }
        
    }
    
    public static void main(String args[]) {
        Random r = new Random();
        ArrayList<Quad> quadList = new ArrayList<Quad>();
        for (int i = 0; i < 12; i++) {
            float c = r.nextInt(100) - 50f;
            float d = r.nextInt(100) - 50f;
            
            
            Quad q = new Quad(c,d,c,d);
            quadList.add(q);
        }
        HashSet<Quad> quadSet = new HashSet<Quad>(quadList);
        GrahamScan g = new GrahamScan(quadSet);
        Thread gThread = new Thread(g);
        gThread.start();
        try{
            gThread.join();
        }
        catch(Exception e){
            ExceptionUtils.getStackTrace(e);
        }
        System.out.println(g.getIDs());
        ArrayList<Quad> out = g.getIDs();
        System.out.println(out.size());
        for(int i = 0; i < out.size(); i++){
            System.out.println("new google.maps.LatLng(" + out.get(i).getLat() + ", " + out.get(i).getLon() + ((i == (out.size()-1)) ? ")" : "),"));
        }
        
    }

}
