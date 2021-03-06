package swicr.logic.time;

import java.util.LinkedList;
import java.util.List;

/**
 * Created on 2017-05-29.
 *
 * @author Adrian Zdanowicz
 */
public class Time implements Runnable {
    static private final int TIMEDELAY = 2000;
    static private final int TIMESTEP = 5;

    static private final int MINUTES_PER_DAY = 60 * 24;

    private int currentTime = 0;
    private boolean timeStopped = false;

    private Thread timeThread;
    private Object timeBarrier = new Object();

    private List<TimeTickEvent> tickEvents = new LinkedList<TimeTickEvent>();
    private int timeScale = 100;

    public Time(int startTimeHour, int startTimeMin) {
        currentTime = startTimeHour * 60 + startTimeMin;

        timeThread = new Thread(this, "Time");
    }

    public void registerTickEvent(TimeTickEvent event) {
        tickEvents.add(event);
    }

    private void dispatchTickEvents(int time) {
        for ( TimeTickEvent e : tickEvents ) {
            e.onTimeTick(time);
        }
    }

    public void start() {
        timeThread.start();
    }

    @Override
    public void run() {
        for ( TimeTickEvent e : tickEvents ) {
            e.setInitialState(currentTime);
        }

        while ( true ) {
            try {
                double scaledTime = (double)TIMEDELAY * (100.0/timeScale);
                Thread.sleep((int)scaledTime);
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
            while ( timeStopped ) {
                try {
                    synchronized (timeBarrier) {
                        timeBarrier.wait();
                    }
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
            }
            currentTime += TIMESTEP;
            if (currentTime >= MINUTES_PER_DAY) {
                currentTime -= MINUTES_PER_DAY;
            }
            dispatchTickEvents(currentTime);
        }
    }

    public boolean toggleTimeState() {
        boolean oldState = timeStopped;
        timeStopped = oldState == false;
        if ( !timeStopped ) {
            synchronized (timeBarrier) {
                timeBarrier.notify();
            }
        }

        return oldState;
    }

    public void setTimeScale(int scale) {
        this.timeScale = scale;
    }

    public int getTimeScale() {
        return timeScale;
    }

    public boolean getIsStopped() {
        return timeStopped;
    }

    public static int getResolution() {
        return TIMESTEP;
    }
}
