package ohCrop;

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
	public static BufferedImage grayScale(BufferedImage original) {
		
		int width = original.getHeight();
		int height = original.getWidth();
		
		int[] sourceData = strip(original);
		
		int[] resultData = new int[sourceData.length];
		
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
		
		BufferedImage result = wrapUp(resultData, original);
		return result;
	}
	
}
