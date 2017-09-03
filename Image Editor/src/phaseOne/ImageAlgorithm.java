package phaseOne;

import java.awt.Point;
import java.awt.image.BufferedImage;
import java.awt.image.DataBuffer;
import java.awt.image.DataBufferInt;
import java.awt.image.Raster;
import java.awt.image.WritableRaster;

/**
 * Super class to handle stripping away and reassembling image data needed for image processing algorithms.
 * @author Sean Maloney
 */
public class ImageAlgorithm {
	
	/**
	 * The mask for the encoded alpha value of a pixel.
	 */
	protected static final int ALPHA_MASK = 0xff000000;
	
	/**
	 * The offset for the encoded alpha value of a pixel.
	 */
	protected static final int ALPHA_OFFSET = 24;
	
	/**
	 * The mask for the encoded red value of a pixel.
	 */
	protected static final int RED_MASK = 0x00ff0000;
	
	/**
	 * The offset for the encoded red value of a pixel.
	 */
	protected static final int RED_OFFSET = 16;
	
	/**
	 * The mask for the encoded green value of a pixel.
	 */
	protected static final int GREEN_MASK = 0x0000ff00;
	
	/**
	 * The offset for the encoded green value of a pixel.
	 */
	protected static final int GREEN_OFFSET = 8;
	
	/**
	 * The mask for the encoded blue value of a pixel.
	 */
	protected static final int BLUE_MASK = 0x000000ff;
	
	/**
	 * The offset for the encoded blue value of a pixel.
	 */
	protected static final int BLUE_OFFSET = 0;

	
	/**
	 * Gets the source data from the raster of a given buffered image.
	 * @param source The BufferedImage whose raster will be modified.
	 * @return The source data of the BufferedImage
	 */
	public static int[] strip(BufferedImage source){
		WritableRaster sourceRaster = source.getRaster();
		DataBuffer sourceDataBuffer = sourceRaster.getDataBuffer();
		DataBufferInt sourceBytes = (DataBufferInt) sourceDataBuffer;
		
		int[] sourceData = sourceBytes.getData();
		
		return sourceData;
	}
	
	/**
	 * Writes the new created resultData to a BufferedImage.
	 *
	 * @param resultData The data created from the Image Processing algorithm
	 * @param source The original image.
	 * @return The new BufferedImage.
	 */
	public static BufferedImage wrapUp(int[] resultData, BufferedImage source) {
		BufferedImage result = new BufferedImage(source.getHeight(), source.getWidth(),
				BufferedImage.TYPE_INT_ARGB);
		
		DataBufferInt resultDataBuffer = new DataBufferInt(resultData, resultData.length);
		Raster resultRaster = Raster.createRaster(source.getSampleModel(), resultDataBuffer, new Point(0, 0));
		
		result.setData(resultRaster);
		
		return result;
	}

}
