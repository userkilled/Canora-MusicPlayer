package e.planet.musicman;

import android.util.Log;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;

public class PerformanceTimer {

    long genesis;
    long tstart;
    long tdur;

    List<Integer> avg;

    String LOG_TAG = "PT";

    public PerformanceTimer()
    {
        tstart = 0;
        tdur = 0;
        genesis = System.currentTimeMillis();
    }
    public long start()
    {
       tstart = System.currentTimeMillis();
       return tstart;
    }
    public long step()
    {
        tdur = System.currentTimeMillis() - tstart;
        tstart = System.currentTimeMillis();
        return tdur;
    }
    public long stop()
    {
        tdur = System.currentTimeMillis() - tstart;
        return tdur;
    }
    public long printTotal(String context, String function)
    {
        long t = System.currentTimeMillis() - genesis;
        Log.v(LOG_TAG,context + " " + function + " Took " + t + " Miliseconds in Total.");
        return t;
    }
    public void printStep(String context, String function)
    {
        tdur = System.currentTimeMillis() - tstart;
        tstart = System.currentTimeMillis();
        Log.v(LOG_TAG,context + " " + function + " Took " + tdur + " Miliseconds to complete.");
    }
    public void startAverage()
    {
        avg = new ArrayList<>();
        tstart = System.currentTimeMillis();
    }
    public void stepAverage()
    {
        tdur = System.currentTimeMillis() - tstart;
        tstart = System.currentTimeMillis();
        avg.add(safeLongToInt(tdur));
    }
    public void printAverage(String context, String function)
    {
        if (avg != null) {
            int total = 0;
            int ret = 0;
            for (int i = 0; i < avg.size(); i++) {
                total += avg.get(i);
                ret = total / avg.size();
            }
            Log.v(LOG_TAG,context + " " + function + " Took on Average: " + ret + " Miliseconds to Complete. Total: " + total + " Miliseconds.");
        }
        else
            Log.v(LOG_TAG,context + " " + function + "ERROR GETTING AVERAGE");
    }

    public static int safeLongToInt(long l) {
        if (l < Integer.MIN_VALUE || l > Integer.MAX_VALUE) {
            throw new IllegalArgumentException
                    (l + " cannot be cast to int without changing its value.");
        }
        return (int) l;
    }
}
