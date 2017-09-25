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
		
		int height = original.getHeight();
		int width = original.getWidth();
		
		int[] sourceData = strip(original);
		
		int[] resultData = new int[sourceData.length];
		
		
		long startTime = System.nanoTime();
		for(int row = 0; row < height; row++) {
			for(int col = 0; col < width; col++) {
				int index = row * width + col;
				
				
			}
		}

		long endTime = System.nanoTime();
		
		long timeTaken = endTime - startTime;
		
		double miliSeconds = timeTaken / 1000000.0;
		JOptionPane.showMessageDialog(null, "Time Taken: " + miliSeconds + " (ms)");
		
		BufferedImage result = wrapUp(resultData, original);
		return result;
	}
	
	/**
	 * Private helper method used to create a stencil.
	 * @param x X component of center pixel.
	 * @param y Y component of center pixel.
	 * @param length The length of the stencil.
	 * @param width The width of the image raster.
	 * @return The stencil.
	 */
	private int[] createStencil(int x, int y, int length, int width){
		int[] result = new int[length];
		for(int row = 0; row < 3; row++) {
			for(int col = 0; col < 3; col++) {
				int index = row * width + col;
				//There's some sort of math here...i just dont know what
			}
		}
		
		return null;
	}
}
