package util;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.*;

/**
 * Created by IntelliJ IDEA.
 * User: niallpanton
 * Date: 27/11/2011
 * Time: 11:19
 */
public class JSONParser {

    private static int count = 0;

    public static void processLine(String line) {

        try {
            if (line.startsWith("{")) {
                if (count % 100000 == 0)
                    System.out.println(count + " lines");

                count++;
                JSONObject json = new JSONObject(line);
            }
        } catch (JSONException j) {
//            j.printStackTrace();
        }
    }



    public static void main(String args[]) {
        long start = System.currentTimeMillis();
        try {

            FileInputStream fstream = new FileInputStream(args[0]);
            DataInputStream in = new DataInputStream(fstream);
            BufferedReader br = new BufferedReader(new InputStreamReader(in));
            String line;
            while ((line = br.readLine()) != null) {
                processLine(line);
            }
            in.close();


        } catch (Exception e) {//Catch exception if any
//            e.printStackTrace();
        }
        System.out.println((System.currentTimeMillis() - start) / 1000 + " s");
        System.out.println(count);

    }
}


