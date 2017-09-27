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
	public static BufferedImage grayScale(BufferedImage original) {

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
		for(int row = 0; row < height; row++) {
			for(int col = 0; col < width; col++) {
				int index = row * width + col;

				int targetAlpha = (sourceData[index] & ALPHA_MASK) >> ALPHA_OFFSET;

				int[] stencil = new int[filterLength];

				int[] redArray = new int[stencil.length];
				int[] blueArray = new int[stencil.length];
				int[] greenArray = new int[stencil.length];
				int[] alphaArray = new int[stencil.length];

				int end = index + (filterLength / 2);
				int start = index - (filterLength / 2);

//				if(end > sourceData.length) {
//					end = sourceData.length;
//				}
//				if(start < 0) {
//					start = 0;
//				}
				
				int count = 0;
				for(int i = start; i < end; i++) {
					//stencil[count] = sourceData[i];
					int currentIndex = i;
					
					if(currentIndex > sourceData.length) {
						currentIndex = sourceData.length;
					}
					else if(currentIndex < 0) {
						currentIndex = 0;
					}
					
					int pixel = sourceData[currentIndex];
					
					int alpha = (pixel & ALPHA_MASK) >> ALPHA_OFFSET;
					int red = (pixel & RED_MASK) >> RED_OFFSET;
					int green = (pixel & GREEN_MASK) >> GREEN_OFFSET;
					int blue = (pixel & BLUE_MASK) >> BLUE_OFFSET;

					redArray[count] = red;
					blueArray[count] = blue;
					greenArray[count] = green;
					alphaArray[count] = alpha;
				
					count++;
				}
				
				double redWeight = 0;
				double greenWeight = 0;
				double blueWeight = 0;
				
				for(int i = 0; i < redArray.length; i++) {
					int currentRed = redArray[i];
					int currentGreen = greenArray[i];
					int currentBlue = blueArray[i];
					
					double filterWeight = filter[i];
					
					redWeight += currentRed + filterWeight;
					greenWeight += currentGreen + filterWeight;
					blueWeight += currentBlue + filterWeight;
				}
				
				int blurPixel = (targetAlpha << ALPHA_OFFSET) | ((int) redWeight << RED_OFFSET) |
						((int) greenWeight << GREEN_OFFSET) | ((int) blueWeight << BLUE_OFFSET);
			
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
