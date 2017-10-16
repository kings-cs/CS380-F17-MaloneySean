package ohCrop.algorithms;

import java.awt.image.BufferedImage;

import javax.swing.JOptionPane;


/**
 * Control class used to handle the Sepia tone algorithm.
 * 
 * @author Sean Maloney
 */
public class Sepia extends ImageAlgorithm{

	/**
	 * Sepia Depth constant.
	 */
	private static final int SEPIA_DEPTH = 20;
	
	
	/** 
	 * Sepia Intensity constant.
	 */
	private static final int SEPIA_INTENSITY = 30;
	
	
	/**
	 * Converts an the colors of a given image to be in Sepia tone.
	 * @param original The original image.
	 * @return The newly re-colored image.
	 */
	public static BufferedImage sepia(BufferedImage original) {
		
		int height = original.getHeight();
		int width = original.getWidth();
		
		int[] sourceData = strip(original);
		
		int[] resultData = new int[sourceData.length];
		
		
		long startTime = System.nanoTime();
		for(int row = 0; row < height; row++) {
			for(int col = 0; col < width; col++) {
				int index = row * width + col;
				
				int pixel = sourceData[index];
				
				//**************Sepia Specific Implementation begins here********
				
				int alpha = (pixel & ALPHA_MASK) >> ALPHA_OFFSET;
				
				int red = (pixel & RED_MASK) >> RED_OFFSET;
				int green = (pixel & GREEN_MASK) >> GREEN_OFFSET;
				int blue = (pixel & BLUE_MASK) >> BLUE_OFFSET;
				
				int average = (red + green + blue) / 3;
				
				int newRed = average + (SEPIA_DEPTH * 2);
				int newBlue = average - SEPIA_INTENSITY;
				int newGreen = average + SEPIA_DEPTH;
				
				if(newRed > 255) {
					newRed = 255;
				}
				else if(newRed < 0) {
					newRed = 0;
				}
				
				if(newBlue > 255) {
					newBlue = 255;
				}
				else if(newBlue < 0) {
					newBlue = 0;
				}
				
				
				if(newGreen > 255) {
					newGreen = 255;
				}
				else if(newGreen < 0) {
					newGreen = 0;
				}
				
			
				int sepiaPixel = (alpha << ALPHA_OFFSET) | (newRed << RED_OFFSET) |
						(newGreen << GREEN_OFFSET) | (newBlue << BLUE_OFFSET);
				
				
				resultData[index] = sepiaPixel;
			}
		}

		long endTime = System.nanoTime();
		
		long timeTaken = endTime - startTime;
		
		double miliSeconds = timeTaken / 1000000.0;
		JOptionPane.showMessageDialog(null, "Time Taken: " + miliSeconds + " (ms)");
		
		BufferedImage result = wrapUp(resultData, original);
		return result;
	}
	
	
}
