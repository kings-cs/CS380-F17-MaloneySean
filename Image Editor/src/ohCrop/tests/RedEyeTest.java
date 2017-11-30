package ohCrop.tests;

import static org.junit.Assert.assertEquals;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.jocl.CL;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_device_id;
import org.jocl.cl_program;
import org.junit.Before;
import org.junit.Test;

import ohCrop.editingAlgorithms.RedEyeParallel;
import ohCrop.utilAlgorithms.ParallelAlgorithm;
import ohCrop.utilAlgorithms.ParallelSetUp;

/**
 * Class to test functionality of pieces used in red eye removal.
 * @author Sean Maloney
 */
public class RedEyeTest extends ParallelAlgorithm{

	/**
	 * The template being used.
	 */
	private BufferedImage template;
	
	/**
	 * The image being used.
	 */
	private BufferedImage original;
	
	/**
	 * The setup object being used.
	 */
	private ParallelSetUp setup;
	
	/**
	 * The OpenCL program.
	 */
	private cl_program program;
	
	/**
	 * The OpenCL context.
	 */
	private cl_context context;
	
	/**
	 * The OpenCL command queue.
	 */
	private cl_command_queue commandQueue;
	
	/**
	 * The OpenCL device.
	 */
	private cl_device_id device;
	
	/**
	 * Enables OpenCL exceptions.
	 */
	@Before
	public void enableExceptions() {
		CL.setExceptionsEnabled(true);
		
		
		File templateFile = new File("Images//Red Eye Template.png");
		 template = null;
		File originalFile = new File("Images//Red Eye Original.png");
		 original = null;
		
		try {
			BufferedImage ri = ImageIO.read(templateFile);
			template = ImageIO.read(templateFile);
			template = new BufferedImage(ri.getWidth(), ri.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics g = template.getGraphics();
			g.drawImage(ri, 0 , 0 , null );		
			
		
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "The Image Could Not Be Read From A File At The Given Path", "Oops", 
					JOptionPane.ERROR_MESSAGE);
		}
		
		
		try {
			BufferedImage oRi = ImageIO.read(originalFile);
			original = ImageIO.read(originalFile);
			original = new BufferedImage(oRi.getWidth(), oRi.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics gO = original.getGraphics();
			gO.drawImage(oRi, 0 , 0 , null );	
			
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "The Image Could Not Be Read From A File At The Given Path", "Oops", 
					JOptionPane.ERROR_MESSAGE);
		}
		
		setup = new ParallelSetUp();
		
		context = setup.getContext();
		commandQueue = setup.getCommandQueue();
		device = setup.getDevice();
		
		program = buildProgram("Kernels/Red_Eye_Kernel", context);
		
	}
	
	/**
	 * Tests the averaging of the channels.
	 */
	@Test
	public void testAverageChannels() {
		//int[] data = {1, 2, 3, 4, 5, 6};
		
		
		
		
		int[] data = strip(template);
		
		
		
		int redAvg = 0;
		int blueAvg = 0;
		int greenAvg = 0;
		
		for(int i = 0; i < data.length; i++) {
			int pixel = data[i];
			int red = (pixel & RED_MASK) >> RED_OFFSET;
			int green = (pixel & GREEN_MASK) >> GREEN_OFFSET;
			int blue = (pixel & BLUE_MASK) >> BLUE_OFFSET;
		
			redAvg += red;
			blueAvg += blue;
			greenAvg += green;
		}
		
		redAvg = redAvg / data.length;
		blueAvg = blueAvg / data.length;
		greenAvg = greenAvg / data.length;
		
		
		
		int[] result = new int[3];
		
		

		int[] redChannel = new int[data.length];
		int[] blueChannel = new int[data.length];
		int[] greenChannel = new int[data.length];
		
		RedEyeParallel.getChannelAverages(result, data, redChannel, blueChannel, greenChannel, context, commandQueue, device, program);
		
		CL.clReleaseProgram(program);
		
		
		
		assertEquals("The red average should equal: " + redAvg, redAvg, result[0]);
		assertEquals("The green average should equal: " + greenAvg, greenAvg, result[1]);
		assertEquals("The blue average should equal: " + blueAvg, blueAvg, result[2]);
		
	}
	
	/**
	 * Test method to determine if the sum of the channel differences is correct.
	 */
	@Test
	public void testSumDifference() {
		int[] templateData = strip(template);
		//int[] resultData = new int[data.length];
		
		
		int[] averages = new int[3];

		int[] redChannel = new int[templateData.length];
		int[] greenChannel = new int[templateData.length];
		int[] blueChannel = new int[templateData.length];
		
		
		RedEyeParallel.getChannelAverages(averages, templateData, redChannel, blueChannel, greenChannel, context, commandQueue, device, program);
		
		int redAvg = averages[0];
		int greenAvg = averages[1];
		int blueAvg = averages[2];
		
		int redDiff = 0;
		int greenDiff = 0;
		int blueDiff = 0;
	
		
		for(int i = 0; i < redChannel.length; i++) {
			
			
			
			redDiff += (redChannel[i] - redAvg) * (redChannel[i] - redAvg);
			greenDiff += (greenChannel[i] - greenAvg) * (greenChannel[i] - greenAvg);
			blueDiff += (blueChannel[i] - blueAvg) * (blueChannel[i] - blueAvg);
		}

		int[] result = new int[3];
		int[][] channelData = {redChannel, greenChannel, blueChannel};
		int[] redUnsquared = new int[templateData.length];
		int[] greenUnsquared = new int[templateData.length];
		int[] blueUnsquared = new int[templateData.length];
		int[][] unsquared = {redUnsquared, greenUnsquared, blueUnsquared};
		
		
		RedEyeParallel.sumDifferences(channelData, result, unsquared, averages, context, commandQueue, device, program);
		
		
		assertEquals("The red sum of differences should equal: " + redDiff, redDiff, result[0]);
		assertEquals("The green sum of differences should equal: " + greenDiff, greenDiff, result[1]);
		assertEquals("The blue sum of differences should equal: " + blueDiff, blueDiff, result[2]);
		
//		for(int i = 0; i < redUnsquared.length; i++) {
//			System.out.println(redUnsquared[i]);
//		}
		
	
		CL.clReleaseProgram(program);
		
	}
	
	/**
	 * Tests that the channels get averaged correctly according to the template.
	 */
	@Test
	public void testSumDifferencesFromTemplate() {
		int[] data = strip(original);
	
		
//		int[] templateData = strip(template);
		int[] resultData = new int[data.length];
		
		RedEyeParallel.redEyeRemoval(context, commandQueue, device, template, original, resultData);
		
	
		
	}
	
//	/**
//	 * Private helper that was supposed to do shit but probably isnt gonna get used.
//	 * @param sourceChannels The source channels.
//	 * @param averages The averages.
//	 * @param dimArray The dimArrays.
//	 */
//	private void calculateSumDiffsFromAverage(int[][] sourceChannels, int[][] averages, int[] dimArray) {
//		//TODO: Delete help?
//		
//		int[] sourceRed = sourceChannels[0];
//		int[] sourceGreen = sourceChannels[1];
//		int[] sourceBlue = sourceChannels[2];
//		
//		int[] redAverages = averages[0];
//		int[] greenAverages = averages[1];
//		int[] blueAverages = averages[2];
//		
//		
//		
//		int width = dimArray[0];
//		int height = dimArray[1];
//		int templateWidth = dimArray[2];
//		int templateHeight = dimArray[3];
//		
//		
//		int maxColTrav = templateWidth / 2;
//		int maxRowTrav = templateHeight / 2;
//		int templateSize = (templateWidth * templateHeight);
//
//		
//		
//		for(int index = 0; index < sourceRed.length; index++) {
//			
//			
//			int newRed = 0;
//			int newBlue = 0;
//			int newGreen = 0;
//			
//			int divisor = templateSize;
//			
//			int row = index / width;
//			int col = index % width;
//							
//			for(int i = row - maxRowTrav; i <= row + maxRowTrav; i++) {
//				for(int j = col - maxColTrav; j <= col + maxColTrav; j++) {
//								
//					int newRow = i;
//					int newCol = j;
//									
//									
//									
//					
//							
//							
//					if(newRow < 0 || newRow > height - 1 || newCol < 0 || newCol > width - 1){
//						divisor--;
//					}
//					else{
//						int currentIndex = newRow * width + newCol;
//								
//						newRed += sourceRed[currentIndex];
//						newGreen += sourceGreen[currentIndex];
//						newBlue += sourceBlue[currentIndex];	
//					}
//									
//					
//									
//				
//				}
//			}
//							
//
//							
//			redAverages[index] = newRed / divisor;
//			greenAverages[index] = newGreen / divisor;
//			blueAverages[index] = newBlue / divisor;
//
//
//		
//
//			int redDiff = 0;
//			int greenDiff = 0;
//			int blueDiff = 0;
//
//			for(int i = row - maxRowTrav; i <= row + maxRowTrav; i++) {
//				for(int j = col - maxColTrav; j <= col + maxColTrav; j++) {
//								
//					int newRow = i;
//					int newCol = j;
//									
//									
//									
//					
//							
//							
//					if(newRow < 0 || newRow > height - 1 || newCol < 0 || newCol > width - 1){
//						//Legit Nothing
//					}
//					else{
//						int currentIndex = newRow * width + newCol;
//								
//						int currentRed = sourceRed[currentIndex] - redAverages[index];		
//						int currentGreen = sourceGreen[currentIndex] - greenAverages[index];
//						int currentBlue = sourceBlue[currentIndex] - blueAverages[index];
//						
//						redDiff += (currentRed * currentRed);
//						greenDiff += (currentGreen * currentGreen);
//						blueDiff += (currentBlue * currentBlue);
//
//					}
//									
//					
//									
//					
//				}
//			}
//
//
//			redAverages[index] = redDiff;
//			greenAverages[index] = greenDiff;
//			blueAverages[index] = blueDiff;
//			
//		}
//		
//	}
	
}
