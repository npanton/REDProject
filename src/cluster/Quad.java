package cluster;

public class Quad {

    double lat, lon, xC, yC;

    public double getLat() {
        return lat;
    }

    public double getLon() {
        return lon;
    }

    public double getxC() {
        return xC;
    }

    public double getyC() {
        return yC;
    }

    public Quad(double x, double y, double lt, double ln) {
        lat = lt;
        lon = ln;
        xC = x;
        yC = y;
    }

    @Override
    public int hashCode() {
        return ((int) lat * 1000) + (int) lon;
    }

    @Override
    public boolean equals(Object c) {
        if (c == null)
            return false;
        if (c == this)
            return true;
        if (c.getClass() != getClass())
            return false;

        Quad val = (Quad) c;
        return val.getLat() == this.lat && val.getLon() == this.lon && val.getxC() == this.xC && val.getyC() == this.yC;
    }

    @Override
    public String toString() {
        return "[" + xC + "," + yC + "," + lat + "," + lon + "]";
    }
}
