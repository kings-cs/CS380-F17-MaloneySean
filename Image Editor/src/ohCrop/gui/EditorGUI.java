package ohCrop.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Graphics;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;

import javax.imageio.ImageIO;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
//import javax.swing.JSlider;
//import javax.swing.JToolBar;

import org.jocl.CL;
import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;

import ohCrop.editingAlgorithms.Blur;
import ohCrop.editingAlgorithms.BlurParallel;
import ohCrop.editingAlgorithms.GrayScale;
import ohCrop.editingAlgorithms.HistogramEquilization;
import ohCrop.editingAlgorithms.Mosaic;
import ohCrop.editingAlgorithms.GrayScaleParallel;
import ohCrop.editingAlgorithms.HistogramEqualizationParallel;
import ohCrop.editingAlgorithms.TransformationParallel;
import ohCrop.editingAlgorithms.MosaicParallel;
import ohCrop.editingAlgorithms.RedEyeParallel;
import ohCrop.editingAlgorithms.SeamlessCloneParallel;
import ohCrop.editingAlgorithms.SepiaParallel;
import ohCrop.editingAlgorithms.Sepia;
import ohCrop.utilAlgorithms.ParallelSetUp;
import ohCrop.utilAlgorithms.SetUpObject;

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
	 * Menu Item that can be used to transform an image rotationally.
	 */
	private JMenuItem transform;
	
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
	 * Menu Item used to perform gray scale editing.
	 */
	private JMenuItem grayFile;
	
	/**
	 * Menu Item used to perform sepia scale.
	 */
	private JMenuItem sepiaFile;
	
	/**
	 * Menu Item used to perform blur.
	 */
	private JMenuItem blurFile;
	
	/**
	 * Menu Item used to perform mosaic.
	 */
	private JMenuItem mosaicFile;
	
	/**
	 * Menu Item used to perform histogram equalization.
	 */
	private JMenuItem histogramFile;
	
	/**
	 * Menu Item used perform red eyes.
	 */
	private JMenuItem redEyeFile;
	
	/**
	 * Menu Item used to perform seamless clones.
	 */
	private JMenuItem cloneFile;
	
	/**
	 * The Pane that will be used to display the image being modified.
	 */
	private JScrollPane display;
	
//	/**
//	 * Tool bar used to perform various functions on the displayed image.
//	 */
//	private JToolBar toolBar;
	
	/**
	 * Menu Item used to flip an image horizontally.
	 */
	private JMenuItem flipHorizontal;
	
	/**
	 * Menu Item used to flip an image vertically.
	 */
	private JMenuItem flipVertical;
	
	/**
	 * Menu Item used to rotate an image 90 degree to the right.
	 */
	private JMenuItem rotateRight;
	
	/**
	 * Menu Item used to rotate an image 90 degrees to the left.
	 */
	private JMenuItem rotateLeft;
	
	/**
	 * Button used to convert the current image to be in Gray Scale.
	 */
	private JButton grayScale;
	
	/**
	 * Button used to convert the current image to be in Gray Scale that is computed in parallel.
	 */
	private JButton grayScaleParallel;
	
	/**
	 * Button used to convert the current image into Sepia tone.
	 */
	private JButton sepiaTone;
	
	/**
	 * Button used to convert the current image into Sepia Tone, computed in parallel.
	 */
	private JButton sepiaToneParallel;
	
	/**
	 * Button used to blur the current image.
	 */
	private JButton blur;
	
	/**
	 * Button used to blur the current image, computed in parallel.
	 */
	private JButton blurParallel;
	
	/**
	 * Button used to convert an image to a mosaic.
	 */
	private JButton mosaic;
	
	/**
	 * Buttons used to convert an image to a mosaic, computed in parallel.
	 */
	private JButton mosaicParallel;
	
	/**
	 * Button used to perform a Histogram Equalization on an image.
	 */
	private JButton histogramEq;
	
	/**
	 * Button used to perform a Histogram Equalization on an image, computed in parallel.
	 */
	private JButton histogramEqParallel;
	
	/**
	 * Button used to perform a Histogram Equalization on an image, computed in parallel using atomics.
	 */
	private JButton atomicParallelHist;
	
	/**
	 * Button used to remove red eyes from an image.
	 */
	private JButton redEyeParallel;
	
	/**
	 * Button used to perform seamless cloning on two images.
	 */
	private JButton cloneParallel;
	
	/**
	 * JPanel used to display an image.
	 */
	private JPanel canvas;
	
	/**
	 * The image being displayed currently.
	 */
	private BufferedImage image;
	
	/**
	 * The image being displayed currently independent of any zoom.
	 */
	private BufferedImage preZoomImage;
	
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
	 * A label stating the current level of zoom.
	 */
	private JLabel currentZoom;
	
	/**
	 * Button that will add 10% to the current level of zoom.
	 */
	private JButton zoomIn;
	
	/**
	 * Button that will subtract 10% from the current level of zoom.
	 */
	private JButton zoomOut;
	
	/**
	 * Class that handles the necessary OpenCL to run parallelisms.
	 */
	private ParallelSetUp parallelControl;
	
	/**
	 * HashMap mapping Device names to their OpenCL id's.
	 */
	private HashMap<String, SetUpObject> deviceMap;
	
	/**
	 * Whether or not a change was made to the image.
	 */
	private boolean changeMade;
	
	/**
	 * A combo box containing the name of all the computational devices.
	 */
	private JComboBox<String> deviceList;
	
	/**
	 * Constructor for the GUI.
	 */
	public EditorGUI() {
		changeMade = false;
		
		CL.setExceptionsEnabled(true);
		parallelControl = new ParallelSetUp();
		deviceMap = parallelControl.listDevices();
		
		WindowAdapter wc = new WindowClosing();
		ActionListener  ae = new ActionEvents();
		
		this.setLayout(new BorderLayout());
		
		deviceList = new JComboBox<String>();
		
		for(String current : deviceMap.keySet()) {
			deviceList.addItem(current);
		}
		
		
		//****************Tool Bar*****************************
//		toolBar = new JToolBar("Tool Bar");
//		toolBar.setFloatable(true);
		grayScale = new JButton("Gray Scale");
		grayScaleParallel = new JButton("Gray Scale (Parallel)");
		sepiaTone = new JButton("Sepia Tone");
		sepiaToneParallel = new JButton("Sepia Tone (Parallel)");
		blur = new JButton("Blur");
		blurParallel = new JButton("Blur (Parallel)");
		mosaic = new JButton("Mosaic");
		mosaicParallel = new JButton("Mosaic (Parallel)");
		histogramEq = new JButton("Histogram Equalization");
		histogramEqParallel = new JButton("Histogram Equalization (Parallel)");
		atomicParallelHist = new JButton("Histogram Eq. (Atomic Parallel)");
		redEyeParallel = new JButton("Red Eye (Parallel)");
		cloneParallel = new JButton("Seamless Clone (Parallel)");
		
		
		//****************Menu Bar*****************************
		JMenuBar menuBar = new JMenuBar();
		file = new JMenu("File");
		transform = new JMenu("Transform");
		
		open = new JMenuItem("Open");
		close = new JMenuItem("Close");
		save = new JMenuItem("Save");
		saveAs = new JMenuItem("Save As...");
		exit = new JMenuItem("Exit");
		
		
		view = new JMenu("View");
		resizeWindow = new JMenuItem("Resize Window");
		zoom = new JMenuItem("Zoom");
		
		flipHorizontal = new JMenuItem("Horizontal Flip");
		flipVertical = new JMenuItem("Vertical Flip");
		rotateRight = new JMenuItem("Rotate Right");
		rotateLeft = new JMenuItem("Rotate Left");
		
		grayFile = new JMenu("Gray Scale");
		sepiaFile = new JMenu("Sepia");
		blurFile = new JMenu("Blur");
		mosaicFile = new JMenu("Mosaic");
		histogramFile = new JMenu("Histogram Equalization");
		redEyeFile = new JMenu("Red Eye");
		cloneFile = new JMenu("Seamless Clone");
		
		menuBar.add(file);
		file.add(exit);
		file.add(open);
		file.add(close);
		file.add(save);
		file.add(saveAs);
	
		
		menuBar.add(view);
		view.add(resizeWindow);
		view.add(zoom);
		
		menuBar.add(transform);
		transform.add(flipHorizontal);
		transform.add(flipVertical);
		transform.add(rotateRight);
		transform.add(rotateLeft);
		
		menuBar.add(grayFile);
		grayFile.add(grayScale);
		grayFile.add(grayScaleParallel);
		
		menuBar.add(sepiaFile);
		sepiaFile.add(sepiaTone);
		sepiaFile.add(sepiaToneParallel);
		
		menuBar.add(blurFile);
		blurFile.add(blur);
		blurFile.add(blurParallel);
		
		menuBar.add(mosaicFile);
		mosaicFile.add(mosaic);
		mosaicFile.add(mosaicParallel);
		
		menuBar.add(histogramFile);
		histogramFile.add(histogramEq);
		histogramFile.add(histogramEqParallel);
		histogramFile.add(atomicParallelHist);
		
		menuBar.add(redEyeFile);
		redEyeFile.add(redEyeParallel);
		
		menuBar.add(cloneFile);
		cloneFile.add(cloneParallel);
		
		//selectDevice.add(deviceList);
		
		this.setJMenuBar(menuBar);
		

		
		//****************Bottom Panel*****************************
		JPanel bottomPanel = new JPanel();
		bottomPanel.setLayout(new FlowLayout(FlowLayout.TRAILING));
		
		zoomIn = new JButton("+");
		zoomOut = new JButton("-");
		currentZoom = new JLabel("Current Zoom: 100%");
		
		
		bottomPanel.add(zoomIn);
		bottomPanel.add(zoomOut);
		bottomPanel.add(currentZoom);
		
		bottomPanel.add(deviceList, FlowLayout.LEFT);
		
		this.add(bottomPanel, BorderLayout.SOUTH);
		//***************Image Displaying Starts Here************
		BufferedImage defaultImage = getImageFromFile("Images//crop.png");
		paintImage(defaultImage);
		windowResize(defaultImage.getHeight(), defaultImage.getWidth());
		
		originalHeight = defaultImage.getHeight();
		originalWidth = defaultImage.getWidth();
		zoomAmount = 1;
		preZoomImage = image;
		
//		toolBar.add(grayScale);
//		toolBar.add(grayScaleParallel);
//		toolBar.add(sepiaTone);
//		toolBar.add(sepiaToneParallel);
//		toolBar.add(blur);
//		toolBar.add(blurParallel);
//		toolBar.add(mosaic);
//		toolBar.add(mosaicParallel);
//		toolBar.add(histogramEq);
//		toolBar.add(histogramEqParallel);
//		toolBar.add(atomicParallelHist);
//		toolBar.add(redEyeParallel);
//		this.add(toolBar, BorderLayout.NORTH);
		
		

		
		//****************Adding Listeners********************************
		grayScale.addActionListener(ae);
		grayScaleParallel.addActionListener(ae);
		sepiaTone.addActionListener(ae);
		sepiaToneParallel.addActionListener(ae);
		blur.addActionListener(ae);
		blurParallel.addActionListener(ae);
		mosaic.addActionListener(ae);
		mosaicParallel.addActionListener(ae);
		histogramEq.addActionListener(ae);
		histogramEqParallel.addActionListener(ae);
		atomicParallelHist.addActionListener(ae);
		redEyeParallel.addActionListener(ae);
		cloneParallel.addActionListener(ae);
		exit.addActionListener(ae);
		open.addActionListener(ae);
		close.addActionListener(ae);
		save.addActionListener(ae);
		saveAs.addActionListener(ae);
		flipHorizontal.addActionListener(ae);
		flipVertical.addActionListener(ae);
		rotateRight.addActionListener(ae);
		rotateLeft.addActionListener(ae);

		
		resizeWindow.addActionListener(ae);
		zoom.addActionListener(ae);
		
		zoomIn.addActionListener(ae);
		zoomOut.addActionListener(ae);
		
		deviceList.addActionListener(ae);
		
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
		
		if(currentFilePath.equals("Images//crop.png")){
			JOptionPane.showMessageDialog(null, "You Can Not Save Over The Default Image");
		}
		else {

			File outputFile = new File(filePath);

			try {
				String extension = filePath.substring(filePath.length() - 3, filePath.length());


				if(extension.equals("jpg")) {
					BufferedImage saveFormat = new BufferedImage(image.getWidth(), image.getHeight(),
							BufferedImage.TYPE_INT_RGB);
					Graphics g = saveFormat.getGraphics();
					g.drawImage(image, 0, 0, null);

					ImageIO.write(saveFormat, extension, outputFile);
				}
				else {
					ImageIO.write(image, extension, outputFile);
				}

			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "The Location To Save The Image To Was Invalid", "Oops", 
						JOptionPane.ERROR_MESSAGE);
			}
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
	 * Private helper method to control zooming function.
	 * @param zoomLevel The amount to zoom the image.
	 */
	private void setZoom(double zoomLevel) {
			double nextZoom = zoomLevel;

			currentZoom.setText("Current Zoom: " + (int) nextZoom + "%");
		
			if((int) nextZoom > 10) {
				zoomOut.setEnabled(true);
			}
			
			if((int) nextZoom == 20) {
				zoomOut.setEnabled(false);
			}
			else if((int) nextZoom == 100) {
				paintImage(preZoomImage);
				zoomAmount = 1;
			}
			else {
				zoomAmount = nextZoom / 100;
				
				int targetWidth = (int) (originalWidth * zoomAmount);
				int targetHeight = (int) (originalHeight * zoomAmount);
				
				int newImageWidth = originalWidth;
				int newImageHeight = originalHeight;

				BufferedImage resizedImage = null;

				do {
					if(originalWidth < targetWidth) {
						newImageWidth *= 2;
						
						if(newImageWidth > targetWidth) {
							newImageWidth = targetWidth;
						}
					}
					else {
						newImageWidth /= 2;
						
						if(newImageWidth < targetWidth) {
							newImageWidth = targetWidth;
						}
					}
					
					if(originalHeight < targetHeight) {
						newImageHeight *= 2;
						
						if(newImageHeight > targetHeight) {
							newImageHeight = targetHeight;
						}
					}
					else {
						newImageHeight /= 2;
						
						if(newImageHeight < targetWidth) {
							newImageHeight = targetHeight;
						}
					}
					
					resizedImage = new BufferedImage(newImageWidth, newImageHeight, BufferedImage.TYPE_INT_ARGB);
					Graphics g = resizedImage.createGraphics();
					g.drawImage(image, 0, 0, newImageWidth, newImageHeight, null);
					g.dispose();
				} while(newImageWidth != targetWidth && newImageHeight != targetHeight);
				
				

				
				paintImage(resizedImage);
			}
	}
	
	/**
	 * Private helper to prompt for a template needed in red eye reduction.
	 * @return The template.
	 */
	private BufferedImage templatePrompt() {
		JFileChooser fileChooser = new JFileChooser();
		int result = fileChooser.showOpenDialog(null);
		
		BufferedImage template = null;
		
		if(result == JFileChooser.APPROVE_OPTION){
			String filePath = fileChooser.getSelectedFile().getPath();
			File templateFile = new File(filePath);
			
			BufferedImage ri;
			try {
				ri = ImageIO.read(templateFile);
				template = ImageIO.read(templateFile);
				template = new BufferedImage(ri.getWidth(), ri.getHeight(), BufferedImage.TYPE_INT_ARGB);
				Graphics g = template.getGraphics();
				g.drawImage(ri, 0 , 0 , null );		
				
				
				
			} catch (IOException e) {
				JOptionPane.showMessageDialog(null, "The Image Could Not Be Read From A File At The Given Path", "Oops", 
						JOptionPane.ERROR_MESSAGE);
			}
			
			
		}
		else{
			JOptionPane.showMessageDialog(null, "Image Location Not Properly Selected", "Oops", 
					JOptionPane.ERROR_MESSAGE);
		}
		return template;
	}
	
	/**
	 * Private helper method used to close the program.
	 */
	private void exit() {
		CL.clReleaseCommandQueue(parallelControl.getCommandQueue());
		CL.clReleaseContext(parallelControl.getContext());
		
		setVisible(false);
		System.exit(0);
	}
	
	/**
	 * Private inner class used to listen for Action Events such as button clicks.
	 * @author Sean Maloney
	 */
	private class ActionEvents implements ActionListener {
		
		/**Gray Scale
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
					
					BufferedImage img = getImageFromFile(filePath);
					paintImage(img);
					windowResize(img.getHeight(), img.getWidth());
					
					
					originalHeight = img.getHeight();
					originalWidth = img.getWidth();
					zoomAmount = 1;
					currentZoom.setText("Current Zoom: 100%");
					
					preZoomImage = image;
				}
				else{
					JOptionPane.showMessageDialog(null, "Image Location Not Properly Selected", "Oops", 
							JOptionPane.ERROR_MESSAGE);
				}
			}
			else if(action.getSource() == close) {
				if(changeMade == true) {
					Object[] options = {"Yes", "No", "Cancel"};
					int result = JOptionPane.showOptionDialog(null, "Would you like to save your current image?", "Saving...", JOptionPane.YES_NO_CANCEL_OPTION,
							JOptionPane.QUESTION_MESSAGE, null,	options, options[2]);

					if(result != JOptionPane.CANCEL_OPTION) {
						if(result == JOptionPane.YES_OPTION) {
							save(currentFilePath);
						}
					}


					BufferedImage img = getImageFromFile("Images//crop.png");
					paintImage(img);
					windowResize(img.getHeight(), img.getWidth());

					originalHeight = img.getHeight();
					originalWidth = img.getWidth();
					zoomAmount = 1;

					preZoomImage = image;
				}
				changeMade = false;
			}
			else if(action.getSource() == save) {
				
			
				
				Object[] options = {"Yes", "No"};
				int result = JOptionPane.showOptionDialog(null, "This Is Going To Modify The Save Location Of The Current File" + 
						" Are You Sure You'd Like To Continue?", "Saving...", JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE, null,	options, options[1]);
				
				if(result == JOptionPane.YES_OPTION) {
					save(currentFilePath);
				}
				else {
					JOptionPane.showMessageDialog(null, "File Not Saved");
				}
				
				
				
			}
			else if(action.getSource() == saveAs) {
				
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
				double zoomLevel = 0;
				try {
					zoomLevel = Double.parseDouble(JOptionPane.showInputDialog("Enter Zoom %: "));

					if(zoomLevel < 20) {
						JOptionPane.showMessageDialog(null, "Can not zoom below 20%");
					}
					else if(zoomLevel > 500) {
						JOptionPane.showMessageDialog(null, "Can not zoom above 500%");
					}
					else {
						setZoom(zoomLevel);
					}
				}
				catch(IllegalArgumentException iae) {
					JOptionPane.showMessageDialog(null, "You Must Enter Only Numeric Characters");
				}

				
			}
			else if(action.getSource() == zoomIn) {
				double toZoom = (zoomAmount * 100) + 10;
				
				String number = (int) toZoom + "";
				int subtract = Integer.parseInt(number.substring(number.length() - 1, number.length()));
			
				toZoom = toZoom - subtract;
				
				setZoom(toZoom);
			}
			else if(action.getSource() == zoomOut) {
				double toZoom = (zoomAmount * 100) - 10;
				
				
				String number = (int) toZoom + "";
				int subtract = Integer.parseInt(number.substring(number.length() - 1, number.length()));
				int add = 10 - subtract;

				if(add != 10) {
					toZoom = toZoom + add;
				}
			

				
				setZoom(toZoom);
				
			}
			else if(action.getSource() == grayScale) {
				changeMade = true;
				double preEditZoom = zoomAmount;
				
				
				BufferedImage gray = GrayScale.grayScale(preZoomImage);
				
				
				preZoomImage = gray;
				
				paintImage(gray);
				
				
				setZoom(preEditZoom * 100);
			}
			else if(action.getSource() == grayScaleParallel) {	
				changeMade = true;
				double preEditZoom = zoomAmount;
				
				
				BufferedImage gray = GrayScaleParallel.parallelGrayScale(parallelControl.getContext(), 
						parallelControl.getCommandQueue(), parallelControl.getDevice(), preZoomImage);
				
				
				preZoomImage = gray;
				
				paintImage(gray);
				
				setZoom(preEditZoom * 100);
			}
			else if(action.getSource() == sepiaTone) {
				changeMade = true;
				double preEditZoom = zoomAmount;
				
				
				BufferedImage sepia = Sepia.sepia(preZoomImage);
				
				
				preZoomImage = sepia;
				
				paintImage(sepia);
				
				
				setZoom(preEditZoom * 100);
			}
			else if(action.getSource() == sepiaToneParallel) {
				changeMade = true;
				double preEditZoom = zoomAmount;
				
				
				BufferedImage sepia = SepiaParallel.parallelSepia(parallelControl.getContext(), 
						parallelControl.getCommandQueue(), preZoomImage);
				
				
				preZoomImage = sepia;
				
				paintImage(sepia);
				
				setZoom(preEditZoom * 100);
			}
			else if(action.getSource() == blur) {
				changeMade = true;
				double preEditZoom = zoomAmount;
				
				
				BufferedImage blur = Blur.blur(preZoomImage);
				
				
				preZoomImage = blur;
				
				paintImage(blur);
				
				
				setZoom(preEditZoom * 100);
			}
			else if(action.getSource() == blurParallel) {
				changeMade = true;
				double preEditZoom = zoomAmount;
				
				
				BufferedImage blur = BlurParallel.parallelBlur(parallelControl.getContext(), 
						parallelControl.getCommandQueue(), parallelControl.getDevice(), preZoomImage);
				
				
				preZoomImage = blur;
				
				paintImage(blur);
				
				
				setZoom(preEditZoom * 100);
			}
			else if(action.getSource() == mosaic) {
				changeMade = true;
				double preEditZoom = zoomAmount;
				
				
				BufferedImage mosaic = Mosaic.mosaic(preZoomImage);
				
				
				preZoomImage = mosaic;
				
				paintImage(mosaic);
				
				
				setZoom(preEditZoom * 100);
			}
			else if(action.getSource() == mosaicParallel) {
				changeMade = true;
				double preEditZoom = zoomAmount;
				
				BufferedImage mosaic =  MosaicParallel.parallelMosaic(parallelControl.getContext(), 
						parallelControl.getCommandQueue(), parallelControl.getDevice(), preZoomImage);
				
				
				preZoomImage = mosaic;
				
				paintImage(mosaic);
				
				
				setZoom(preEditZoom * 100);
			}
			else if(action.getSource() == histogramEq) {
				changeMade = true;
				double preEditZoom = zoomAmount;
				
				
				BufferedImage histoEqualized = HistogramEquilization.histogramEq(preZoomImage);
				
				
				preZoomImage = histoEqualized;
				
				paintImage(histoEqualized);
				
				
				setZoom(preEditZoom * 100);
			}
			else if(action.getSource() == histogramEqParallel) {
				changeMade = true;
				double preEditZoom = zoomAmount;
				
			
				BufferedImage histoEqualized = HistogramEqualizationParallel.parallelHistogramEq(parallelControl.getContext(), 
						parallelControl.getCommandQueue(), parallelControl.getDevice(), preZoomImage, false);
				
				
				preZoomImage = histoEqualized;
				
				paintImage(histoEqualized);
				
				
				setZoom(preEditZoom * 100);
				
			}
			else if(action.getSource() == atomicParallelHist) {
				changeMade = true;
				double preEditZoom = zoomAmount;
				
			
				BufferedImage histoEqualized = HistogramEqualizationParallel.parallelHistogramEq(parallelControl.getContext(), 
						parallelControl.getCommandQueue(), parallelControl.getDevice(), preZoomImage, true);
				
				
				preZoomImage = histoEqualized;
				
				paintImage(histoEqualized);
				
				
				setZoom(preEditZoom * 100);
			}
			else if(action.getSource() == redEyeParallel) {
				changeMade = true;
				double preEditZoom = zoomAmount;
				
				JOptionPane.showMessageDialog(null, "Select Template Location", "Choose Template File", 
						JOptionPane.INFORMATION_MESSAGE);
				BufferedImage template = templatePrompt();

				if(template != null) {

					int eyeCount = 2;



					try {
						eyeCount = Integer.parseInt(JOptionPane.showInputDialog("Enter Number of Red Eyes: ", 0));

					} catch (NumberFormatException e) {
						JOptionPane.showMessageDialog(null, "Only Numeric Characters May Be Entered, Setting Count to 2");
					}

					if(eyeCount <= 0) {
						JOptionPane.showMessageDialog(null, "Eye Count Must Be Greater Than 0, Setting Count to 2");
					}




					BufferedImage redEyesRemoved =  RedEyeParallel.redEyeRemoval(parallelControl.getContext(),
							parallelControl.getCommandQueue(), parallelControl.getDevice(), 
							template, preZoomImage, eyeCount);


					preZoomImage = redEyesRemoved;

					paintImage(redEyesRemoved);


					setZoom(preEditZoom * 100);
				}
			}
			else if(action.getSource() == cloneParallel) {
				changeMade = true;
				double preEditZoom = zoomAmount;
				
				JOptionPane.showMessageDialog(null, "Select Clone Location", "Choose Clone File", 
						JOptionPane.INFORMATION_MESSAGE);
				BufferedImage clone = templatePrompt();
				
				if(clone != null) {
					
					int iterations = 10;



					try {
						iterations = Integer.parseInt(JOptionPane.showInputDialog("Enter Number of Seam Improvement Iterations: ", 0));

					} catch (NumberFormatException e) {
						JOptionPane.showMessageDialog(null, "Only Numeric Characters May Be Entered, Setting Count to 10");
					}

					if(iterations <= 0) {
						JOptionPane.showMessageDialog(null, "Iteration Count Must Be Greater Than 0, Setting Count to 10");
					}
					
					
					BufferedImage cloneMerged =  SeamlessCloneParallel.seamlessClone(parallelControl.getContext(),
							parallelControl.getCommandQueue(), parallelControl.getDevice(), 
							preZoomImage, clone, iterations);


					preZoomImage = cloneMerged;

					paintImage(cloneMerged);


					setZoom(preEditZoom * 100);
				
				}
			}
			else if(action.getSource() == flipHorizontal) {
				changeMade = true;
				double preEditZoom = zoomAmount;
				
				
				BufferedImage horizontal = TransformationParallel.horizontalFlip(parallelControl.getContext(), 
						parallelControl.getCommandQueue(), parallelControl.getDevice(), preZoomImage, "horizontal_kernel");
				
				
				preZoomImage = horizontal;
				
				paintImage(horizontal);
				
				setZoom(preEditZoom * 100);
			}
			else if(action.getSource() == flipVertical) {
				changeMade = true;
				double preEditZoom = zoomAmount;
				
				
				BufferedImage vertical = TransformationParallel.horizontalFlip(parallelControl.getContext(), 
						parallelControl.getCommandQueue(), parallelControl.getDevice(), preZoomImage, "vertical_kernel");
				
				
				preZoomImage = vertical;
				
				paintImage(vertical);
				
				setZoom(preEditZoom * 100);
				
			}
			else if(action.getSource() == rotateLeft) {
				changeMade = true;
				double preEditZoom = zoomAmount;
				
				
				BufferedImage left = TransformationParallel.horizontalFlip(parallelControl.getContext(), 
						parallelControl.getCommandQueue(), parallelControl.getDevice(), preZoomImage, "left_kernel");
				
				
				preZoomImage = left;
				
				paintImage(left);
				
				setZoom(preEditZoom * 100);

			}
			else if(action.getSource() == rotateRight) {
				changeMade = true;
				double preEditZoom = zoomAmount;
				
				
				BufferedImage right = TransformationParallel.horizontalFlip(parallelControl.getContext(), 
						parallelControl.getCommandQueue(), parallelControl.getDevice(), preZoomImage, "right_kernel");
				
				
				preZoomImage = right;
				
				paintImage(right);
				
				setZoom(preEditZoom * 100);
			}
			else if(action.getSource() == deviceList) {
				String result = (String) deviceList.getSelectedItem();
		
				
				cl_device_id newDevice = deviceMap.get(result).getDeviceId();
				cl_platform_id newPlatform = deviceMap.get(result).getPlatformId();
				
				parallelControl = new ParallelSetUp(newDevice, newPlatform);
				
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
