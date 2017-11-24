package ohCrop.editingAlgorithms;

import java.awt.image.BufferedImage;

import javax.swing.JOptionPane;

import ohCrop.utilAlgorithms.ImageAlgorithm;

/**
 * Serial Implementaion of Histogram Equalization.
 * 
 * @author Sean Maloney
 */
public class HistogramEquilization extends ImageAlgorithm {
	// 1. Calculate histogram of the grayscale values in the source image. For this
	// first part of the
	// project, you should do the simple thing and use atomics.
	// 2. Calculate the cumulative frequency distribution of source image histogram.
	// 3. Calculate an idealized histogram by dividing the number of pixels in your
	// image with the
	// number of bins in your histogram.
	// 4. Calculate the cumulative frequency distribution of the idealized
	// histogram.
	// 5. Design the mapping comparing the cumulative frequency distribution of the
	// idealized histogram
	// with the cumulative frequency distribution of the source image.
	// 6. Map each pixel of the source image to the new intensity value.

	/**
	 * Performs a Histogram Equalization on a given image.
	 * 
	 * @param original
	 *            The original image.
	 * @return The equalized image.
	 */
	public static BufferedImage histogramEq(BufferedImage original) {
		// int height = original.getHeight();
		// int width = original.getWidth();

		int[] sourceData = strip(original);

		// int[] resultData = new int[sourceData.length];

		long startTime = System.nanoTime();

		int[] histogram = calculateHistogram(sourceData);

		int[] distribution = cumulativeFrequencyDistribution(histogram);

		int[] ideal = idealizeHistogram(histogram, sourceData.length);

		int[] idealCum = cumulativeFrequencyDistribution(ideal);

		int[] mapping = mapHistogram(distribution, idealCum);

		int[] resultData = mapPixels(mapping, sourceData);

		long endTime = System.nanoTime();

		long timeTaken = endTime - startTime;

		double miliSeconds = timeTaken / 1000000.0;
		JOptionPane.showMessageDialog(null, "Time Taken: " + miliSeconds + " (ms)");

		BufferedImage result = wrapUp(resultData, original);
		return result;
	}

	//TODO: MAKE ALL THESE METHODS PRIVATE AFTER GETTING PARALLEL FUNCTIONING
	/**
	 * Calculates the histogram of the given image.
	 * 
	 * @param imageRaster
	 *            The image that is being equalized.
	 * @return An array representaion of the histogram.
	 */
	public static int[] calculateHistogram(int[] imageRaster) {

		// int numBins = 8;
		// TODO: UNCOMMENT THIS
		int numBins = 256;
		int[] bins = new int[numBins];

		for (int i = 0; i < numBins; i++) {
			bins[i] = 0;
		}

		for (int i = 0; i < imageRaster.length; i++) {
			int currentValue = (imageRaster[i] & RED_MASK) >> RED_OFFSET;
			bins[currentValue]++;
		}

		return bins;
	}

	/**
	 * Calculates the Cumulative Frequency Distribution of a given histogram.
	 * 
	 * @param bins
	 *            The given histogram.
	 * @return The cumulative distribution in an array.
	 */
	public static int[] cumulativeFrequencyDistribution(int[] bins) {
		int[] distribution = new int[bins.length];

		for (int i = bins.length - 1; i >= 0; i--) {
			for (int j = i; j >= 0; j--) {
				distribution[i] += bins[j];
			}
		}

		return distribution;
	}

	/**
	 * Idealizes a given histogram.
	 * 
	 * @param histogram
	 *            The histogram to be converted.
	 * @param pixelCount
	 *            The amount of pixels in the image.
	 * @return The new Ideal histogram.
	 */
	public static int[] idealizeHistogram(int[] histogram, int pixelCount) {
		int[] ideal = new int[histogram.length];

		int avg = pixelCount / histogram.length;
		int leftOver = pixelCount - (avg * histogram.length);
		int mid = (histogram.length - 1) / 2;
		int extraStart = mid - (leftOver / 2);
		int extraEnd = extraStart + leftOver;

		for (int i = 0; i < histogram.length; i++) {

			if (i >= extraStart && i < extraEnd) {
				ideal[i] = avg + 1;
			} else {
				ideal[i] = avg;
			}

		}

		return ideal;
	}

	/**
	 * Maps values from the original frequency distribution to the cumulative
	 * frequency distribuition.
	 * 
	 * @param cuf
	 *            The original frequency distribution.
	 * @param cuFeq
	 *            THe frequency distribution of the of the idealized histogram.
	 * @return The design of the mapping.
	 */
	public static int[] mapHistogram(int[] cuf, int[] cuFeq) {
		int[] mapping = new int[cuf.length];

		for (int i = 0; i < mapping.length; i++) {
			int mapKey = 0;
			int current = cuf[i];

			int bestDif = Integer.MAX_VALUE;
			int lastDif = Integer.MAX_VALUE;

			int j = 0;
			boolean foundClosest = false;
			// TODO: This is overly complicated maloney.
			while (!foundClosest && j < mapping.length) {
				int feqCurrent = cuFeq[j];
				int currentDif = Math.abs(current - feqCurrent);

				if (currentDif < bestDif) {
					bestDif = currentDif;
					mapKey = j;
				}

				// TODO: If this ends up bugged, comment this out until bug is found.
				lastDif = currentDif;
				if (lastDif > currentDif) {
					foundClosest = true;
				}

				j++;
			}

			mapping[i] = mapKey;
		}

		return mapping;
	}

	/**
	 * Creates a new image according to the mapping generated.
	 * 
	 * @param mapping
	 *            The mapping used to create the new image.
	 * @param original
	 *            The original image.
	 * @return The new image.
	 */
	public static int[] mapPixels(int[] mapping, int[] original) {
		int[] result = new int[original.length];

		for (int index = 0; index < original.length; index++) {
			int pixel = original[index];

			int grayVal = (pixel & RED_MASK) >> RED_OFFSET;

			int newGray = mapping[grayVal];

			int alpha = (pixel & ALPHA_MASK) >> ALPHA_OFFSET;

			int resultColor = (alpha << ALPHA_OFFSET) | (newGray << RED_OFFSET) | (newGray << BLUE_OFFSET)
					| (newGray << GREEN_OFFSET);
			result[index] = resultColor;

		}

		return result;

	}

	// /**
	// * USED FOR TESTING ONLY.
	// * @param args Not Used.
	// */
	// public static void main(String[] args) {
	// int[] test = {4, 4, 4, 4, 4,
	// 3, 4, 5, 4, 3,
	// 3, 5, 5, 5, 3,
	// 3, 4, 5, 4, 3,
	// 4, 4, 4, 4, 4};
	//
	//
	// int[] histogram = calculateHistogram(test);
	// System.out.println("HISTOGRAM");
	// for(int i : histogram) {
	// System.out.print(i + " | ");
	// }
	// System.out.println();
	// System.out.println("CUMULATIVE FREQ");
	// int[] distribution = cumulativeFrequencyDistribution(histogram);
	// for(int i : distribution) {
	// System.out.print(i + " | ");
	// }
	//
	// System.out.println();
	// System.out.println("IDEAL HISTOGRAM");
	// int[] ideal = idealizeHistogram(histogram, test.length);
	// for(int i : ideal) {
	// System.out.print(i + " | ");
	// }
	//
	// System.out.println();
	// System.out.println("IDEAL CUMULATIVE FREQ");
	// int[] idealCum = cumulativeFrequencyDistribution(ideal);
	// for(int i : idealCum) {
	// System.out.print(i + " | ");
	// }
	//
	// System.out.println();
	// System.out.println("MAPPING");
	// int[] mapping = mapHistogram(distribution, idealCum);
	// for(int i : mapping) {
	// System.out.print(i + " | ");
	// }
	//
	// System.out.println();
	// System.out.println("MAP");
	// int[] newImage = mapPixels(mapping, test);
	// for(int i = 0; i < newImage.length; i++) {
	// System.out.print(newImage[i] + " | ");
	// if((i + 1) % 5 == 0) {
	// System.out.println();
	// }
	// }
	// }

}
