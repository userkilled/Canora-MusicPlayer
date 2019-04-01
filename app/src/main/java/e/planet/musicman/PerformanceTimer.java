package e.planet.musicman;

import android.util.Log;

public class PerformanceTimer {

    long tstart;
    long tdur;

    public PerformanceTimer()
    {
        tstart = 0;
        tdur = 0;
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
    public void printStep(String LOG_TAG, String function)
    {
        tdur = System.currentTimeMillis() - tstart;
        tstart = System.currentTimeMillis();
        Log.v(LOG_TAG,function + " Took " + tdur + " Miliseconds to complete.");
    }
}
