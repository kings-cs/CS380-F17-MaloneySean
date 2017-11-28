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
		
		int[] rgbAvergaes = new int[3];
		
		
		getChannelAverages(rgbAvergaes, data, context, commandQueue, device, program);
		
		CL.clReleaseProgram(program);
		
	
	}
	
	
	/**
	 * Helper method to compute the averages of each color channel.
	 * 
	 * @param rgbAverages An array containing the red avergae at index 0, green at index 1, and blue at index 2.
	 * @param data The original image data.
	 * @param context The OpenCL context.
	 * @param commandQueue The OpenCL commandQueue.
	 * @param device The OpenCl device.
	 * @param program The OpenCL program.
	 */
	public static void getChannelAverages(int[] rgbAverages, int[] data, cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
		int[] redChannel = new int[data.length];
		int[] blueChannel = new int[data.length];
		int[] greenChannel = new int[data.length];
		
		seperateChannels(data, redChannel, blueChannel, greenChannel, context, commandQueue, device, program);
		
		int[] redSum = averageChannels(redChannel, context, commandQueue, device, program);
		int[] blueSum = averageChannels(blueChannel, context, commandQueue, device, program);
		int[] greenSum = averageChannels(greenChannel, context, commandQueue, device, program);
		
		rgbAverages[0] = redSum[0] / data.length;
		rgbAverages[1] = greenSum[0] / data.length;
		rgbAverages[2] = blueSum[0] / data.length;
	}
	
	/**
	 * Seperates the individual channels of an image.
	 * 
	 * @param data The original source data from the image.
	 * @param redChannel The redChannel only.
	 * @param blueChannel The blueChannel only.
	 * @param greenChannel The greenChannel only.
	 * @param context The OpenCL context.
	 * @param commandQueue The OpenCL commandQueue.
	 * @param device The OpenCL device.
	 * @param program The OpenCL program.
	 */
	private static void seperateChannels(int[] data, int[] redChannel, int[] blueChannel, int[] greenChannel,
			cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
		
		int globalSize = data.length;
		int localSize = calculateLocalSize(globalSize, device);
		
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};
		
		int[][] params = {data, redChannel, blueChannel, greenChannel};
		
		Pointer[] pointers = createPointers(params);
		Pointer ptrRed = pointers[1];
		Pointer ptrBlue = pointers[2];
		Pointer ptrGreen = pointers[3];
		
		cl_mem[] objects = createMemObjects(params, pointers, context);
		cl_mem memRed = objects[1];
		cl_mem memBlue = objects[2];
		cl_mem memGreen = objects[3];
		
		cl_kernel kernel = CL.clCreateKernel(program, "seperate_channels", null);
		setKernelArgs(objects, kernel);

		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
		
		
		CL.clEnqueueReadBuffer(commandQueue, memRed, 
				CL.CL_TRUE, 0, redChannel.length * Sizeof.cl_float,
				ptrRed, 0, null, null);
		
		CL.clEnqueueReadBuffer(commandQueue, memBlue, 
				CL.CL_TRUE, 0, blueChannel.length * Sizeof.cl_float,
				ptrBlue, 0, null, null);
		
		CL.clEnqueueReadBuffer(commandQueue, memGreen, 
				CL.CL_TRUE, 0, greenChannel.length * Sizeof.cl_float,
				ptrGreen, 0, null, null);
	}
	
	/**
	 * Averages the RGB color channels of a given input.
	 * @param context The OpenCL context used.
	 * @param commandQueue The OpenCL commandQueue used.
	 * @param device The OpenCL device used.
	 * @param redData The image to be modified.
	 * @param program The OpenCL program used.
	 * @return An array containing the three averages.
	 */
	private static int[] averageChannels(int[] redData, cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
				
		int maxSize = getMaxWorkGroupSize(device);
		int padSize = getPadSize(redData, maxSize);
		int[] paddedData = new int[padSize];
		
		cl_program padProgram = buildProgram("Kernels/Scan_Kernel", context);
		padArray(redData, paddedData, padSize, maxSize, context, commandQueue, device, padProgram);
		
		
		int globalSize = paddedData.length;
		int localSize = calculateLocalSize(globalSize, device);
		
		
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};
		
		
		int[] result = new int[globalSize / localSize];
//		int[] blueResult = new int[globalSize / localSize];
//		int[] greenResult = new int[globalSize / localSize];
		
		
		
		int[][] params = {paddedData, result};
		
		Pointer[] pointers = createPointers(params);
		Pointer ptrResult = pointers[1];
		
		cl_mem[] objects = createMemObjects(params, pointers, context);
		cl_mem memResult = objects[1];
		
		cl_kernel kernel = CL.clCreateKernel(program, "average_channels", null);
		
		setKernelArgs(objects, kernel);
		
		
		
		
		
		CL.clSetKernelArg(kernel, 2, Sizeof.cl_int * localWorkSize[0], null);
//		CL.clSetKernelArg(kernel, 3, Sizeof.cl_int * localWorkSize[0], null);
//		CL.clSetKernelArg(kernel, 4, Sizeof.cl_int * localWorkSize[0], null);
		
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
		
		
		CL.clEnqueueReadBuffer(commandQueue, memResult, 
				CL.CL_TRUE, 0, result.length * Sizeof.cl_float,
				ptrResult, 0, null, null);
		
		int[] newResult = result;
	
		
		if(result.length > 1) {
			newResult = averageChannels(newResult, context, commandQueue, device, program);
		}
		
		
	
		
		CL.clReleaseKernel(kernel);
		CL.clReleaseProgram(padProgram);
		releaseMemObject(objects);
		
		
		
		return newResult;
		
	}
	
}
