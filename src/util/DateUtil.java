package util;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;

/**
 * @author Niall Panton
 *         Date: 10/09/2011
 *         Time: 17:59
 */
public class DateUtil {



    public static Date parseDate(String date) {
        DateFormat formatter = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            return formatter.parse(date);
        } catch (Exception e) {
            return null;
        }
    }

    public static Date alterDate(Date currentDate, int stepType, int amount) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(currentDate);
        switch (stepType) {
            case 0:
                calendar.add(Calendar.SECOND, amount);
                break;
            case 1:
                calendar.add(Calendar.MINUTE, amount);
                break;
            case 2:
                calendar.add(Calendar.HOUR_OF_DAY, amount);
                break;
            case 3:
                calendar.add(Calendar.DAY_OF_MONTH, amount);
                break;
            case 4:
                calendar.add(Calendar.WEEK_OF_MONTH, amount);
                break;
            case 5:
                calendar.add(Calendar.MONTH, amount);
                break;
        }
        return calendar.getTime();
    }

}
