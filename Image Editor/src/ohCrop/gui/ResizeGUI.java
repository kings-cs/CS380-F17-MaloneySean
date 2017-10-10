package ohCrop.gui;

import java.awt.BorderLayout;

import javax.swing.JFrame;
import javax.swing.JSlider;
import javax.swing.JTextField;

/**
 * GUI frame that appears to prompt the user for the dimensions to change the GUI to.
 * @author Sean Maloney 
 */
public class ResizeGUI extends JFrame{
	
	/**
	 * Generated Serial ID.
	 */
	private static final long serialVersionUID = -2840094187213020629L;

	/**
	 * Constructor for the Resize GUI.
	 */
	public ResizeGUI() {
		this.setLayout(new BorderLayout());
		
		JSlider zoomSlider = new JSlider(JSlider.HORIZONTAL, 100, 500, 100);
		zoomSlider.setMajorTickSpacing(100);
		zoomSlider.setMinorTickSpacing(10);
		zoomSlider.setPaintTicks(true);
		zoomSlider.setPaintLabels(true);
		
		JTextField text = new JTextField();
		
		
		this.add(zoomSlider, BorderLayout.CENTER);
		this.add(text, BorderLayout.SOUTH);
		
		//TODO:In order to update these together do I need an action listener or a change listener?
	}
	
	
	
	/**
	 * The main method for running the GUI.
	 * 
	 * @param args Not Used.
	 */
	public static void main(String[] args) {
		ResizeGUI mainFrame = new ResizeGUI();
		mainFrame.setTitle("Oh Crop");
		mainFrame.setSize(250, 250);
		mainFrame.setVisible(true);
		
		//TODO: Can I set a border? Can I set it to Open in the center?
	}
}
