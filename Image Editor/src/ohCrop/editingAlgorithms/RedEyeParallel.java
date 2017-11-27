package ohCrop.editingAlgorithms;



import java.awt.image.BufferedImage;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_program;


import ohCrop.utilAlgorithms.ParallelAlgorithm;

/**
 * Algorithm to remove Red Eye's from images.
 * @author Sean Maloney
 */
public class RedEyeParallel extends ParallelAlgorithm{

	
	/**
	 * Removes Red Eyes from the given image.
	 * @param context The OpenCL context used.
	 * @param commandQueue The OpenCL commandQueue used.
	 * @param device The OpenCL device used.
	 * @param original The image to be modified.
	 * @param resultData The result of 
	 */
	public static void redEyeRemoval(cl_context context, cl_command_queue commandQueue, cl_device_id device, BufferedImage original, int[] resultData) {
		
		cl_program program = buildProgram("Kernels/Red_Eye_Kernel", context);
		int[] data = strip(original);
		
		
		
		int[] averages = averageChannels(data, context, commandQueue, device, program);
	
		for(int i = 0; i < averages.length; i++) {
			System.out.println(averages[i]);
		}
		
		
		CL.clReleaseProgram(program);
		
		
		
	}
	
	/**
	 * Averages the RGB color channels of a given input.
	 * @param context The OpenCL context used.
	 * @param commandQueue The OpenCL commandQueue used.
	 * @param device The OpenCL device used.
	 * @param data The image to be modified.
	 * @param program The OpenCL program used.
	 * @return An array containing the three averages.
	 */
	private static int[] averageChannels(int[] data, cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
				
		int maxSize = getMaxWorkGroupSize(device);
		int padSize = getPadSize(data, maxSize);
		int[] paddedData = new int[padSize];
		
		cl_program padProgram = buildProgram("Kernels/Scan_Kernel", context);
		padArray(data, paddedData, padSize, maxSize, context, commandQueue, device, padProgram);
		
		int[] paddedResultData = new int[paddedData.length];
		
		int[] dimensions = {data.length};
		
		int[][] params = {paddedData, paddedResultData, dimensions};
		
		Pointer[] pointers = createPointers(params);
		Pointer ptrResult = pointers[1];
		
		cl_mem[] objects = createMemObjects(params, pointers, context);
		cl_mem memResult = objects[1];
		
		cl_kernel kernel = CL.clCreateKernel(program, "average_channels", null);
		
		setKernelArgs(objects, kernel);
		
		
		int globalSize = paddedData.length;
		int localSize = calculateLocalSize(globalSize, device);
		
		
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};
		
		
		CL.clSetKernelArg(kernel, 3, Sizeof.cl_int * localWorkSize[0], null);
		CL.clSetKernelArg(kernel, 4, Sizeof.cl_int * localWorkSize[0], null);
		CL.clSetKernelArg(kernel, 5, Sizeof.cl_int * localWorkSize[0], null);
		
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
		
		
		CL.clEnqueueReadBuffer(commandQueue, memResult, 
				CL.CL_TRUE, 0, paddedResultData.length * Sizeof.cl_float,
				ptrResult, 0, null, null);
		
		
		

	
		CL.clReleaseKernel(kernel);
		CL.clReleaseProgram(padProgram);
		releaseMemObject(objects);
		
		
		
		
		return paddedResultData;
	}
	
}
