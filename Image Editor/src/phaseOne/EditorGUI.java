package phaseOne;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JToolBar;

/**
 * Main GUI window used in an Image Editor application that will allow the user to perform various modifications on a displayed image.
 * @author Sean Maloney
 */
public class EditorGUI extends JFrame{
	
	/**
	 * Generated Serial ID.
	 */
	private static final long serialVersionUID = 7320549510928224667L;
	
	/**
	 * Menu Item used to open the file dialog.
	 */
	private JMenuItem file;
	
	/**
	 * Menu Item that can be used to exit the program.
	 */
	private JMenuItem exit;
	
	/**
	 * Menu Item used to open image files.
	 */
	private JMenuItem open;
	
	/**
	 * Menu Item used to close the currently open image.
	 */
	private JMenuItem close;
	
	/**
	 * Menu Item that contains various functions relevant to the User's view.
	 */
	private JMenuItem view;
	
	/**
	 * Menu Item that lets the user change the size of the window.
	 */
	private JMenuItem resizeWindow;
	
	/**
	 * Menu Item that lets the user zoom in on an image.
	 */
	private JMenuItem zoom;
	
	/**
	 * Menu Item used to save the image currently being edited.
	 */
	private JMenuItem save;
	
	/**
	 * Menu Item used to save the image currently being edited to a new destination.
	 */
	private JMenuItem saveAs;
	
	/**
	 * The Pane that will be used to display the image being modified.
	 */
	private JScrollPane display;
	
	/**
	 * Tool bar used to perform various functions on the displayed image.
	 */
	private JToolBar toolBar;
	
	/**
	 * Button used to convert the current image to be in Gray Scale.
	 */
	private JButton grayScale;
	
	/**
	 * JPanel used to display an image.
	 */
	private JPanel canvas;
	
	/**
	 * The image being displayed currently.
	 */
	private BufferedImage image;
	
	/**
	 * The path for the most recently opened image.
	 */
	private String currentFilePath;
	
	/**
	 * The original height of the image being displayed before any form of zooming is applied.
	 */
	private int originalHeight;
	
	/**
	 * The original width of the image being displayed before any form of zooming is applied.
	 */
	private int originalWidth;
	
	/**
	 * The current level of zoom being applied where 1.0 is the original amount.
	 */
	private double zoomAmount;
	
	/**
	 * Constructor for the GUI.
	 */
	public EditorGUI() {
		WindowAdapter wc = new WindowClosing();
		ActionListener  ae = new ActionEvents();
		
		this.setLayout(new BorderLayout());
		
		JMenuBar menuBar = new JMenuBar();
		file = new JMenu("File");
		exit = new JMenuItem("Exit");
		open = new JMenuItem("Open");
		close = new JMenuItem("Close");
		save = new JMenuItem("Save");
		saveAs = new JMenuItem("Save As...");
		
		view = new JMenu("View");
		resizeWindow = new JMenuItem("Resize Window");
		zoom = new JMenuItem("Zoom");
		
		menuBar.add(file);
		file.add(exit);
		file.add(open);
		file.add(close);
		file.add(save);
		file.add(saveAs);
		
		menuBar.add(view);
		view.add(resizeWindow);
		view.add(zoom);
		
		this.setJMenuBar(menuBar);
		
		toolBar = new JToolBar("Tool Bar");
		toolBar.setFloatable(true);
		
		
		//TODO: Come back to this section later to make buttons look better
		grayScale = new JButton("Gray Scale");
	
		
	
		
		//***************Image Displaying Starts Here**********
		BufferedImage defaultImage = getImageFromFile("Images//crop.png");
		paintImage(defaultImage);
		windowResize(defaultImage.getHeight(), defaultImage.getWidth());
		
		originalHeight = defaultImage.getHeight();
		originalWidth = defaultImage.getWidth();
		zoomAmount = 1;
		
		
		toolBar.add(grayScale);
		this.add(toolBar, BorderLayout.NORTH);
		
		

		
		//****************Adding Listeners********************************
		grayScale.addActionListener(ae);
		exit.addActionListener(ae);
		open.addActionListener(ae);
		close.addActionListener(ae);
		save.addActionListener(ae);
		saveAs.addActionListener(ae);
		
		resizeWindow.addActionListener(ae);
		zoom.addActionListener(ae);
		
		this.addWindowListener(wc);
	}
	
	/**
	 * Gets a BufferedImage to be used to pain the screen out of a file.
	 * @param filePath The path of the file.
	 * @return The desired BufferedImage.
	 */
	private BufferedImage getImageFromFile(String filePath) {
		File currentPicture = new File(filePath);
		BufferedImage ri = null;
		try {
			ri = ImageIO.read(currentPicture);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "The Image Could Not Be Read From A File At The Given Path", "Oops", 
					JOptionPane.ERROR_MESSAGE);
		}
		
		currentFilePath = filePath;
		return ri;
	}
	
	/**
	 * Paint the image at the file path onto the GUI.
	 * @param ri The BufferedImage used to paint the screen.
	 */
	private void paintImage(BufferedImage ri) {
		
		
		image = new BufferedImage(ri.getWidth(), ri.getHeight(),
				BufferedImage.TYPE_INT_ARGB);
		Graphics g = image.getGraphics();
		g.drawImage(ri, 0, 0, null);
		
	
		canvas = new JPanel() {
			/**
			 * Serial ID.
			 */
			private static final long serialVersionUID = 1L;

			@Override
			public void paintComponent(Graphics g) {
				super.paintComponent(g);
				if(image != null) {
					g.drawImage(image, 0, 0, this);
				}
			}
		};
		
		canvas.setPreferredSize(new Dimension(image.getWidth(), image.getHeight()));
		
		if(display != null) {
			display.setVisible(false);
		}
		
		display = new JScrollPane(canvas);	
		
		
		this.add(display, BorderLayout.CENTER);
		revalidate();
	}

	
	/**
	 * Private helper method used to save the image currently being edited. 
	 * @param filePath The destination for the image.
	 */
	private void save(String filePath){
		File outputFile = new File(filePath);
		try {
			ImageIO.write(image, "png", outputFile);
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "The Location To Save The Image To Was Invalid", "Oops", 
					JOptionPane.ERROR_MESSAGE);
		}
	}
	
	/**
	 * Private helper method used to change the size of the GUI window.
	 * @param height The new height of the GUI.
	 * @param width The new width of the GUI.
	 */
	private void windowResize(int height, int width) {
		int newWidth = width + 150;
		int newHeight = height + 150;
		
		int systemWidth = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().width;
		int systemHeight = java.awt.GraphicsEnvironment.getLocalGraphicsEnvironment().getMaximumWindowBounds().height;
		
		if(width > systemWidth) {
			newWidth = systemWidth;
		}
		if(height > systemHeight) {
			newWidth = systemHeight;
		}
		
		this.setSize(new Dimension(newWidth, newHeight));
	}
	
	/**
	 * Private helper method used to close the program.
	 */
	private void exit() {
		setVisible(false);
		System.exit(0);
	}
	
	/**
	 * Private inner class used to listen for Action Events such as button clicks.
	 * @author Sean Maloney
	 */
	private class ActionEvents implements ActionListener {
		
		/**
		 * Performs the appropriate action for a certain button press.
		 */
		@Override
		public void actionPerformed(ActionEvent action) {
			if(action.getSource() == exit) {
				exit();
			}
			else if(action.getSource() == open) {
				JFileChooser fileChooser = new JFileChooser();
				int result = fileChooser.showOpenDialog(null);
				
				if(result == JFileChooser.APPROVE_OPTION){
					String filePath = fileChooser.getSelectedFile().getPath();
					
					//TODO: Do something here with file types
					
					BufferedImage img = getImageFromFile(filePath);
					paintImage(img);
					windowResize(img.getHeight(), img.getWidth());
					
					
					originalHeight = img.getHeight();
					originalWidth = img.getWidth();
					zoomAmount = 1;
				}
				else{
					JOptionPane.showMessageDialog(null, "Image Location Not Properly Selected", "Oops", 
							JOptionPane.ERROR_MESSAGE);
				}
			}
			else if(action.getSource() == close) {
				
				BufferedImage img = getImageFromFile("Images//crop.png");
				paintImage(img);
				windowResize(img.getHeight(), img.getWidth());
				
				originalHeight = img.getHeight();
				originalWidth = img.getWidth();
				zoomAmount = 1;
			}
			else if(action.getSource() == save) {
				save(currentFilePath);
				
			}
			else if(action.getSource() == saveAs) {
				//TODO: The file format must be specified
				
				JFileChooser fileChooser = new JFileChooser();
				int result = fileChooser.showOpenDialog(null);
				
				if(result == JFileChooser.APPROVE_OPTION){
					String filePath = fileChooser.getSelectedFile().getPath();
					
					
					save(filePath);
				}
				else{
					JOptionPane.showMessageDialog(null, "Image Location Not Properly Selected", "Oops", 
							JOptionPane.ERROR_MESSAGE);
				}
			}
			else if(action.getSource() == resizeWindow) {
				//TODO: MAKE ONE WINODW THAT CAN GET BOTH INPUTS
				try{
					int height = Integer.parseInt(JOptionPane.showInputDialog("Enter New Height: ", 0));
					int width = Integer.parseInt(JOptionPane.showInputDialog("Enter New Width: ", 0));
					
					
					windowResize(height, width);
					}
					catch(NumberFormatException e){
						JOptionPane.showMessageDialog(null, "Only Numeric Characters May Be Entered");
					}
			}
			else if(action.getSource() == zoom) {		
				int intitalPos = (int) (zoomAmount * 100);
				JSlider zoomSlider = new JSlider(JSlider.HORIZONTAL, 100, 500, intitalPos);
				zoomSlider.setMajorTickSpacing(100);
				zoomSlider.setMinorTickSpacing(10);
				zoomSlider.setPaintTicks(true);
				zoomSlider.setPaintLabels(true);
				
				try {
					//TODO: How do I update the text box?
					//TODO: How am I supposed to get from the slider?
					//TODO: Label in an option pane?
					
					//double zoomLevel = Double.parseDouble(JOptionPane.showInputDialog(zoomSlider));
					JOptionPane.showMessageDialog(null, zoomSlider);
					double zoomLevel = zoomSlider.getValue();
					
					//System.out.println(zoomLevel);
					//System.out.println(zoomSlider.getValue());
					
					if(zoomLevel < 100) {
						zoomLevel = 100;
					}
					
					zoomAmount = zoomLevel / 100;
					
					
					int newImageWidth = (int) (originalWidth * zoomAmount);
					int newImageHeight = (int) (originalHeight * zoomAmount);

					BufferedImage resizedImage = new BufferedImage(newImageWidth, newImageHeight, BufferedImage.TYPE_INT_ARGB);
					Graphics g = resizedImage.createGraphics();
					g.drawImage(image, 0, 0, newImageWidth, newImageHeight, null);
					g.dispose();

					paintImage(resizedImage);
				}
				catch(NumberFormatException e){
					JOptionPane.showMessageDialog(null, "Only Numeric Characters May Be Entered");
				}
				
			}
			else if(action.getSource() == grayScale) {		
				long startTime = System.nanoTime();
				BufferedImage gray = Grayscale.grayScale(image);
				long endTime = System.nanoTime();
				
				long timeTaken = endTime - startTime;
				
				double miliSeconds = timeTaken / 1000000.0;
				JOptionPane.showMessageDialog(null, "Time Taken: " + miliSeconds + " (ms)");
				
				paintImage(gray);
			}
		}
	}
	
	/**
	 * The window closing listener that tells the GUI what to do upon exit.
	 * 
	 * @author Sean Maloney
	 */
	private class WindowClosing extends WindowAdapter {
		
		/**
		 * Closes the Frame when it is done being used.
		 */
		
		@Override
		public void windowClosing(WindowEvent arg0) {
			exit();
		}
		
	}
	
	
	/**
	 * The main method for running the GUI.
	 * 
	 * @param args Not Used.
	 */
	public static void main(String[] args) {
		EditorGUI mainFrame = new EditorGUI();
		mainFrame.setTitle("Oh Crop");
		mainFrame.setVisible(true);
	}
}
