import java.util.*;

public abstract class DelayTimer extends Timer {
	float seconds;
	public DelayTimer(float seconds) {
		super(true);
		this.seconds = seconds;
	}
	public abstract void run();
	public void start() {
		schedule(new TimerTask() { public void run() { DelayTimer.this.run(); }}, (int) (seconds * 1000));
	}
}