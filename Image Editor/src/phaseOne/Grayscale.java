package phaseOne;

import java.awt.image.BufferedImage;

/**
 * Control class used to handle the Gray Scale algorithm.
 * 
 * @author Sean Maloney
 */
public class Grayscale extends ImageAlgorithm{

	/**
	 * Converts an the colors of a given image to be in GrayScale.
	 * @param original The original image.
	 * @return The newly re-colored image.
	 */
	public BufferedImage grayScale(BufferedImage original) {
		int width = original.getHeight();
		int height = original.getWidth();
		
		int[] sourceData = strip(original);
		
		int[] resultData = new int[sourceData.length];
		
		for(int row = 0; row < height; row++) {
			for(int col = 0; col < width; col++) {
				int index = row * width + col;
				
				int pixel = sourceData[index];
				
				
			}
		}
		
		wrapUp(resultData, original);
		return null;
	}
	
}
