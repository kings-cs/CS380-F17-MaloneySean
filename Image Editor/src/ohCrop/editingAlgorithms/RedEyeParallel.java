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
import ohCrop.utilAlgorithms.RadixSort;

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
	 * @param template The template used to modify the image.
	 * @param original The image to be modified.
	 * @param resultData The result of 
	 */
	public static void redEyeRemoval(cl_context context, cl_command_queue commandQueue, cl_device_id device, BufferedImage template, BufferedImage original, int[] resultData) {
		
		cl_program program = buildProgram("Kernels/Red_Eye_Kernel", context);
		
		//Template Stuff
		int[] templateData = strip(template);
		
		int[] rgbTemplateAvergaes = new int[3];
		
		int[] redTemplateChannel = new int[templateData.length];
		int[] blueTemplateChannel = new int[templateData.length];
		int[] greenTemplateChannel = new int[templateData.length];
		
		getChannelAverages(rgbTemplateAvergaes, templateData, redTemplateChannel, blueTemplateChannel, greenTemplateChannel, 
				context, commandQueue, device, program);
		
		int[] differenceSums = new int[3];
		
		int[][] templateChannels = {redTemplateChannel, greenTemplateChannel, blueTemplateChannel};
		
		int[] redUnsquared = new int[templateData.length];
		int[] greenUnsquared = new int[templateData.length];
		int[] blueUnsquared = new int[templateData.length];
		
		int[][] unsquared = {redUnsquared, greenUnsquared, blueUnsquared};
		sumDifferences(templateChannels, differenceSums, unsquared, rgbTemplateAvergaes, context, commandQueue, device, program);
		
		
		//Image Stuff
		int[] data = strip(original);
		int[] redSourceChannel = new int[data.length];
		int[] greenSourceChannel = new int[data.length];
		int[] blueSourceChannel = new int[data.length];
		
		seperateChannels(data, redSourceChannel, blueSourceChannel, greenSourceChannel, context, commandQueue, device, program);
		
		
		int[][] sourceChannels = {redSourceChannel, greenSourceChannel, blueSourceChannel};
		
		
		int[] redAveragesFromTemplate = new int[data.length];
		int[] greenAveragesFromTemplate = new int[data.length];
		int[] blueAveragesFromTemplate = new int[data.length];
		int[][] resultAverages = {redAveragesFromTemplate, greenAveragesFromTemplate, blueAveragesFromTemplate};
		
		int[] dimensions = {original.getWidth(), original.getHeight(), template.getWidth(), template.getHeight()};
		
		float[] nccArray = new float[data.length];
		//int[] nccArray = new int[data.length];
		
		calculateNcc(sourceChannels, resultAverages, unsquared, differenceSums, dimensions, nccArray,
				context, commandQueue, device, program);
		
		
		
		
		
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
		
		releaseMemObject(objects);
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
		int padSize = getPadSize(redData.length, maxSize);
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
	 * @param unsquared A two-dimensional arrays containing the unsquared differences, red at index 0, green at index 1, and blue at index 2.
	 * @param average An array containing the individual channel averages, red at index 0, green at index 1, and blue at index 2.
	 * @param context The OpenCL context.
	 * @param commandQueue The OpenCL commandQueue.
	 * @param device The OpenCL device.
	 * @param program The OpenCL program.
	 */
	private static void differenceFromAvg(int[][] data, int[][] results, int[][] unsquared, int[] average, cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
		int[] redChannel = data[0];
		int[] greenChannel = data[1];
		int[] blueChannel = data[2];
		
		
		int[] redResult = results[0];
		int[] greenResult = results[1];
		int[] blueResult = results[2];
		
		int[] redUnsquared = unsquared[0];
		int[] greenUnsquared = unsquared[1];
		int[] blueUnsquared = unsquared[2];
		
		int globalSize = redChannel.length;
		int localSize = calculateLocalSize(globalSize, device);
		
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};
		
		int[][] params = {redChannel, greenChannel, blueChannel, redResult,
				greenResult, blueResult, average, 
				redUnsquared, greenUnsquared, blueUnsquared};
		Pointer[] pointers = createPointers(params);
		Pointer ptrRedResult = pointers[3];
		Pointer ptrGreenResult = pointers[4];
		Pointer ptrBlueResult = pointers[5];
		Pointer ptrRedUnsquared = pointers[7];
		Pointer ptrGreenUnsquared = pointers[8];
		Pointer ptrBlueUnsquared = pointers[9];
		
		cl_mem[] objects = createMemObjects(params, pointers, context);
		cl_mem memRedResult = objects[3];
		cl_mem memGreenResult = objects[4];
		cl_mem memBlueResult = objects[5];
		cl_mem memRedUnsquared = objects[7];
		cl_mem memGreenUnsquared = objects[8];
		cl_mem memBlueUnsquared = objects[9];
		
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
	
	
	
		CL.clEnqueueReadBuffer(commandQueue, memRedUnsquared, 
				CL.CL_TRUE, 0, redUnsquared.length * Sizeof.cl_float,
				ptrRedUnsquared, 0, null, null);
		
		
		CL.clEnqueueReadBuffer(commandQueue, memGreenUnsquared, 
				CL.CL_TRUE, 0, greenUnsquared.length * Sizeof.cl_float,
				ptrGreenUnsquared, 0, null, null);
		
		CL.clEnqueueReadBuffer(commandQueue, memBlueUnsquared, 
				CL.CL_TRUE, 0, blueUnsquared.length * Sizeof.cl_float,
				ptrBlueUnsquared, 0, null, null);
		
		releaseMemObject(objects);
	}
	
	/**
	 * Helper method to calculate the sum of the difference between the individual pixels and their averages.
	 * @param data A two-dimensional arrays containing the arrays for the red, green, and blue channels.
	 * @param result An array containing the sums, the red sum at index 0, green at index 1, and blue at index 2.
	 * @param unsquared A two-dimensional arrays containing the unsquared differences, red at index 0, green at index 1, and blue at index 2.
	 * @param averages An array containing the averages, the red at index 0, green at index 1, and blue at index 2.
	 * @param context The OpenCL context.
	 * @param commandQueue The OpenCL commandQueue.
	 * @param device The OpenCL device.
	 * @param program The OpenCL program.
	 */
	public static void sumDifferences(int[][] data, int[] result, int[][] unsquared, int[] averages, cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
		
		int[] redDiffs = new int[data[0].length];
		int[] greenDiffs = new int[data[0].length];
		int[] blueDiffs = new int[data[0].length];
		
		
		int[][] results = {redDiffs, greenDiffs, blueDiffs};
		
		
		
		differenceFromAvg(data, results, unsquared, averages, context, commandQueue, device, program);

		
		int[] redSumDiff = reduce(redDiffs, context, commandQueue, device, program);
		int[] greenSumDiff = reduce(greenDiffs, context, commandQueue, device, program);
		int[] blueSumDiff = reduce(blueDiffs, context, commandQueue, device, program);
		

		
		result[0] = redSumDiff[0];
		result[1] = greenSumDiff[0];
		result[2] = blueSumDiff[0];
	}
	
	/**
	 * Calculates the average per channel for the portion of the image overlapped by the template.
	 * @param sourceChannels The color channels from the source image.
	 * @param resultAverages The resulting averages for each pixel.
	 * @param unsquared The unsquared values of a pixel in the template and the associated average.
	 * @param templateDiffs The sum of the differences for each channel in the template. 
	 * @param dimensions An array containing the dimensions of the original image and the template.
	 * @param nccArray The resulting ncc values.
	 * @param context The OpenCL context.
	 * @param commandQueue The OpenCL command queue.
	 * @param device The OpenCL device.
	 * @param program The OpenCL program.
	 */
	private static void calculateNcc(int[][] sourceChannels,  int[][] resultAverages, int[][] unsquared, int[]templateDiffs, int[] dimensions, float[]nccArray,
			cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
		int[] sourceRed = sourceChannels[0];
		int[] sourceGreen = sourceChannels[1];
		int[] sourceBlue = sourceChannels[2];
		
//		int[] templateRed = templateChannels[0];
//		int[] templateGreen = templateChannels[1];
//		int[] templateBlue = templateChannels[2];
		
		int[] redAverages = resultAverages[0];
		int[] greenAverages = resultAverages[1];
		int[] blueAverages = resultAverages[2];
		
		int globalSize = sourceRed.length;
		int localSize = calculateLocalSize(globalSize, device);
		
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};
		
		
		int[] redSumDiffs = new int[sourceRed.length];
		int[] greenSumDiffs = new int[sourceGreen.length];
		int[] blueSumDiffs = new int[sourceBlue.length];
		
		int[] redUnsquared = unsquared[0];
		int[] greenUnsquared = unsquared[1];
		int[] blueUnsquared = unsquared[2];
		
		
		int[] redProductDiffs = new int[sourceRed.length];
		int[] greenProductDiffs = new int[sourceGreen.length];
		int[] blueProductDiffs = new int[sourceBlue.length];
		
		int[] redTemplateDiffs = {templateDiffs[0]};
		int[] greenTemplateDiffs = {templateDiffs[1]};
		int[] blueTemplateDiffs = {templateDiffs[2]};
		
		int[][] params = {sourceRed, sourceGreen, sourceBlue, 
				redAverages, greenAverages, blueAverages, dimensions,
				redSumDiffs, greenSumDiffs, blueSumDiffs,
				redUnsquared, greenUnsquared, blueUnsquared,
				redProductDiffs, greenProductDiffs, blueProductDiffs,
				redTemplateDiffs, greenTemplateDiffs, blueTemplateDiffs /*,nccArray*/};
		
		
		Pointer[] pointers = createPointers(params);
		Pointer ptrRedAverage = pointers[3];
		Pointer ptrGreenAverage = pointers[4];
		Pointer ptrBlueAverage = pointers[5];
		
		Pointer ptrRedSumDiffs = pointers[7];
		Pointer ptrGreenSumDiffs = pointers[8];
		Pointer ptrBlueSumDiffs = pointers[9];
		
		Pointer ptrRedProductDiffs = pointers[13];
		Pointer ptrGreenProductDiffs = pointers[14];
		Pointer ptrBlueProductDiffs = pointers[15];
		
		Pointer ptrNcc = Pointer.to(nccArray);
		
		cl_mem[] objects = createMemObjects(params, pointers, context);
		cl_mem memRedAverage = objects[3];
		cl_mem memGreenAverage = objects[4];
		cl_mem memBlueAverage = objects[5];
		
		cl_mem memRedSumDiffs = objects[7];
		cl_mem memGreenSumDiffs = objects[8];
		cl_mem memBlueSumDiffs = objects[9];
		
		cl_mem memRedProductDiffs = objects[13];
		cl_mem memGreenProductDiffs = objects[14];
		cl_mem memBlueProductDiffs = objects[15];
		
		//cl_mem memNcc = objects[19];
		
		cl_mem memNcc = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * nccArray.length, ptrNcc, null);
		
		cl_kernel kernel = CL.clCreateKernel(program, "calculate_ncc", null);
		setKernelArgs(objects, kernel);
		
		CL.clSetKernelArg(kernel, 19, Sizeof.cl_mem, Pointer.to(memNcc));
		
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
		
		//TODO: A Lot of these read buffers are gonna get removed
		
		CL.clEnqueueReadBuffer(commandQueue, memRedAverage, 
				CL.CL_TRUE, 0, redAverages.length * Sizeof.cl_float,
				ptrRedAverage, 0, null, null);
		
		CL.clEnqueueReadBuffer(commandQueue, memGreenAverage, 
				CL.CL_TRUE, 0, greenAverages.length * Sizeof.cl_float,
				ptrGreenAverage, 0, null, null);
		
		CL.clEnqueueReadBuffer(commandQueue, memBlueAverage, 
				CL.CL_TRUE, 0, blueAverages.length * Sizeof.cl_float,
				ptrBlueAverage, 0, null, null);
		
		CL.clEnqueueReadBuffer(commandQueue, memRedSumDiffs, 
				CL.CL_TRUE, 0, redSumDiffs.length * Sizeof.cl_float,
				ptrRedSumDiffs, 0, null, null);
		
		CL.clEnqueueReadBuffer(commandQueue, memGreenSumDiffs, 
				CL.CL_TRUE, 0, greenSumDiffs.length * Sizeof.cl_float,
				ptrGreenSumDiffs, 0, null, null);
		
		CL.clEnqueueReadBuffer(commandQueue, memBlueSumDiffs, 
				CL.CL_TRUE, 0, blueSumDiffs.length * Sizeof.cl_float,
				ptrBlueSumDiffs, 0, null, null);

		
		CL.clEnqueueReadBuffer(commandQueue, memRedProductDiffs, 
				CL.CL_TRUE, 0, redProductDiffs.length * Sizeof.cl_float,
				ptrRedProductDiffs, 0, null, null);
		
		CL.clEnqueueReadBuffer(commandQueue, memGreenProductDiffs, 
				CL.CL_TRUE, 0, greenProductDiffs.length * Sizeof.cl_float,
				ptrGreenProductDiffs, 0, null, null);
		
		CL.clEnqueueReadBuffer(commandQueue, memBlueProductDiffs, 
				CL.CL_TRUE, 0, blueProductDiffs.length * Sizeof.cl_float,
				ptrBlueProductDiffs, 0, null, null);
		
		CL.clEnqueueReadBuffer(commandQueue, memNcc, 
				CL.CL_TRUE, 0, nccArray.length * Sizeof.cl_float,
				ptrNcc, 0, null, null);
		
		
		
		float[] minNcc = getMinNcc(nccArray, context, commandQueue, device, program);
	
		int[] nccInts = new int[nccArray.length];
		int[] keys = new int[nccInts.length];
		
		convertNccToPosInt(nccArray, minNcc, nccInts, keys, context, commandQueue, device, program);
		
	

		
		int[] sortedNcc = new int[nccInts.length];
		int[] sortedKeys = new int[nccInts.length];
		

			
		
		RadixSort.sort(nccInts, keys, sortedNcc, sortedKeys, 14, context, commandQueue, device);
		
		for(int i = 0; i < sortedNcc.length; i++) {
			System.out.println("V: " + sortedNcc[i] + " K: " + sortedKeys[i]);
		}
		
		releaseMemObject(objects);
		
	}
	

	/**
	 * Converts an array of ncc values stored as floats to be positive integers.
	 * @param nccArray The array of ncc values.
	 * @param minNcc The smallest ncc value.
	 * @param resultData The resulting integers.
	 * @param keys An empty array to be intialized to contain keys to be sorted.
	 * @param context The OpenCL context.
	 * @param commandQueue The OpenCL command queue.
	 * @param device The OpenCL device.
	 * @param program The OpenCL program.
	 */
	private static void convertNccToPosInt(float[] nccArray, float[] minNcc, int[] resultData, int[] keys,
			cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
		
		
		int globalSize = nccArray.length;
		int localSize = calculateLocalSize(globalSize, device);
		
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};
		
		Pointer ptrNccArray = Pointer.to(nccArray);
		Pointer ptrMinNcc = Pointer.to(minNcc);
		Pointer ptrResultData = Pointer.to(resultData);
		Pointer ptrKeys = Pointer.to(keys);
		
		cl_mem memPaddedData = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * nccArray.length, ptrNccArray, null);
		cl_mem memMinNcc = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * minNcc.length, ptrMinNcc, null);
		cl_mem memResultData = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * resultData.length, ptrResultData, null);
		cl_mem memKeys = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * keys.length, ptrKeys, null);
		
		
		cl_mem[] objects = {memPaddedData, memMinNcc, memResultData, memKeys};
		
		cl_kernel kernel = CL.clCreateKernel(program, "convert_ncc", null);
		setKernelArgs(objects, kernel);
		
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
		
		CL.clEnqueueReadBuffer(commandQueue, memResultData, 
				CL.CL_TRUE, 0, resultData.length * Sizeof.cl_float,
				ptrResultData, 0, null, null);
		
		CL.clEnqueueReadBuffer(commandQueue, memKeys, 
				CL.CL_TRUE, 0, keys.length * Sizeof.cl_float,
				ptrKeys, 0, null, null);
		
	}
	
	
	/**
	 * Helper to get the minimum of all the ncc values, stored in an array of floats.
	 * @param nccArray The array of ncc values.
	 * @param context The OpenCL context.
	 * @param commandQueue The OpenCL command queue.
	 * @param device The OpenCL device.
	 * @param program The OpenCL program.
	 * @return A single element array containing the min.
	 */
	private static float[] getMinNcc(float[] nccArray, cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
		
		int maxSize = getMaxWorkGroupSize(device);
		int padSize = getPadSize(nccArray.length, maxSize);
		float[] paddedData = new float[padSize];
		
		
		

		padNccForMin(nccArray, paddedData, padSize, context, commandQueue, device, program);
		
		
		int globalSize = paddedData.length;
		int localSize = calculateLocalSize(globalSize, device);
		
		
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};
		
		
		float[] result = new float[globalSize / localSize];

		
		

		
		
		Pointer ptrPaddedData = Pointer.to(paddedData);
		Pointer ptrResult = Pointer.to(result);
		
		
		cl_mem memPaddedData = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * paddedData.length, ptrPaddedData, null);
		cl_mem memResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * result.length, ptrResult, null);
		
		cl_mem objects[] = {memPaddedData, memResult};
		
		
		cl_kernel kernel = CL.clCreateKernel(program, "min_float_reduce", null);
		
		setKernelArgs(objects, kernel);
		
		
		
		
		
		CL.clSetKernelArg(kernel, 2, Sizeof.cl_float * localWorkSize[0], null);

		
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
		
		
		CL.clEnqueueReadBuffer(commandQueue, memResult, 
				CL.CL_TRUE, 0, result.length * Sizeof.cl_float,
				ptrResult, 0, null, null);
		
		float[] newResult = result;
	
		
		if(result.length > 1) {
			newResult = getMinNcc(newResult, context, commandQueue, device, program);
		}
		
		
	
		
		CL.clReleaseKernel(kernel);
		releaseMemObject(objects);
		
		
		
		return newResult;
		
	}
	
	
	/**
	 * Pads an array of floats with positive infinity so that it can be min reduced.
	 * @param nccArray The array to be padded.
	 * @param paddedNcc The padded array.
	 * @param padSize The size to pad to.
	 * @param context The OpenCL context.
	 * @param commandQueue The OpenCL command queue.
	 * @param device The OpenCL device.
	 * @param program The OpenCL program.
	 */
	private static void padNccForMin(float[] nccArray, float[] paddedNcc, int padSize,
			cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
		int[] padSizeArray = {padSize};
		
		Pointer ptrNcc = Pointer.to(nccArray);
		Pointer ptrResult = Pointer.to(paddedNcc);
		Pointer ptrPadSize = Pointer.to(padSizeArray);
		
		cl_mem memNcc = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * nccArray.length, ptrNcc, null);
		cl_mem memResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * paddedNcc.length, ptrResult, null);
		cl_mem memPadSize = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * padSizeArray.length, ptrPadSize, null);
		
		
		cl_mem[] objects = {memNcc, memResult, memPadSize};
		
		cl_kernel kernel = CL.clCreateKernel(program, "pad_float_min", null);
		setKernelArgs(objects, kernel);
		
		long[] globalWorkSize = new long[] { nccArray.length };
		long[] localWorkSize = new long[] { calculateLocalSize(nccArray.length, device) };
	
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize, 0, null, null);
		
		CL.clEnqueueReadBuffer(commandQueue, memResult, CL.CL_TRUE, 0, paddedNcc.length * Sizeof.cl_float,
				ptrResult, 0, null, null);
	
		
		
		releaseMemObject(objects);
	}	
}
