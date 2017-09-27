package ohCrop.algorithms;

import java.awt.image.BufferedImage;

import javax.swing.JOptionPane;

/**
 * Control class used to handle the Blur algorithm.
 * 
 * @author Sean Maloney
 */
public class Blur extends ImageAlgorithm{
	/**
	 * Performs a Guassian Blur on the given image.
	 * @param original The original image.
	 * @return The newly blurred image.
	 */
	public static BufferedImage blur(BufferedImage original) {

		double[] filter = {0.0232468, 0.0338240, 0.0383276, 0.0338240, 0.0232468,
				0.0338240, 0.0492136, 0.0557663, 0.0492136, 0.0338240,
				0.0383276, 0.0557663, 0.0631915, 0.0557663, 0.0383276,
				0.0338240, 0.0492136, 0.0557663, 0.0492136, 0.0338240,
				0.0232468, 0.0338240, 0.0383276, 0.0338240, 0.0232468};
		
		
		
		int height = original.getHeight();
		int width = original.getWidth();

		int[] sourceData = strip(original);

		int[] resultData = new int[sourceData.length];
		int filterLength = 24;

		long startTime = System.nanoTime();
		
		int[] redArray = new int[sourceData.length];
		int[] blueArray = new int[sourceData.length];
		int[] greenArray = new int[sourceData.length];
		int[] alphaArray = new int[sourceData.length];
		
		double[] redAvg = new double[sourceData.length];
		double[] greenAvg = new double[sourceData.length];
		double[] blueAvg = new double[sourceData.length];
		
		for(int row = 0; row < height; row++) {
			for(int col = 0; col < width; col++) {
				int index = row * width + col;
				
				int pixel = sourceData[index];
				
				int alpha = (pixel & ALPHA_MASK) >> ALPHA_OFFSET;
				int red = (pixel & RED_MASK) >> RED_OFFSET;
				int green = (pixel & GREEN_MASK) >> GREEN_OFFSET;
				int blue = (pixel & BLUE_MASK) >> BLUE_OFFSET;
				
				redArray[index] = red;
				blueArray[index] = blue;
				greenArray[index] = green;
				alphaArray[index] = alpha;
			}
		}
		
		
		
		for(int row = 0; row < height; row++) {
			for(int col = 0; col < width; col++) {
				int index = row * width + col;
				
				//int[] stencil = new int[filterLength];
				int end = index + (filterLength / 2);
				int start = index - (filterLength / 2);
				
				int count = 0;
				
				double newRed = 0;
				double newBlue = 0;
				double newGreen = 0;
				
				for(int i = start; i < end; i++) {
					//stencil[count] = sourceData[i];
					int currentIndex = i;
					
					if(currentIndex > sourceData.length - 1) {
						currentIndex = sourceData.length - 1;
					}
					else if(currentIndex < 0) {
						currentIndex = 0;
					}
					
					newRed += redArray[currentIndex] * filter[count];
					newGreen += greenArray[currentIndex] * filter[count];
					newBlue += blueArray[currentIndex] * filter[count];
					
					count++;
				}
				
				redAvg[index] = newRed;
				greenAvg[index] = newGreen;
				blueAvg[index] = newBlue;
			}
		}
		
		
		for(int row = 0; row < height; row++) {
			for(int col = 0; col < width; col++) {
				int index = row * width + col;
				
				int blurPixel = (alphaArray[index] << ALPHA_OFFSET) | ((int) redAvg[index] << RED_OFFSET) |
						((int) greenAvg[index] << GREEN_OFFSET) | ((int) blueAvg[index] << BLUE_OFFSET);
				
				resultData[index] = blurPixel;
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
