package ohCrop.algorithms;

import java.awt.image.BufferedImage;

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
 * Control class used to handle the Blur algorithm in parallel.
 * 
 * @author Sean Maloney
 *
 */
public class BlurParallel extends ImageAlgorithm{
	/**
	 * Converts the individual pixels of an image to be blurred, calculated in parallel.
	 * 
	 * @param context The OpenCL context used for the parallel computing.
	 * @param commandQueue The OpenCL commandQueue used for the parallel computing.
	 * @param original The image to be colored.
	 * 
	 * @return The newly blurred image.
	 */
	public static BufferedImage parallelBlur(cl_context context, cl_command_queue commandQueue, BufferedImage original) {
		
		
		
		int[] imageRaster = strip(original);
		int[] resultData = new int[imageRaster.length];
		
		int[] redArray = new int[imageRaster.length];
		int[] greenArray = new int[imageRaster.length];
		int[] blueArray = new int[imageRaster.length];
		int[] alphaArray = new int[imageRaster.length];
		
		float[] redAvg = new float[imageRaster.length];
		float[] greenAvg = new float[imageRaster.length];
		float[] blueAvg = new float[imageRaster.length];
		
		int[] width = {original.getWidth()};
		
		Pointer ptrRaster = Pointer.to(imageRaster);
		Pointer ptrResult = Pointer.to(resultData);
		
		Pointer ptrRed = Pointer.to(redArray);
		Pointer ptrGreen = Pointer.to(greenArray);
		Pointer ptrBlue = Pointer.to(blueArray);
		Pointer ptrAlpha = Pointer.to(alphaArray);
		
		Pointer ptrRedAvg = Pointer.to(redAvg);
		Pointer ptrGreenAvg = Pointer.to(greenAvg);
		Pointer ptrBlueAvg = Pointer.to(blueAvg);
		
		Pointer ptrWidth = Pointer.to(width);
		
		cl_mem memRaster = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * imageRaster.length, ptrRaster, null);
		cl_mem memResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * resultData.length, ptrResult, null);
		
		
		cl_mem memRed = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * redArray.length, ptrRed, null);
		cl_mem memGreen = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * greenArray.length, ptrGreen, null);
		cl_mem memBlue = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * blueArray.length, ptrBlue, null);
		cl_mem memAlpha = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * alphaArray.length, ptrAlpha, null);
		
		cl_mem memRedAvg = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * redAvg.length, ptrRedAvg, null);
		cl_mem memGreenAvg = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * greenAvg.length, ptrGreenAvg, null);
		cl_mem memBlueAvg = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * blueAvg.length, ptrBlueAvg, null);
		
		cl_mem memWidth = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * width.length, ptrWidth, null);
		
		//KERNEL EXECUTION, SHOULD PROBABLY SPLIT THESE UP
		
		//Create the program from the source code
		//Create the OpenCL kernel from the program
		String seperateSource = KernelReader.readFile("Kernels/SeperateChannel_Kernel");
		String stencilSource = KernelReader.readFile("Kernels/StencilChannels_Kernel");
		//TODO: String ... String ...
		
		
		//System.out.println(source);
		
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] {seperateSource}, null, null);
		cl_program stencilProgram = CL.clCreateProgramWithSource(context, 1, new String[] {stencilSource}, null, null);
		
		//Build the program
		CL.clBuildProgram(program, 0, null, null, null, null);
		CL.clBuildProgram(stencilProgram, 0, null, null, null, null);
		//Create the kernel
		cl_kernel seperateKernel = CL.clCreateKernel(program, "seperateChannel_Kernel", null);
		cl_kernel stencilKernel = CL.clCreateKernel(program, "stencilChannel_Kernel", null);
		
		//Set the arguments for the kernel
		//TODO: Doesnt need memResult
		CL.clSetKernelArg(seperateKernel, 0, Sizeof.cl_mem, Pointer.to(memRaster));
		CL.clSetKernelArg(seperateKernel, 1, Sizeof.cl_mem, Pointer.to(memRed));
		CL.clSetKernelArg(seperateKernel, 2, Sizeof.cl_mem, Pointer.to(memGreen));
		CL.clSetKernelArg(seperateKernel, 3, Sizeof.cl_mem, Pointer.to(memBlue));
		CL.clSetKernelArg(seperateKernel, 4, Sizeof.cl_mem, Pointer.to(memAlpha));
		
		CL.clSetKernelArg(stencilKernel, 0, Sizeof.cl_mem, Pointer.to(memRaster));
		CL.clSetKernelArg(stencilKernel, 1, Sizeof.cl_mem, Pointer.to(memRed));
		CL.clSetKernelArg(stencilKernel, 2, Sizeof.cl_mem, Pointer.to(memGreen));
		CL.clSetKernelArg(stencilKernel, 3, Sizeof.cl_mem, Pointer.to(memBlue));
		CL.clSetKernelArg(stencilKernel, 4, Sizeof.cl_mem, Pointer.to(memRedAvg));
		CL.clSetKernelArg(stencilKernel, 5, Sizeof.cl_mem, Pointer.to(memGreenAvg));
		CL.clSetKernelArg(stencilKernel, 6, Sizeof.cl_mem, Pointer.to(memBlueAvg));
		CL.clSetKernelArg(stencilKernel, 7, Sizeof.cl_mem, Pointer.to(memWidth));
		
		//Set the work-item dimensions
		long[] globalWorkSize = new long[] {resultData.length};
		long[] localWorkSize = new long[] {1};
		
		
		long startTime = System.nanoTime();
		//Execute the kernel
		CL.clEnqueueNDRangeKernel(commandQueue, seperateKernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
		CL.clEnqueueNDRangeKernel(commandQueue, stencilKernel, 1, null,
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
		CL.clReleaseKernel(seperateKernel);
		CL.clReleaseProgram(program);
		CL.clReleaseMemObject(memRaster);
		CL.clReleaseMemObject(memResult);
		
		
		return result;
	}
}
