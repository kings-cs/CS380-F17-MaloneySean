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
		
		
		File currentPicture = new File("Images//Red Eye Template.png");
		 original = null;
		try {
			BufferedImage ri = ImageIO.read(currentPicture);
			original = ImageIO.read(currentPicture);
			original = new BufferedImage(ri.getWidth(), ri.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics g = original.getGraphics();
			g.drawImage(ri, 0 , 0 , null );			
		
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
		
		
		
		
		int[] data = strip(original);
		
		
		
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
		int[] data = strip(original);
		int[] resultData = new int[data.length];
		
		
		int[] result = new int[3];

		int[] redChannel = new int[data.length];
		int[] greenChannel = new int[data.length];
		int[] blueChannel = new int[data.length];
		
		
		RedEyeParallel.getChannelAverages(result, data, redChannel, blueChannel, greenChannel, context, commandQueue, device, program);
		
		int redAvg = result[0];
		int greenAvg = result[1];
		int blueAvg = result[2];
		
		int redDiff = 0;
		int greenDiff = 0;
		int blueDiff = 0;
		
		for(int i = 0; i < redChannel.length; i++) {
			redDiff += redChannel[i] - redAvg;
			greenDiff += greenChannel[i] - greenAvg;
			blueDiff += blueChannel[i] - blueAvg;
		}
		
		System.out.println("RED DIFF: " + redDiff);
		System.out.println("GREEN DIFF: " + greenDiff);
		System.out.println("BLUE DIFF: " + blueDiff);
		
		RedEyeParallel.redEyeRemoval(context, commandQueue, device, original, resultData);
		
		CL.clReleaseProgram(program);
		
	}
	
}
