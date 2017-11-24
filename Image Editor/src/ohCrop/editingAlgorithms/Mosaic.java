package ohCrop.editingAlgorithms;

import java.awt.image.BufferedImage;
import java.util.Random;

import javax.swing.JOptionPane;

import ohCrop.utilAlgorithms.ImageAlgorithm;

/**
 * Class used to apply a mosaic filter to an image.
 * 
 * @author Sean Maloney
 */
public class Mosaic extends ImageAlgorithm {
	/**
	 * Converts an the colors of a given image to be in GrayScale.
	 * 
	 * @param original
	 *            The original image.
	 * @return The newly re-colored image.
	 */
	public static BufferedImage mosaic(BufferedImage original) {
		int tileCount = 1024;
		try {
			tileCount = Integer.parseInt(JOptionPane.showInputDialog("Enter Number of Mosaic Center Points: ", 0));

		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(null, "Only Numeric Characters May Be Entered, Setting Count to 1024");
		}
		
		if(tileCount <= 0) {
			JOptionPane.showMessageDialog(null, "Tile Count Must Be Greater Than 0, Setting Count to 1024");
		}

		int height = original.getHeight();
		int width = original.getWidth();

		int[] sourceData = strip(original);

		int[] resultData = new int[sourceData.length];

		int[] tilePoints = generateAnchors(tileCount, sourceData.length);

		long startTime = System.nanoTime();
		for (int row = 0; row < height; row++) {
			for (int col = 0; col < width; col++) {
				int index = row * width + col;

				// int pixel = sourceData[index];

				// **************Gray Scale Specific Implementation begins here********
				int closestCenter = findClosestCenter(row, col, width, tilePoints);

				int mosaicPixel = sourceData[closestCenter];

				resultData[index] = mosaicPixel;
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
	 * Private helper method used to generate center points for a Mosaic image.
	 * 
	 * @param num
	 *            The number of tiles.
	 * @param imageLength
	 *            The length of the image.
	 * @return A collection of randomly chosen indices to be used as center points.
	 */
	private static int[] generateAnchors(int num, int imageLength) {
		int[] result = new int[num];

		Random rand = new Random();
		for (int i = 0; i < result.length; i++) {
			int nextIndex = rand.nextInt(imageLength);
			result[i] = nextIndex;
		}

		return result;
	}

	/**
	 * Private helper method used to find the closest tile center to the pixel at
	 * (row, col). 
	 * 
	 * @param row
	 *            The x coordinate of the current pixel.
	 * @param col
	 *            The y coordinate of the current pixel.
	 * @param width
	 *            The width of the image.
	 * @param tilePoints
	 *            The collection of center point indexes.
	 * @return The 1-Dimensional index of the closest center point to the pixel at
	 *         (row, col).
	 */
	private static int findClosestCenter(int row, int col, int width, int[] tilePoints) {
		int closestIndex = 0;
		int currentDistance = Integer.MAX_VALUE;

		for (int i = 0; i < tilePoints.length; i++) {
			int currentIndex = tilePoints[i];

			int rowTwo = currentIndex / width;
			int colTwo = currentIndex % width;

			double rowDifSqed = Math.pow((rowTwo - row), 2);
			double colDifSqed = Math.pow((colTwo - col), 2);

			int distance = (int) Math.sqrt(rowDifSqed + colDifSqed);

			if (distance < currentDistance) {
				currentDistance = distance;
				closestIndex = currentIndex;
			}
		}

		return closestIndex;
	}
}
