/**
 * Battle class is a JPanel representing an RPG style battle scene
 * and also keeps track of user input data and its randomness, ending
 * the game when necessary. 
 * @author Alexander Wong and Jiaming Chen
 * Period: 2
 * Date: 2016-04-30 (ISO)
 */

import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import javax.imageio.ImageIO;
import javax.sound.sampled.Clip;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JTextField;

public class Battle extends JPanel
{
	public static final double ALPHA = .05;
	public static final int NUM_OPTIONS = 4;
	private final Clip clip; 
	private static final List<Integer> userInputs = new LinkedList<>();
	private Overworld floor;
	private int turnsLeft;
	private Enemy enemy;

	/**
	 * Stores a reference to the floor whose GUI must be returned to after
	 * this battle, adds scenery and music, and responds to user input.
	 * @param floor - the floor to which the GUI must return.
	 */
	public Battle(Overworld floor, Enemy enemy, int turns)
	{
		setFocusable(true);
		this.floor = floor;
		this.enemy = enemy;
		turnsLeft = turns;
		clip = enemy.getType().getMusic();
		addComponents();
		takeInput();
	}
	
	/**
	 * Adds scenery to the GUI: Title text, enemy sprite, and options.
	 */
	private void addComponents()
	{
		this.setLayout(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		
		this.add(centeredTextBox(enemy.getType().getText(), Color.GRAY), c);
		c.gridy++;
		
		this.add(enemy.getType().getSprite(), c);
		c.gridy++;

		this.add(centeredTextBox("Options:", Color.GRAY), c);
		c.gridy++;
		
		JPanel options = new JPanel(new GridLayout(2, 2));
		for(int gridx = 0; gridx < 4; gridx++)
			options.add(centeredTextBox(""+gridx, Color.GRAY));
		
		this.add(options, c);
	}
	
	/**
	 * Plays thematic music while responding to user data, returning to
	 * the overworld or ending the game as necessary.
	 */
	private void takeInput()
	{
		if(clip != null)
			clip.loop(Clip.LOOP_CONTINUOUSLY);
		
		this.addKeyListener(new KeyListener() 
		{
			public void keyReleased(KeyEvent e)
			{
				char c = e.getKeyChar();
				if(c >= '1' && c <= '4')
				{
					takeTurn(c - '1');
				}
			}
			
			public void keyTyped(KeyEvent e){}
			public void keyPressed(KeyEvent e){}
		});
	}
	
	/**
	 * Processes a turn by the player
	 * @param input the option selected by the player
	 */
	private void takeTurn(int input)
	{
		userInputs.add(input);
		testForRandomness();
		if (--turnsLeft == 0)
			overworldReturn();
	}
	
	/**
	 * Ends the battle by returning the GUI to the Overworld.
	 */
	private void overworldReturn()
	{
		if(clip != null)
			clip.stop();

		if(floor != null)
			floor.overworldReturn();
	}
	
	/**
	 * Returns a JTextField with a centered, non-editable, non-focusable 
	 * given text with a given color background.
	 * @param message - the text to be displayed in this JTextField
	 * @param background - the background color for this JTextField
	 * @return a JTextField with a centered, non-editable, non-focusable 
	 *         given text with a given color background.
	 */
	private static JTextField centeredTextBox(String message, Color background)
	{
		JTextField ret = new JTextField(message);
		ret.setHorizontalAlignment(JTextField.CENTER);
		ret.setEditable(false);
		ret.setFocusable(false);
		ret.setBackground(background);
		return ret;
	}
	
	/**
	 * Tests the randomness of the input history of the player and causes the 
	 * player to lose if the results do not meet the randomness threshold.
	 */
	private void testForRandomness()
	{
		int maxGroupSize = (int) (Math.log(userInputs.size() / 5.0) / Math.log(NUM_OPTIONS));
		for (int groupSize = 1; groupSize <= maxGroupSize; groupSize++) 
		{
			int numGroups = (int) Math.pow(NUM_OPTIONS, groupSize);
			
			if (userInputs.size() % groupSize != 0)
				continue;
			
			Iterator<Integer> it = userInputs.iterator();
			int[] frequencies = new int[numGroups];
			while (it.hasNext()) {
				int n = 0;
				for (int j = 0; j < groupSize; j++) 
				{
					n *= 4;
					n += it.next();
				}
				frequencies[n]++;
			}
			double pValue = chiSquaredUniformityTest(frequencies);
			if (pValue <= ALPHA)
			{
				System.out.printf("P-Value: %.6f%n", pValue);
				floor.loseTheGame();
			}
		}
	}

	/**
	 * Performs a chi squared statistical test on a given list of Integers
	 * @param data - an array of integers on which to perform analysis
	 * @return a value in [0, 1] describing randomness, with 1 being more
	 *         random than 0.
	 */
	private static double chiSquaredUniformityTest(int[] data) 
	{
		int total = 0;
		for (int n : data)
			total += n;
		
		double chiSquared = 0;
		for (int n : data) 
		{
			double expected = (double) total / data.length;
			double diff = n - expected;
			chiSquared += diff * diff / expected;
		}
		return 1 - chiSquaredCDF(chiSquared, data.length - 1);
	}
	
	/**
	 * Approximates the gamma function for a given real number n.
	 * @param n - the number for which gamma shall be approximated.
	 * @return an approximation of the gamma function for the given n.
	 */
	private static double gamma(double n) 
	{
		n--;
		return Math.sqrt(2 * Math.PI * n) * Math.pow(n / Math.E, n);
	}
	
	private static double lowerIncompleteGamma(double n, double x) 
	{
		double s = 0;
		for (double t = 0; t < x; t+= .001) {
			s += Math.pow(t, n-1)*Math.pow(Math.E, -t)*.001;
		}
		return s;
	}
	
	private static double chiSquaredCDF(double x, double k) {
		return lowerIncompleteGamma(k/2, x/2)/gamma(k/2);
	}
}
