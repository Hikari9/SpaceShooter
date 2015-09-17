import java.awt.event.KeyEvent;
import javax.swing.JFrame;

public class LaunchGame {

	public static void main(String[] args) {
		// launch menu
		
		JFrame frame = new JFrame("Space Shooter");
		Game game = Global.randomGame();
		
		frame.add(game);
		FullScreenToggle.addToggleToGame(frame, game, KeyEvent.VK_F11);
        
        Menu menu = new MainMenu();
		game.addMenu(menu);
        
		game.startTheGame();
		
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setSize(1000,600);
		frame.setVisible(true);
	}
	
}