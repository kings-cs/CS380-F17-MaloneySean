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
 * Class used to rotate an image 90 degrees to the left.
 * @author Sean Maloney
 *
 */
public class ParallelLeftRotation extends ImageAlgorithm{
	/**
	 * Rotates an image 90 degrees to the left.
	 * 
	 * @param context The OpenCL context used for the parallel computing.
	 * @param commandQueue The OpenCL commandQueue used for the parallel computing.
	 * @param original The image to be colored.
	 * 
	 * @return The rotated image.
	 */
	public static BufferedImage rotateLeft(cl_context context, cl_command_queue commandQueue, BufferedImage original) {
		
		
		
		int[] imageRaster = strip(original);
		int[] resultData = new int[imageRaster.length];
		
		Pointer ptrRaster = Pointer.to(imageRaster);
		Pointer ptrResult = Pointer.to(resultData);
		
		cl_mem memRaster = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * imageRaster.length, ptrRaster, null);
		cl_mem memResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * resultData.length, ptrResult, null);
		
		//KERNEL EXECUTION, SHOULD PROBABLY SPLIT THESE UP
		
		//Create the program from the source code
		//Create the OpenCL kernel from the program
		String source = KernelReader.readFile("Kernels/Left_Kernel");
		
		//System.out.println(source);
		
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] {source}, null, null);
		
		
		//Build the program
		CL.clBuildProgram(program, 0, null, null, null, null);
		
		//Create the kernel
		cl_kernel kernel = CL.clCreateKernel(program, "left_Kernel", null);
		
		//Set the arguments for the kernel
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memRaster));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memResult));
	
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
		
		
		return result;
	}


}
