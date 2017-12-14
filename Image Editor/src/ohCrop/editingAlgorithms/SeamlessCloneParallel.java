package ohCrop.editingAlgorithms;

import java.awt.image.BufferedImage;

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

import ohCrop.utilAlgorithms.ParallelAlgorithm;

/**
 * Class to create a seamless clone of two images.
 * @author Sean Maloney
 */
public class SeamlessCloneParallel extends ParallelAlgorithm {

	/**
	 * Field to keep track of the time taken across all kernels.
	 */
	private static long TIME;
	
	/**
	 * Creates a seamless clone of two images.
	 * @param context The openCL context.
	 * @param commandQueue The openCL commandQueue.
	 * @param device The openCL device. 
	 * @param scene The openCL scene.
	 * @param clone The openCL clones.
	 * @param iterations The amount of iterations to run. 
	 * @return The combined image.
	 */
	public static BufferedImage seamlessClone(cl_context context, cl_command_queue commandQueue, cl_device_id device, BufferedImage scene, BufferedImage clone, int iterations) {
		TIME = 0;
		
		BufferedImage result = null;
		
		cl_program program = buildProgram("Kernels/Clone_Kernel", context);
		
		int[] cloneData = strip(clone);
		float[] redCloneChannel = new float[cloneData.length];
		float[] greenCloneChannel = new float[cloneData.length];
		float[] blueCloneChannel = new float[cloneData.length];
		float[] alphaCloneChannel = new float[cloneData.length];
		
		
		convertChannelsToFloats(cloneData, redCloneChannel, blueCloneChannel, greenCloneChannel, alphaCloneChannel, context, commandQueue, device, program);
		
		int[] mask = new int[cloneData.length];
		int[] cloneDimensions = {clone.getHeight(), clone.getWidth()};
		findMaskValues(cloneDimensions, alphaCloneChannel, mask, context, commandQueue, device, program);
		
		int[] sceneData = strip(scene);
		float[] redSceneChannel = new float[sceneData.length];
		float[] greenSceneChannel = new float[sceneData.length];
		float[] blueSceneChannel = new float[sceneData.length];
		float[] alphaSceneChannel = new float[sceneData.length];
		convertChannelsToFloats(sceneData, redSceneChannel, blueSceneChannel, greenSceneChannel, alphaSceneChannel, context, commandQueue, device, program);
		
		int[] initial = new int[sceneData.length];
		initialGuess(sceneData, cloneData, mask, initial, context, commandQueue, device, program);
		
		
		
		int[] previousIteration = initial;
		float[] redMergedChannel = new float[sceneData.length];
		float[] greenMergedChannel = new float[sceneData.length];
		float[] blueMergedChannel = new float[sceneData.length];
		float[] alphaMergedChannel = new float[sceneData.length];
		convertChannelsToFloats(previousIteration, redMergedChannel, blueMergedChannel, greenMergedChannel, alphaMergedChannel, context, commandQueue, device, program);
		//float[] finalData = initial;
		
		float[][] cloneChannels = {redCloneChannel, greenCloneChannel, blueCloneChannel};
		float[][] sceneChannels = {redSceneChannel, greenSceneChannel, blueSceneChannel};
		float[][] mergedChannels = {redMergedChannel, greenMergedChannel, blueMergedChannel};
		
		float[] redResultChannel = new float[sceneData.length];
		float[] greenResultChannel = new float[sceneData.length];
		float[] blueResultChannel = new float[sceneData.length];
		float[][] resultChannels = {redResultChannel, greenResultChannel, blueResultChannel};
		improveClone(cloneDimensions, sceneChannels, cloneChannels, mask, mergedChannels, resultChannels, context, commandQueue, device, program);
		
		
		
		int[] resultData = new int[sceneData.length];
		//float[][] finalChannels = {resultChannels[0], resultChannels[1], resultChannels[2], alphaSceneChannel};
		float[][] finalChannels = {redMergedChannel, greenMergedChannel, blueMergedChannel, alphaSceneChannel};
		convertToInt(finalChannels, resultData, context, commandQueue, device, program);
		
		
		
		result = wrapUp(resultData, scene);
		
		CL.clReleaseProgram(program);
		double miliSeconds = TIME / 1000000.0;
		JOptionPane.showMessageDialog(null, "Time Taken: " + miliSeconds + " (ms)");
		
		return result;
	}
	
	/**
	 * Iteratively improves the clone.
	 * 
	 * @param dimensions The dimensions of the image.
	 * @param sceneChannels The RGB scene channels as floats.
	 * @param cloneChannels The RGB clone channels as floats.
	 * @param mask The mask data.
	 * @param mergedChannels The RGB merged channels as floats.
	 * @param resultChannels The RGB result channels as floats.
	 * @param context The OpenCL context.
	 * @param commandQueue The OpenCL command queue.
	 * @param device The OpenCL device.
	 * @param program The OpenCL program.
	 */
	private static void improveClone(int[] dimensions, float[][] sceneChannels, float[][] cloneChannels, int[] mask, float[][] mergedChannels, float[][] resultChannels, cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
		int globalSize = sceneChannels[0].length;
		int localSize = calculateLocalSize(globalSize, device);
		
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};
		
		float[] redSceneChannel = sceneChannels[0];
		float[] greenSceneChannel = sceneChannels[1];
		float[] blueSceneChannel = sceneChannels[2];
		
		float[] redCloneChannel = cloneChannels[0];
		float[] greenCloneChannel = cloneChannels[1];
		float[] blueCloneChannel = cloneChannels[2];
		
		float[] redMergedChannel = mergedChannels[0];
		float[] greenMergedChannel = mergedChannels[1];
		float[] blueMergedChannel = mergedChannels[2];
		
		float[] redResultChannel = resultChannels[0];
		float[] greenResultChannel = resultChannels[1];
		float[] blueResultChannel = resultChannels[2];
		
		Pointer ptrDimensions = Pointer.to(dimensions);
		
		Pointer ptrRedScene = Pointer.to(redSceneChannel);
		Pointer ptrGreenScene = Pointer.to(greenSceneChannel);
		Pointer ptrBlueScene = Pointer.to(blueSceneChannel);
		
		Pointer ptrRedClone = Pointer.to(redCloneChannel);
		Pointer ptrBlueClone = Pointer.to(blueCloneChannel);
		Pointer ptrGreenClone = Pointer.to(greenCloneChannel);
		
		Pointer ptrMask = Pointer.to(mask);
		
		Pointer ptrRedMerged = Pointer.to(redMergedChannel);
		Pointer ptrGreenMerged = Pointer.to(greenMergedChannel);
		Pointer ptrBlueMerged = Pointer.to(blueMergedChannel);
		
		Pointer ptrRedResult = Pointer.to(redResultChannel);
		Pointer ptrGreenResult = Pointer.to(greenResultChannel);
		Pointer ptrBlueResult = Pointer.to(blueResultChannel);
		
		cl_mem memDimensions = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * dimensions.length, ptrDimensions, null);
		
		cl_mem memRedScene =  CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * redSceneChannel.length, ptrRedScene, null);
		cl_mem memGreenScene =  CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * greenSceneChannel.length, ptrGreenScene, null);
		cl_mem memBlueScene =  CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * blueSceneChannel.length, ptrBlueScene, null);
		
		cl_mem memRedClone = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * redCloneChannel.length, ptrRedClone, null);
		cl_mem memGreenClone = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * greenCloneChannel.length, ptrGreenClone, null);
		cl_mem memBlueClone = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * blueCloneChannel.length, ptrBlueClone, null);
		
		cl_mem memMask = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * mask.length, ptrMask, null);
		
		cl_mem memRedMerged = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * redMergedChannel.length, ptrRedMerged, null);
		cl_mem memGreenMerged = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * greenMergedChannel.length, ptrGreenMerged, null);
		cl_mem memBlueMerged = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * blueMergedChannel.length, ptrBlueMerged, null);
		
		cl_mem memRedResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * redResultChannel.length, ptrRedResult, null);
		cl_mem memGreenResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * greenResultChannel.length, ptrGreenResult, null);
		cl_mem memBlueResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * blueResultChannel.length, ptrBlueResult, null);
		
		
		cl_mem[] objects = {memDimensions, 
				memRedScene, memGreenScene, memBlueScene,
				memRedClone, memGreenClone, memBlueClone,
				memMask, 
				memRedMerged, memGreenMerged, memBlueMerged,
				memRedResult, memGreenResult, memBlueResult};
		cl_kernel kernel = CL.clCreateKernel(program, "improve_clone", null);
		setKernelArgs(objects, kernel);
		
		long startTime = System.nanoTime();
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
		long endTime = System.nanoTime();
		TIME += endTime - startTime;
		
	
		
		CL.clEnqueueReadBuffer(commandQueue, memRedResult, 
				CL.CL_TRUE, 0, redResultChannel.length * Sizeof.cl_float,
				ptrRedResult, 0, null, null);
		CL.clEnqueueReadBuffer(commandQueue, memGreenResult, 
				CL.CL_TRUE, 0, greenResultChannel.length * Sizeof.cl_float,
				ptrGreenResult, 0, null, null);
		CL.clEnqueueReadBuffer(commandQueue, memRedResult, 
				CL.CL_TRUE, 0, blueResultChannel.length * Sizeof.cl_float,
				ptrBlueResult, 0, null, null);
		
		
		releaseMemObject(objects);
	}
	
	/**
	 * Makes the initial guess.
	 * 
	 * @param sceneData The raster from the scene image.
	 * @param cloneData The raster from the clone image.
	 * @param maskData The mask data.
	 * @param result The resulting raster.
	 * @param context The OpenCL context.
	 * @param commandQueue The OpenCL commandQueue.
	 * @param device The OpenCL device.
	 * @param program The OpenCL program.
	 */
	private static void initialGuess(int[] sceneData, int[] cloneData, int[] maskData, int[] result, cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
		int globalSize = sceneData.length;
		int localSize = calculateLocalSize(globalSize, device);
		
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};
		
		Pointer ptrScene = Pointer.to(sceneData);
		Pointer ptrClone = Pointer.to(cloneData);
		Pointer ptrMask = Pointer.to(maskData);
		Pointer ptrResult = Pointer.to(result);
		
		cl_mem memScene =  CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * sceneData.length, ptrScene, null);
		cl_mem memClone = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * cloneData.length, ptrClone, null);
		cl_mem memMask = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * maskData.length, ptrMask, null);
		cl_mem memResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * result.length, ptrResult, null);
		
		cl_mem[] objects = {memScene, memClone, memMask, memResult};
		cl_kernel kernel = CL.clCreateKernel(program, "intial_guess", null);
		setKernelArgs(objects, kernel);
		
		long startTime = System.nanoTime();
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
		long endTime = System.nanoTime();
		TIME += endTime - startTime;
		
	
		
		CL.clEnqueueReadBuffer(commandQueue, memResult, 
				CL.CL_TRUE, 0, result.length * Sizeof.cl_float,
				ptrResult, 0, null, null);
		
		
		releaseMemObject(objects);
	}
	
	
	/**
	 * Creates the mask for the clone image.
	 * 
	 * @param dimensions The dimensions of the clone image.
	 * @param alphaChannel The alpha channel.
	 * @param mask The generated mask.
	 * @param context The OpenCL context.
	 * @param commandQueue The OpenCL commandqueue.
	 * @param device The OpenCL device.
	 * @param program The OpenCL program.
	 */
	private static void findMaskValues(int[] dimensions, float[] alphaChannel, int[] mask, cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
		int globalSize = alphaChannel.length;
		int localSize = calculateLocalSize(globalSize, device);
		
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};
		
		
		Pointer ptrDimensions = Pointer.to(dimensions);
		Pointer ptrAlpha = Pointer.to(alphaChannel);
		Pointer ptrMask = Pointer.to(mask);
		
		cl_mem memDimensions = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * dimensions.length, ptrDimensions, null);
		cl_mem memAlpha = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * alphaChannel.length, ptrAlpha, null);
		cl_mem memMask = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * mask.length, ptrMask, null);
		
		cl_mem[] objects = {memDimensions, memAlpha, memMask};
		
		cl_kernel kernel = CL.clCreateKernel(program, "generate_mask", null);
		setKernelArgs(objects, kernel);
		
		long startTime = System.nanoTime();
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
		long endTime = System.nanoTime();
		TIME += endTime - startTime;
		
	
		
		CL.clEnqueueReadBuffer(commandQueue, memMask, 
				CL.CL_TRUE, 0, mask.length * Sizeof.cl_float,
				ptrMask, 0, null, null);
		
		
		releaseMemObject(objects);
	}
	
	
	/**
	 * Extracts the color channels from the clone image and makes them into floats.
	 * @param data The clone image raster.
	 * @param redChannel The red channel.
	 * @param blueChannel The blue channel.
	 * @param greenChannel The green channel.
	 * @param alphaChannel The alpha channel.
	 * @param context The openCL context.
	 * @param commandQueue The openCL command queue.
	 * @param device The openCL device.
	 * @param program The openCL program.
	 */
	private static void convertChannelsToFloats(int[] data, float[] redChannel, float[] blueChannel, float[] greenChannel, float[] alphaChannel,
			cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
	
		int globalSize = data.length;
		int localSize = calculateLocalSize(globalSize, device);
		
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};
		
		Pointer ptrData = Pointer.to(data);
		Pointer ptrRedClone = Pointer.to(redChannel);
		Pointer ptrGreenClone = Pointer.to(greenChannel);
		Pointer ptrBlueClone = Pointer.to(blueChannel);
		Pointer ptrAlphaClone = Pointer.to(alphaChannel);
		
		cl_mem memData = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * data.length, ptrData, null);
		cl_mem memRedClone = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * redChannel.length, ptrRedClone, null);
		cl_mem memGreenClone = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * greenChannel.length, ptrGreenClone, null);
		cl_mem memBlueClone = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * blueChannel.length, ptrBlueClone, null);
		cl_mem memAlphaClone = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * alphaChannel.length, ptrAlphaClone, null);
		
		
		cl_mem[] objects = {memData, memRedClone, memBlueClone, memGreenClone, memAlphaClone};
		
		cl_kernel kernel = CL.clCreateKernel(program, "seperate_channels", null);
		setKernelArgs(objects, kernel);
		
		long startTime = System.nanoTime();
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
		long endTime = System.nanoTime();
		TIME += endTime - startTime;
		
		CL.clEnqueueReadBuffer(commandQueue, memRedClone, 
				CL.CL_TRUE, 0, redChannel.length * Sizeof.cl_float,
				ptrRedClone, 0, null, null);
		
		CL.clEnqueueReadBuffer(commandQueue, memBlueClone, 
				CL.CL_TRUE, 0, blueChannel.length * Sizeof.cl_float,
				ptrBlueClone, 0, null, null);
		
		CL.clEnqueueReadBuffer(commandQueue, memGreenClone, 
				CL.CL_TRUE, 0, greenChannel.length * Sizeof.cl_float,
				ptrGreenClone, 0, null, null);
		
		CL.clEnqueueReadBuffer(commandQueue, memAlphaClone, 
				CL.CL_TRUE, 0, alphaChannel.length * Sizeof.cl_float,
				ptrAlphaClone, 0, null, null);
		
		releaseMemObject(objects);
		
	}
	
	/**
	 * Converts the final result back to be integer values.
	 * @param finalChannels The channels to be converted back to ints and combined into the final image.
	 * @param result The data as ints.
	 * @param context The OpenCL context.
	 * @param commandQueue The OpenCL command queue.
	 * @param device The OpenCL device.
	 * @param program The OpenCL program.
	 */
	private static void convertToInt(float[][] finalChannels, int[] result, cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
		int globalSize = finalChannels[0].length;
		int localSize = calculateLocalSize(globalSize, device);
		
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};
		
		
		float[] redChannel = finalChannels[0];
		float[] greenChannel = finalChannels[1];
		float[] blueChannel = finalChannels[2];
		float[] alphaChannel = finalChannels[3];
		
		Pointer ptrRed = Pointer.to(redChannel);
		Pointer ptrGreen = Pointer.to(greenChannel);
		Pointer ptrBlue = Pointer.to(blueChannel);
		Pointer ptrAlpha = Pointer.to(alphaChannel);
		
		Pointer ptrResult = Pointer.to(result);
		
		cl_mem memRed = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * redChannel.length, ptrRed, null);
		cl_mem memGreen = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * greenChannel.length, ptrGreen, null);
		cl_mem memBlue = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * blueChannel.length, ptrBlue, null);
		cl_mem memAlpha = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * alphaChannel.length, ptrAlpha, null);
		
		cl_mem memResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * result.length, ptrResult, null);
		
		cl_mem[] objects = {memRed, memGreen, memBlue, memAlpha, memResult};
		
		cl_kernel kernel = CL.clCreateKernel(program, "int_cast", null);
		setKernelArgs(objects, kernel);
		
		long startTime = System.nanoTime();
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
		long endTime = System.nanoTime();
		TIME += endTime - startTime;
		
		CL.clEnqueueReadBuffer(commandQueue, memResult, 
				CL.CL_TRUE, 0, result.length * Sizeof.cl_float,
				ptrResult, 0, null, null);
		
		releaseMemObject(objects);
	}
}
