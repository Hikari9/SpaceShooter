import java.util.*;
import java.util.concurrent.*;

public class FixedTimer {
	
	public final FixedTask task;
    protected Future future = null;
    public static final ScheduledExecutorService pool = 
        Executors.newScheduledThreadPool(Global.poolSize);
       
	
	public static ScheduledFuture<?> schedule(Runnable r) {
		return pool.schedule(r, 0, TimeUnit.MILLISECONDS);
	}
	
    
    public boolean fixedRate() { return task.fixedRate(); }
    public boolean isRunning() { return future != null && !future.isCancelled(); }
    public float FPS() { return task.FPS(); }
	
	public FixedTimer(FixedTask task) {
        this.task = task;
		task.connectedTimer.add(this);
        Rescheduler.add(this);
	}
	
	public FixedTimer start() {
		// don't schedule at fixed rate for variable delay
		// schedule(new Runner(), (int) (task.delay() * 1000));
		if (task.fixedRate()) {
            future = pool.scheduleAtFixedRate(task, (long) (task.delay() * 1000000), (long) (1000000 / task.FPS()), TimeUnit.MICROSECONDS);
        }
        else {
            future = pool.scheduleWithFixedDelay(task, (long) (task.delay() * 1000000), (long) (1000000 / task.FPS()), TimeUnit.MICROSECONDS);
        }
        return this;
	}
	
    public FixedTimer stop(boolean interrupt) {
        if (future != null)
            future.cancel(interrupt);
		return this;
    }
    
	public FixedTimer stop() {
		return stop(false);
	}
    	
	static class Rescheduler implements Runnable {
        
        public static void add(FixedTimer timer) {
			if (timer.task.reschedulable()) {
				timerList.offer(timer);
				fixedRateList.offer(timer.fixedRate());
				FPSList.offer(timer.FPS());
			}
        }
        
        static LinkedList<FixedTimer> timerList = new LinkedList<FixedTimer>();
        static LinkedList<Boolean> fixedRateList = new LinkedList<Boolean>();
        static LinkedList<Float> FPSList = new LinkedList<Float>();
        
        static {
            pool.scheduleAtFixedRate(new Rescheduler(), 1, 200, TimeUnit.MILLISECONDS);
        }
        
		public void run() {
			try {
				ListIterator<FixedTimer> it1 = timerList.listIterator();
				ListIterator<Boolean> it2 = fixedRateList.listIterator();
				ListIterator<Float> it3 = FPSList.listIterator();
				while (it1.hasNext() && it2.hasNext() && it3.hasNext()) {
					FixedTimer timer = it1.next();
					Boolean fixedRate = it2.next();
					Float FPS = it3.next();
					if (fixedRate != timer.fixedRate() || FPS != timer.FPS()) {
						// fixedRateList.set(i, timer.fixedRate());
						it2.set(timer.fixedRate());
						it3.set(timer.FPS());
						boolean wasRunning = timer.isRunning();
						timer.stop();
						if (wasRunning)
							timer.start();
					}
				}
			}
			catch (Exception ex) {
				System.err.println("Exception occured in rescheduler: " + ex);
			}
			
		}
	}
	
}

abstract class FixedTask implements Runnable {
	public final ConcurrentLinkedQueue<FixedTimer> connectedTimer = new ConcurrentLinkedQueue<FixedTimer>();
	public boolean fixedRate() { return true; }
	public boolean reschedulable() { return false; }
	public float FPS() { return Global.FPS; }
    public float delay() { return 0; }
	public abstract void run();
	public void stop() {
		for (FixedTimer timer : connectedTimer) {
			timer.stop();
		}
	}
}
