import java.io.*;
import java.util.concurrent.*;
import java.net.*;
import javax.sound.sampled.*;
import javax.sound.sampled.LineEvent.Type.*;

public class Sounds {

	public static final Playable shoot = new Playable("shoot.wav");
	public static final Playable powerUp = new Playable("powerup.wav");
	public static final Playable gameOver = new Playable("gameover.wav");
	public static final Playable death = new Playable("death.wav");
	public static final Clip bgm = createClip("bgm.wav"); // use default Clip to enable looping
	
	static {
		// remove gap from bgm audio
		bgm.setLoopPoints(2000, bgm.getFrameLength() - 1000);
	}

	public static class Playable {
	
		File file;
		
		long lastPlayTime = 0;
		
		public Playable(String filename) {
			this.file = new File("audio/" + filename);
			// this.format = new AudioFormat(AudioFormat.Encoding.PCM_SIGNED, AudioSystem.NOT_SPECIFIED, 16, 2, 4, AudioSystem.NOT_SPECIFIED, true);
			// this.info = new DataLine.Info(Clip.class, format);
		}
		
		public boolean enabled = true;
		
		public void play() {
			
			if (!enabled) return;
			
			// allow only one instance to play in an interval of 100 milliseconds
			
			long time = System.currentTimeMillis();
			if (time - lastPlayTime >= 100) {
				lastPlayTime = time;
				// play sound if not playing anymore
				FixedTimer.schedule(player);
			}
			
		}
		
		Runnable player = new Runnable() {
			public void run() {
				try {
					final AudioInputStream in = AudioSystem.getAudioInputStream(file);
					
					// get data via line so that we can attach line listener later
					final Clip clip = (Clip) AudioSystem.getClip();
					clip.open(in);
					clip.start();
					
					clip.addLineListener(new LineListener() {
						public void update(LineEvent e) {
							// close clip if sound stops to empty garbage memory
							if (e.getType().equals(LineEvent.Type.STOP)) {
								clip.close();
							}
						}
					});
				}
				catch (Exception ex) {
					System.err.println("Unavailable audio: " + ex);
				}
			}
		};
	}
	
	public static Clip createClip(String filename) {
		try {
			Clip clip = AudioSystem.getClip();
			AudioInputStream in = AudioSystem.getAudioInputStream(new File("audio/" + filename));
			clip.open(in);
			return clip;
		}
		catch (Exception ex) {
			System.err.println("Unavailable audio: " + ex);
		}
		return null;
	}
	
}

