package ohCrop.editingAlgorithms;

import java.awt.image.BufferedImage;
import java.util.Random;

import javax.swing.JOptionPane;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_program;

import ohCrop.utilAlgorithms.KernelReader;
import ohCrop.utilAlgorithms.ParallelAlgorithm;

/**
 * Class used to create a Mosaic image, computed in Parallel.
 * @author Sean Maloney
 */
public class MosaicParallel extends ParallelAlgorithm{

	/**
	 * Converts the individual pixels of an image t be in shades of gray computed using parallelism.
	 * 
	 * @param context The OpenCL context used for the parallel computing.
	 * @param commandQueue The OpenCL commandQueue used for the parallel computing.
	 * @param original The image to be colored.
	 * @param device The device used by OpenCL.
	 * 
	 * @return The newly colored image.
	 */
	public static BufferedImage parallelMosaic(cl_context context, cl_command_queue commandQueue, cl_device_id device, BufferedImage original) {
		int tileCount = 1024;
		try {
			tileCount = Integer.parseInt(JOptionPane.showInputDialog("Enter Number of Mosaic Center Points: ", 0));

		} catch (NumberFormatException e) {
			JOptionPane.showMessageDialog(null, "Only Numeric Characters May Be Entered, Setting Count to 1024");
		}
		
		if(tileCount <= 0) {
			JOptionPane.showMessageDialog(null, "Tile Count Must Be Greater Than 0, Setting Count to 1024");
		}
		
		
		int[] imageRaster = strip(original);
		int[] resultData = new int[imageRaster.length];
		int[] dimensions = {original.getWidth(), original.getHeight(), tileCount};
		int[] tilePoints = generateAnchors(tileCount, imageRaster.length);
		
		int[][] params = {imageRaster, resultData, dimensions, tilePoints};
		
		Pointer[] pointers = createPointers(params);
		Pointer ptrResult = pointers[1];
				
		cl_mem[] objects = createMemObjects(params, pointers, context) ;
		cl_mem memResult = objects[1];
		
		
		//KERNEL EXECUTION, SHOULD PROBABLY SPLIT THESE UP
		
		//Create the program from the source code
		//Create the OpenCL kernel from the program
		String source = KernelReader.readFile("Kernels/Mosaic_Kernel");
		
		//System.out.println(source);
		
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] {source}, null, null);
		
		
		//Build the program
		CL.clBuildProgram(program, 0, null, null, null, null);
		
		//Create the kernel
		cl_kernel kernel = CL.clCreateKernel(program, "mosaic_kernel", null);
		
		//Set the arguments for the kernel
		setKernelArgs(objects, kernel);
	
		//WORK GROUP STUFF		
		

		
		int globalSize = imageRaster.length;
		int localSize = calculateLocalSize(globalSize, device);




		//Set the work-item dimensions
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};

		long startTime = System.nanoTime();
		//Execute the kernel
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
		long endTime = System.nanoTime();
		
		
		displayTimeTaken(startTime, endTime);
		
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memResult, 
				CL.CL_TRUE, 0, resultData.length * Sizeof.cl_float,
				ptrResult, 0, null, null);
		
		
		BufferedImage result = wrapUp(resultData, original);
		
				
		//Release kernel, program, 
		CL.clReleaseKernel(kernel);
		CL.clReleaseProgram(program);
		releaseMemObject(objects);
		
		
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
	
}
