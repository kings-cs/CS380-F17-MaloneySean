package ohCrop.editingAlgorithms;


import java.awt.image.BufferedImage;

import javax.swing.JOptionPane;

import ohCrop.utilAlgorithms.ImageAlgorithm;

/**
 * Control class used to handle the Gray Scale algorithm.
 * 
 * @author Sean Maloney 
 */
public class GrayScale extends ImageAlgorithm{

	/**
	 * Converts an the colors of a given image to be in GrayScale.
	 * @param original The original image.
	 * @return The newly re-colored image.
	 */
	public static BufferedImage grayScale(BufferedImage original) {
		
		int height = original.getHeight();
		int width = original.getWidth();
		
		int[] sourceData = strip(original);
		
		int[] resultData = new int[sourceData.length];
		
		
		long startTime = System.nanoTime();
		for(int row = 0; row < height; row++) {
			for(int col = 0; col < width; col++) {
				int index = row * width + col;
				
				int pixel = sourceData[index];
				
				//**************Gray Scale Specific Implementation begins here********
				
				int alpha = (pixel & ALPHA_MASK) >> ALPHA_OFFSET;
				
				int red = (pixel & RED_MASK) >> RED_OFFSET;
				int green = (pixel & GREEN_MASK) >> GREEN_OFFSET;
				int blue = (pixel & BLUE_MASK) >> BLUE_OFFSET;
								
				int gray = (int) ((red * 0.299) + (green * 0.587) + (blue * 0.114));
				
				int newRed = gray;
				int newGreen = gray;
				int newBlue = gray;
				
				int grayPixel = (alpha << ALPHA_OFFSET) | (newRed << RED_OFFSET) |
						(newGreen << GREEN_OFFSET) | (newBlue << BLUE_OFFSET);
			
				
				
				resultData[index] = grayPixel;
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
