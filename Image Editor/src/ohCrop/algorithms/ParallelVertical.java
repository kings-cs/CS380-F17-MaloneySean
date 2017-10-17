package ohCrop.algorithms;

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

/**
 * Class used to flip an image vertically.
 * @author Sean Maloney
 *
 */
public class ParallelVertical extends ImageAlgorithm{
	/**
	 * Flips an image vertically.
	 * 
	 * @param context The OpenCL context used for the parallel computing.
	 * @param commandQueue The OpenCL commandQueue used for the parallel computing.
	 * @param original The image to be colored.
	 * @param device The device used by OpenCL.
	 * 
	 * @return The vertically flipped image.
	 */
	public static BufferedImage verticalFlip(cl_context context, cl_command_queue commandQueue, cl_device_id device, BufferedImage original) {
		
		
		
		int[] imageRaster = strip(original);
		int[] resultData = new int[imageRaster.length];
		int[] dimensions = {original.getWidth(), original.getHeight()};
		
		Pointer ptrRaster = Pointer.to(imageRaster);
		Pointer ptrResult = Pointer.to(resultData);
		Pointer ptrDimensions = Pointer.to(dimensions);
		
		cl_mem memRaster = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * imageRaster.length, ptrRaster, null);
		cl_mem memResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * resultData.length, ptrResult, null);
		cl_mem memDimensions = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * dimensions.length, ptrDimensions, null);
		
		//KERNEL EXECUTION, SHOULD PROBABLY SPLIT THESE UP
		
		//Create the program from the source code
		//Create the OpenCL kernel from the program
		String source = KernelReader.readFile("Kernels/Vertical_Kernel");
		
		//System.out.println(source);
		
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] {source}, null, null);
		
		
		//Build the program
		CL.clBuildProgram(program, 0, null, null, null, null);
		
		//Create the kernel
		cl_kernel kernel = CL.clCreateKernel(program, "vertical_kernel", null);
		
		//Set the arguments for the kernel
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memRaster));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memResult));
		CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memDimensions));
	
		//WORK GROUP STUFF		
		

		long[] size = new long[1];
		CL.clGetDeviceInfo(device, CL.CL_DEVICE_MAX_WORK_GROUP_SIZE, 0, 
				null, size);

		int[] sizeBuffer = new int[(int) size[0]];
		CL.clGetDeviceInfo(device, CL.CL_DEVICE_MAX_WORK_GROUP_SIZE, 
				sizeBuffer.length, Pointer.to(sizeBuffer), null);



		int maxGroupSize = sizeBuffer[0];
		int globalSize = imageRaster.length;
		int localSize = maxGroupSize;

		boolean divisible = false;

		while(!divisible) {
			int mod = globalSize % localSize;
			if(mod == 0) {
				divisible = true;
			}
			else {
				localSize--;
			}
		}

		
		//Set the work-item dimensions
		long[] globalWorkSize = new long[] {imageRaster.length};
		long[] localWorkSize = new long[] {localSize};
		
		
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
		CL.clReleaseMemObject(memDimensions);
		
		
		return result;
	}

}
