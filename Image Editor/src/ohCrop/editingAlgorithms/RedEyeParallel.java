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
	 * @param template The image to be modified.
	 * @param resultData The result of 
	 */
	public static void redEyeRemoval(cl_context context, cl_command_queue commandQueue, cl_device_id device, BufferedImage template, BufferedImage original, int[] resultData) {
		
		cl_program program = buildProgram("Kernels/Red_Eye_Kernel", context);
		
		//Template Stuff
		int[] temlateData = strip(template);
		
		int[] rgbTemplateAvergaes = new int[3];
		
		int[] redTemplateChannel = new int[temlateData.length];
		int[] blueTemplateChannel = new int[temlateData.length];
		int[] greenTemplateChannel = new int[temlateData.length];
		
		getChannelAverages(rgbTemplateAvergaes, temlateData, redTemplateChannel, blueTemplateChannel, greenTemplateChannel, 
				context, commandQueue, device, program);
		
		int[] differenceSums = new int[3];
		
		int[][] templateChannels = {redTemplateChannel, greenTemplateChannel, blueTemplateChannel};
		sumDifferences(templateChannels, differenceSums, rgbTemplateAvergaes, context, commandQueue, device, program);
		
		
		//Image Stuff
		int[] data = strip(original);
		int[] redSourceChannel = new int[data.length];
		int[] greenSourceChannel = new int[data.length];
		int[] blueSourceChannel = new int[data.length];
		
		seperateChannels(data, redSourceChannel, blueSourceChannel, greenSourceChannel, context, commandQueue, device, program);
		int[][] sourceChannels = {redSourceChannel, greenSourceChannel, blueSourceChannel};
		
		
		CL.clReleaseProgram(program);
		
	
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
	 * Helper method to compute the averages of each color channel.
	 * 
	 * @param rgbAverages An array containing the red avergae at index 0, green at index 1, and blue at index 2.
	 * @param data The original image data.
	 * @param redChannel outputs as the red channel.
	 * @param blueChannel outputs as the blue channel.
	 * @param greenChannel outputs as the green channel.
	 * @param context The OpenCL context.
	 * @param commandQueue The OpenCL commandQueue.
	 * @param device The OpenCl device.
	 * @param program The OpenCL program.
	 */
	public static void getChannelAverages(int[] rgbAverages, int[] data, int[] redChannel, int[] blueChannel, int[] greenChannel,
			cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
		
		
		//TODO: You're silly and forgot that you already have a kernel for seperating, refactor this later if you have time.
		seperateChannels(data, redChannel, blueChannel, greenChannel, context, commandQueue, device, program);
		
		int[] redSum = reduce(redChannel, context, commandQueue, device, program);
		int[] blueSum = reduce(blueChannel, context, commandQueue, device, program);
		int[] greenSum = reduce(greenChannel, context, commandQueue, device, program);
		
		rgbAverages[0] = redSum[0] / data.length;
		rgbAverages[1] = greenSum[0] / data.length;
		rgbAverages[2] = blueSum[0] / data.length;
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
	private static int[] reduce(int[] redData, cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
				
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
			newResult = reduce(newResult, context, commandQueue, device, program);
		}
		
		
	
		
		CL.clReleaseKernel(kernel);
		CL.clReleaseProgram(padProgram);
		releaseMemObject(objects);
		
		
		
		return newResult;
		
	}
	
	/**
	 * Helper method to calculate the differences between the individual pixels in the template and the channel averages.
	 * @param data A two-dimensional arrays containing the arrays for the red, green, and blue channels.
	 * @param results A two-dimensional array containing the differences for the red, green, and blue channels.
	 * @param average An array containing the individual channel averages, red at index 0, green at index 1, and blue at index 2.
	 * @param context The OpenCL context.
	 * @param commandQueue The OpenCL commandQueue.
	 * @param device The OpenCL device.
	 * @param program The OpenCL program.
	 */
	private static void differenceFromAvg(int[][] data, int[][] results, int[] average, cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
		int[] redChannel = data[0];
		int[] greenChannel = data[1];
		int[] blueChannel = data[2];
		
		
		int[] redResult = results[0];
		int[] greenResult = results[1];
		int[] blueResult = results[2];
		
		int globalSize = redChannel.length;
		int localSize = calculateLocalSize(globalSize, device);
		
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};
		
		int[][] params = {redChannel, greenChannel, blueChannel, redResult,
				greenResult, blueResult, average};
		Pointer[] pointers = createPointers(params);
		Pointer ptrRedResult = pointers[3];
		Pointer ptrGreenResult = pointers[4];
		Pointer ptrBlueResult = pointers[5];
		
		cl_mem[] objects = createMemObjects(params, pointers, context);
		cl_mem memRedResult = objects[3];
		cl_mem memGreenResult = objects[4];
		cl_mem memBlueResult = objects[5];
		
		cl_kernel kernel = CL.clCreateKernel(program, "calculate_differences", null);
		
		setKernelArgs(objects, kernel);
		
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
		
		CL.clEnqueueReadBuffer(commandQueue, memRedResult, 
				CL.CL_TRUE, 0, redResult.length * Sizeof.cl_float,
				ptrRedResult, 0, null, null);
		
		
		CL.clEnqueueReadBuffer(commandQueue, memGreenResult, 
				CL.CL_TRUE, 0, greenResult.length * Sizeof.cl_float,
				ptrGreenResult, 0, null, null);
		
		CL.clEnqueueReadBuffer(commandQueue, memBlueResult, 
				CL.CL_TRUE, 0, blueResult.length * Sizeof.cl_float,
				ptrBlueResult, 0, null, null);
	}
	
	/**
	 * Helper method to calculate the sum of the differnce between the individual pixels and their averages.
	 * @param data A two-dimensional arrays containing the arrays for the red, green, and blue channels.
	 * @param result An array containing the sums, the red sum at index 0, green at index 1, and blue at index 2.
	 * @param averages An array containing the averages, the red at index 0, green at index 1, and blue at index 2.
	 * @param context The OpenCL context.
	 * @param commandQueue The OpenCL commandQueue.
	 * @param device The OpenCL device.
	 * @param program The OpenCL program.
	 */
	public static void sumDifferences(int[][] data, int[] result, int[] averages, cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
		
		int[] redDiffs = new int[data[0].length];
		int[] greenDiffs = new int[data[0].length];
		int[] blueDiffs = new int[data[0].length];
		
		
		int[][] results = {redDiffs, greenDiffs, blueDiffs};
		
		
		
		differenceFromAvg(data, results, averages, context, commandQueue, device, program);

		
		int[] redSumDiff = reduce(redDiffs, context, commandQueue, device, program);
		int[] greenSumDiff = reduce(greenDiffs, context, commandQueue, device, program);
		int[] blueSumDiff = reduce(blueDiffs, context, commandQueue, device, program);
		

		
		result[0] = redSumDiff[0];
		result[1] = greenSumDiff[0];
		result[2] = blueSumDiff[0];
	}
	
	private static void averageImageWithTemplate(int[][] sourceChannels, int[][] templateChannels, int[][] resultAverages, int[] dimensions,
			cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
		int[] sourceRed = sourceChannels[0];
//		int[] sourceGreen = sourceChannels[1];
//		int[] sourceBlue = sourceChannels[2];
		
		int[] templateRed = templateChannels[0];
//		int[] templateGreen = templateChannels[1];
//		int[] templateBlue = templateChannels[2];
		
		int[] redAverages = resultAverages[0];
//		int[] greenAverages = resultAverages[1];
//		int[] blueAverages = resultAverages[2];
		
		int globalSize = sourceRed.length;
		int localSize = calculateLocalSize(globalSize, device);
		
		
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};
		
		int[][] params = {sourceRed, templateRed, redAverages};
		Pointer[] pointers = createPointers(params);
		Pointer ptrRedAverage = pointers[2];
		
		cl_mem[] objects = createMemObjects(params, pointers, context);
		cl_mem memRedAverage = objects[2];
		
		cl_kernel kernel = CL.clCreateKernel(program, "average_with_template", null);
		setKernelArgs(objects, kernel);
		
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
		
		
		CL.clEnqueueReadBuffer(commandQueue, memRedAverage, 
				CL.CL_TRUE, 0, redAverages.length * Sizeof.cl_float,
				ptrRedAverage, 0, null, null);
		
		
	}
	
}
