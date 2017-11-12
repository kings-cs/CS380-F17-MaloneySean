package ohCrop.editingAlgorithms;
 
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
		

		long startTime = System.nanoTime();
		
		int[] redArray = new int[sourceData.length];
		int[] blueArray = new int[sourceData.length];
		int[] greenArray = new int[sourceData.length];
		int[] alphaArray = new int[sourceData.length];
		
		int[] redAvg = new int[sourceData.length];
		int[] greenAvg = new int[sourceData.length];
		int[] blueAvg = new int[sourceData.length];
		
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
				
	
				int count = 0;
				
				double newRed = 0;
				double newBlue = 0;
				double newGreen = 0;
				
				
				int maxTrav = 2;
				
				for(int i = row - maxTrav; i <= row + maxTrav; i++) {
					for(int j = col - maxTrav; j <= col + maxTrav; j++) {
						
						int newRow = i;
						int newCol = j;
						
						
						
						if(newRow < 0) {
							newRow = 0;
						}
						else if(newRow > height - 1) {
							newRow = height - 1;
						}
						
						if(newCol < 0) {
							newCol = 0;
						}
						else if(newCol > width - 1) {
							newCol = width - 1;
						}
						
						int currentIndex = newRow * width + newCol;
						
						newRed += redArray[currentIndex] * filter[count];
						newGreen += greenArray[currentIndex] * filter[count];
						newBlue += blueArray[currentIndex] * filter[count];
						
						count++;
					}
				}
				

				
				redAvg[index] = (int) newRed;
				greenAvg[index] = (int) newGreen;
				blueAvg[index] = (int) newBlue;
			}
		}
		
		
		for(int row = 0; row < height; row++) {
			for(int col = 0; col < width; col++) {
				int index = row * width + col;
				//System.out.println("RED AVG: " + redAvg[index]);
				
				
			
				
				int blurPixel = (alphaArray[index] << ALPHA_OFFSET) | (redAvg[index] << RED_OFFSET) |
						(greenAvg[index] << GREEN_OFFSET) | (blueAvg[index] << BLUE_OFFSET);
				
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
