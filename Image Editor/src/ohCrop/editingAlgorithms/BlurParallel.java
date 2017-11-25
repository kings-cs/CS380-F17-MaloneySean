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

import ohCrop.utilAlgorithms.KernelReader;
import ohCrop.utilAlgorithms.ParallelAlgorithm;

/**
 * Control class used to handle the Blur algorithm in parallel.
 * 
 * @author Sean Maloney
 *
 */
public class BlurParallel extends ParallelAlgorithm{
	/**
	 * Converts the individual pixels of an image to be blurred, calculated in parallel.
	 * 
	 * @param context The OpenCL context used for the parallel computing.
	 * @param commandQueue The OpenCL commandQueue used for the parallel computing.
	 * @param device The OpenCL device used for the parallel computing.
	 * @param original The image to be colored.
	 * 
	 * @return The newly blurred image.
	 */
	public static BufferedImage parallelBlur(cl_context context, cl_command_queue commandQueue, cl_device_id device, BufferedImage original) {
		
		
		
		int[] imageRaster = strip(original);
		int[] resultData = new int[imageRaster.length];
		
		int[] redArray = new int[imageRaster.length];
		int[] greenArray = new int[imageRaster.length];
		int[] blueArray = new int[imageRaster.length];
		int[] alphaArray = new int[imageRaster.length];
		
		int[] redAvg = new int[imageRaster.length];
		int[] greenAvg = new int[imageRaster.length];
		int[] blueAvg = new int[imageRaster.length];
		
		int[] dimension = {original.getWidth(), original.getHeight()};
		
		int[][] params = {imageRaster, resultData, redArray, greenArray, blueArray,
				alphaArray, redAvg, greenAvg, blueAvg, dimension};
		
		
		Pointer[] pointers = createPointers(params);
		
		Pointer ptrResult = pointers[1];
		
		Pointer ptrRed = pointers[2];
		Pointer ptrGreen = pointers[3];
		Pointer ptrBlue = pointers[4];
		Pointer ptrAlpha = pointers[5];
		
		Pointer ptrRedAvg = pointers[6];
		Pointer ptrGreenAvg = pointers[7];
		Pointer ptrBlueAvg = pointers[8];
	
		

		cl_mem[] objects = createMemObjects(params, pointers, context);
		
		//KERNEL EXECUTION, SHOULD PROBABLY SPLIT THESE UP
		
		//Create the program from the source code
		//Create the OpenCL kernel from the program
		String source = KernelReader.readFile("Kernels/Blur_Kernel");
		
		
	
		
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] {source}, null, null);
		
		//Build the program
		CL.clBuildProgram(program, 0, null, null, null, null);
		
		//Create the kernel
		cl_kernel seperateKernel = CL.clCreateKernel(program, "seperateChannel_Kernel", null);
		cl_kernel stencilKernel = CL.clCreateKernel(program, "stencilChannel_Kernel", null);
		cl_kernel recombineKernel = CL.clCreateKernel(program, "recombineChannel_Kernel", null);
		

		cl_mem memRaster = objects[0];
		cl_mem memResult = objects[1];
		cl_mem memRed = objects[2];
		cl_mem memGreen = objects[3];
		cl_mem memBlue = objects[4];
		cl_mem memAlpha = objects[5];
		cl_mem memRedAvg = objects[6];
		cl_mem memGreenAvg = objects[7];
		cl_mem memBlueAvg = objects[8];
		cl_mem memDimension = objects[9];
		
		cl_mem[] seperateObjects = {memRaster, memRed, memGreen, memBlue, memAlpha};
		setKernelArgs(seperateObjects, seperateKernel);
	
		cl_mem[] stencilObjects = {memRaster, memRed, memGreen, memBlue, memRedAvg,
				memGreenAvg, memBlueAvg, memDimension};
		setKernelArgs(stencilObjects, stencilKernel);
		

		cl_mem[] recombineObjects = {memResult, memAlpha, memRedAvg, memGreenAvg, memBlueAvg};
		setKernelArgs(recombineObjects, recombineKernel);


//WORK GROUP STUFF		
		


		int globalSize = imageRaster.length;
		int localSize =  calculateLocalSize(globalSize, device);

		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};
		
		

		
		
		
//END WORK GROUP STUFF	
		
		
		long startTime = System.nanoTime();
		//Execute the First kernel, outputs red, green, blue, alpha
		CL.clEnqueueNDRangeKernel(commandQueue, seperateKernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
		
		CL.clEnqueueReadBuffer(commandQueue, memRed, 
				CL.CL_TRUE, 0, redArray.length * Sizeof.cl_int,
				ptrRed, 0, null, null);
		CL.clEnqueueReadBuffer(commandQueue, memGreen, 
				CL.CL_TRUE, 0, redArray.length * Sizeof.cl_int,
				ptrGreen, 0, null, null);
		CL.clEnqueueReadBuffer(commandQueue, memBlue, 
				CL.CL_TRUE, 0, redArray.length * Sizeof.cl_int,
				ptrBlue, 0, null, null);
		CL.clEnqueueReadBuffer(commandQueue, memAlpha, 
				CL.CL_TRUE, 0, alphaArray.length * Sizeof.cl_int,
				ptrAlpha, 0, null, null);
		
		
		//Execute the second kernel, outputs redAvg, greenAvg, blueAvg.
		CL.clEnqueueNDRangeKernel(commandQueue, stencilKernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
		CL.clEnqueueReadBuffer(commandQueue, memRedAvg, 
				CL.CL_TRUE, 0, redAvg.length * Sizeof.cl_int,
				ptrRedAvg, 0, null, null);
		CL.clEnqueueReadBuffer(commandQueue, memGreenAvg, 
				CL.CL_TRUE, 0, greenAvg.length * Sizeof.cl_int,
				ptrGreenAvg, 0, null, null);
		CL.clEnqueueReadBuffer(commandQueue, memBlueAvg, 
				CL.CL_TRUE, 0, blueAvg.length * Sizeof.cl_int,
				ptrBlueAvg, 0, null, null);
	
		
		
		
		//Execute the third kernel, outputs result 
		CL.clEnqueueNDRangeKernel(commandQueue, recombineKernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
			
		
		
		long endTime = System.nanoTime();
		
		displayTimeTaken(startTime, endTime);
		
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memResult, 
				CL.CL_TRUE, 0, resultData.length * Sizeof.cl_int,
				ptrResult, 0, null, null);
		
		

		
		BufferedImage result = wrapUp(resultData, original);
		
				
		//Release kernel, program, 
		CL.clReleaseKernel(seperateKernel);
		CL.clReleaseKernel(stencilKernel);
		CL.clReleaseKernel(recombineKernel);
		CL.clReleaseProgram(program);
	

		releaseMemObject(objects);
		
		return result;
	}
}
