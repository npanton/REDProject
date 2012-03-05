package util;

/**
 * @author Niall Panton
 *         Date: 23/10/2011
 *         Time: 21:07
 */
public class PointInPolygon {

    public static boolean pointInPolygon(int polySides, float polyX[], float polyY[], float x, float y) {

        int i, j = polySides - 1;
        boolean oddNodes = false;

        // Avoid zero error
        if(y == 0.0f)
            y+=0.0001;

        for (i = 0; i < polySides; i++) {
            if ((polyY[i] < y && polyY[j] >= y
                    || polyY[j] < y && polyY[i] >= y)
                    && (polyX[i] <= x || polyX[j] <= x)) {
                oddNodes ^= (polyX[i] + (y - polyY[i]) / (polyY[j] - polyY[i]) * (polyX[j] - polyX[i]) < x);
            }
            j = i;
        }

        return oddNodes;
    }

    public static void main(String[] args){
        float[] b = {1.0f, 1.0f, -1.0f, -1.0f};
        float[] c = {1.0f, -1.0f, 1.0f, 1.0f};
        System.out.println(pointInPolygon(4, b, c, 0.5f, 0.5f));
        System.out.println(pointInPolygon(4, b, c, 0.0f, 0.0f));

    }

}
