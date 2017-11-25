package ohCrop.editingAlgorithms;

import java.awt.image.BufferedImage;


import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_program;

import ohCrop.utilAlgorithms.KernelReader;
import ohCrop.utilAlgorithms.ParallelAlgorithm;

/**
 * Control class used to handle the Sepia Tone algorithm in parallel.
 * 
 * @author Sean Maloney
 *
 */
public class SepiaParallel extends ParallelAlgorithm{
	/**
	 * Converts the individual pixels of an image t be in shades of gray computed using parallelism.
	 * 
	 * @param context The OpenCL context used for the parallel computing.
	 * @param commandQueue The OpenCL commandQueue used for the parallel computing.
	 * @param original The image to be colored.
	 * 
	 * @return The newly colored image.  
	 */
	public static BufferedImage parallelSepia(cl_context context, cl_command_queue commandQueue, BufferedImage original) {
		
		
		
		int[] imageRaster = strip(original);
		int[] resultData = new int[imageRaster.length];
		int[][] params = {imageRaster, resultData};
		
		
		Pointer[] pointers = createPointers(params);
		Pointer ptrResult = pointers[1];
		

		
		
		cl_mem[] objects = createMemObjects(params, pointers, context) ;
		cl_mem memResult = objects[1];
		//KERNEL EXECUTION, SHOULD PROBABLY SPLIT THESE UP
		
		//Create the program from the source code
		//Create the OpenCL kernel from the program
		String source = KernelReader.readFile("Kernels/Sepia_Kernel");
		
		//System.out.println(source);
		
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] {source}, null, null);
		
		
		//Build the program
		CL.clBuildProgram(program, 0, null, null, null, null);
		
		//Create the kernel
		cl_kernel kernel = CL.clCreateKernel(program, "sepia_kernel", null);
		
		//Set the arguments for the kernel
		setKernelArgs(objects, kernel);
		
		//Set the work-item dimensions
		long[] globalWorkSize = new long[] {resultData.length};
		long[] localWorkSize = new long[] {1};
		
		
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
}
