package ohCrop.algorithms;

import java.awt.image.BufferedImage;
import java.util.Random;

import javax.swing.JOptionPane;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_program;

/**
 * Class used to create a Mosaic image, computed in Parallel.
 * @author Sean Maloney
 */
public class ParallelMosaic extends ImageAlgorithm{

	/**
	 * Converts the individual pixels of an image t be in shades of gray computed using parallelism.
	 * 
	 * @param context The OpenCL context used for the parallel computing.
	 * @param commandQueue The OpenCL commandQueue used for the parallel computing.
	 * @param original The image to be colored.
	 * 
	 * @return The newly colored image.
	 */
	public static BufferedImage parallelMosaic(cl_context context, cl_command_queue commandQueue, BufferedImage original) {
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
		int[] dimensions = {original.getWidth(), original.getHeight()};
		int[] tilePoints = generateAnchors(tileCount, imageRaster.length);
		
		Pointer ptrRaster = Pointer.to(imageRaster);
		Pointer ptrResult = Pointer.to(resultData);
		Pointer ptrDimensions = Pointer.to(dimensions);
		Pointer ptrTiles = Pointer.to(tilePoints);
		
		cl_mem memRaster = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * imageRaster.length, ptrRaster, null);
		cl_mem memResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * resultData.length, ptrResult, null);
		cl_mem memDimensions = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * dimensions.length, ptrDimensions, null);
		cl_mem memTiles = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * dimensions.length, ptrTiles, null);
		
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
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memRaster));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memResult));
		CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memDimensions));
		CL.clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(memTiles));
	
		//Set the work-item dimensions
		long[] globalWorkSize = new long[] {resultData.length};
		long[] localWorkSize = new long[] {1};
		
		
		long startTime = System.nanoTime();
		//Execute the kernel
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
		long endTime = System.nanoTime();
		
		long timeTaken = endTime - startTime;
		
		double miliSeconds = timeTaken / 1000000.0;
		JOptionPane.showMessageDialog(null, "Time Taken: " + miliSeconds + " (ms)");
		
		
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memResult, 
				CL.CL_TRUE, 0, resultData.length * Sizeof.cl_float,
				ptrResult, 0, null, null);
		
		
		BufferedImage result = wrapUp(resultData, original);
		
				
		//Release kernel, program, 
		CL.clReleaseKernel(kernel);
		CL.clReleaseProgram(program);
		CL.clReleaseMemObject(memRaster);
		CL.clReleaseMemObject(memResult);
		CL.clReleaseMemObject(memDimensions);
		
		
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
