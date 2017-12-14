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
	 * @return The combined image.
	 */
	public static BufferedImage seamlessClone(cl_context context, cl_command_queue commandQueue, cl_device_id device, BufferedImage scene, BufferedImage clone) {
		TIME = 0;
		
		BufferedImage result = null;
		
		cl_program program = buildProgram("Kernels/Clone_Kernel", context);
		
		int[] cloneData = strip(clone);
		float[] redCloneChannel = new float[cloneData.length];
		float[] greenCloneChannel = new float[cloneData.length];
		float[] blueCloneChannel = new float[cloneData.length];
		float[] alphaCloneChannel = new float[cloneData.length];
		
		convertChannelsToFloats(cloneData, redCloneChannel, blueCloneChannel, greenCloneChannel, alphaCloneChannel, context, commandQueue, device, program);
		
		
		float[] mask = new float[cloneData.length];
		findMaskValues(alphaCloneChannel, mask, context, commandQueue, device, program);
		
		return result;
	}
	
	
	/**
	 * Creates the mask for the clone image.
	 * @param alphaChannel The alpha channel.
	 * @param mask The generated mask.
	 * @param context The OpenCL context.
	 * @param commandQueue The OpenCL commandqueue.
	 * @param device The OpenCL device.
	 * @param program The OpenCL program.
	 */
	private static void findMaskValues(float[] alphaChannel, float[] mask, cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
		int globalSize = alphaChannel.length;
		int localSize = calculateLocalSize(globalSize, device);
		
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};
		
		Pointer ptrAlpha = Pointer.to(alphaChannel);
		Pointer ptrMask = Pointer.to(mask);
		
		cl_mem memAlpha = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * alphaChannel.length, ptrAlpha, null);
		cl_mem memMask = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * mask.length, ptrMask, null);
		
		cl_mem[] objects = {memAlpha, memMask};
		
		cl_kernel kernel = CL.clCreateKernel(program, "seperate_channels", null);
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
}
