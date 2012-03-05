package misc;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.sql.*;
import java.util.*;

/**
 * @author Niall Panton
 *         Date: 19/11/2011
 *         Time: 18:08
 */
public class NGram {

    private ArrayList<String> stoplist = new ArrayList<String>();
    private Map<String, Integer> wordCount = new HashMap<String, Integer>();

    public void readStopList() {
        URL listUrl = null;
        BufferedReader in = null;
        try {
            listUrl = new URL("http://dl.dropbox.com/u/476487/stop_list.txt");
            in = new BufferedReader(new InputStreamReader(listUrl.openStream()));
        } catch (Exception e) {
            // If the file cannot be found, or the URL is malformed, exit
            System.out.println("Error could not read file. Cause:\n" + e.toString());
        }
        try {
            String line = null;
            while ((line = in.readLine()) != null) {
                // Add each line to the collection
                stoplist.add(line);
            }
            // Close the stream
            in.close();
        } catch (Exception ioe) {
            System.out.println("Error could not read file. Cause:\n" + ioe.toString());
        }
    }

    public void execute() {
        Connection connect = null;
        try {
            // MySQL
            //Class.forName("com.mysql.jdbc.Driver");
            // SQLite
            Class.forName("org.sqlite.JDBC");
            //set up string
            //connection = DriverManager.getConnection("jdbc:mysql://localhost:8889/red?" + "user=root&password=root");
            connect = DriverManager.getConnection("jdbc:sqlite:tweets.db");

            PreparedStatement preparedStatement = connect.prepareStatement("SELECT `tweet` FROM  `tweets`");
            ResultSet tweets = preparedStatement.executeQuery();
            while (tweets.next()) {
                String[] tweetWords = tweets.getString("tweet").split(" ");

                for (int i = 0; i < tweetWords.length; i++) {
                    if (i + 1 < tweetWords.length) {
                        String word0 = tweetWords[i].toLowerCase();
                        String word1 = tweetWords[i + 1].toLowerCase();

                        if (!word0.contains("http://") && !word0.contains("@") && !stoplist.contains(word0) && !word1.contains("http://") && !word1.contains("@") && !stoplist.contains(word1)) {
                            word0 = word0.trim().replaceAll("[^A-Za-z]", "");
                            word0 = word0.trim().replaceAll("[^A-Za-z]", "");
                            if (!word0.equals("") || !word1.equals("")) {
                                String word = word0 + " " + word1;
                                if (wordCount.containsKey(word)) {
                                    int count = wordCount.get(word);
                                    wordCount.put(word, ++count);
                                } else
                                    wordCount.put(word, 1);
                            }
                        }
                    }
                }

//                for (String word : tweets.getString("tweet").split(" ")) {
//                    word = word.toLowerCase();
//                    if (!word.contains("http://") && !word.contains("@") && !stoplist.contains(word)) {
//                        word = word.trim().replaceAll("[^A-Za-z]", "");
//                        if (!word.equals("")) {
//                            if (wordCount.containsKey(word)) {
//                                int count = wordCount.get(word);
//                                wordCount.put(word, ++count);
//                            } else
//                                wordCount.put(word, 1);
//                        }
//                    }
//                }
            }
            tweets.close();
            preparedStatement.close();
        } catch (SQLException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException c) {
            c.printStackTrace();
        } finally {
            try {
                if (connect != null) {
                    connect.close();
                }
            } catch (SQLException se) {
                se.printStackTrace();
            }
        }
        System.out.println(sortedWordsByCount(25, wordCount));

    }


    public Map<String, Integer> sortedWordsByCount(int max, Map<String, Integer> mapIn) {
        Map<String, Integer> topWordsMap = new TreeMap<String, Integer>();
        Map<String, Integer> wordSet = sortByValue(mapIn);
        int i = 0;
        for (Map.Entry<String, Integer> mainEntry : wordSet.entrySet()) {
            if (i < max) {
                topWordsMap.put(mainEntry.getKey(), mainEntry.getValue());
                i++;
            } else
                break;
        }
        return topWordsMap;
    }

    /**
     * Sort a map by it's values
     *
     * @param map Map to be sorted
     * @return Sorted map
     */
    public static Map sortByValue(Map map) {
        if (map == null) {
            return null;
        }
        List list = new LinkedList(map.entrySet());
        Collections.sort(list, new Comparator() {
            public int compare(Object o1, Object o2) {
                return ((Comparable) ((Map.Entry) (o2)).getValue()).compareTo(((Map.Entry) (o1)).getValue());
            }
        }
        );
        Map result = new LinkedHashMap();
        for (Object aList : list) {
            Map.Entry entry = (Map.Entry) aList;
            result.put(entry.getKey(), entry.getValue());
        }
        return result;
    }


    public static void main(String args[]) {
        NGram n = new NGram();
        n.readStopList();
        n.execute();
    }

}