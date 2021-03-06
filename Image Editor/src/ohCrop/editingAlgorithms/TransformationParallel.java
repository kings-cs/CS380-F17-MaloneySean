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
 * Class used to flip an image according to the given kernel.
 * @author Sean Maloney
 *
 */
public class TransformationParallel extends ParallelAlgorithm{
	/**
	 * Flips an image according to the given kernel.
	 * 
	 * @param context The OpenCL context used for the parallel computing.
	 * @param commandQueue The OpenCL commandQueue used for the parallel computing.
	 * @param original The image to be colored.
	 * @param device The device used by OpenCl.
	 * @param kernelMethod The name of the kernel method to be run.
	 * 
	 * @return The transformed image.
	 */
	public static BufferedImage horizontalFlip(cl_context context, cl_command_queue commandQueue, cl_device_id device, BufferedImage original, String kernelMethod) {
		
		int[] imageRaster = strip(original);
		int[] resultData = new int[imageRaster.length];
		int[] dimensions = {original.getWidth(), original.getHeight()};
		
		int[][] params = {imageRaster, resultData, dimensions};
		
		Pointer[] pointers = createPointers(params);
		Pointer ptrResult = pointers[1];
		
		cl_mem[] objects = createMemObjects(params, pointers, context) ;
		cl_mem memResult = objects[1];

		
		//KERNEL EXECUTION, SHOULD PROBABLY SPLIT THESE UP
		
		//Create the program from the source code
		//Create the OpenCL kernel from the program
		String source = KernelReader.readFile("Kernels/Transformations_Kernel");
		
		//System.out.println(source);
		
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] {source}, null, null);
		
		
		//Build the program
		CL.clBuildProgram(program, 0, null, null, null, null);
		
		//Create the kernel
		cl_kernel kernel = CL.clCreateKernel(program, kernelMethod, null);
		
		//Set the arguments for the kernel
		setKernelArgs(objects, kernel);
	

		//Set the work-item dimensions
		int globalSize = imageRaster.length;
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {calculateLocalSize(globalSize, device)};

		
		long startTime = System.nanoTime();
		//Execute the kernel
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
	
		long endTime = System.nanoTime();
		
		displayTimeTaken(startTime, endTime);
		
		
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memResult, 
				CL.CL_TRUE, 0, resultData.length * Sizeof.cl_float,
				ptrResult, 0, null, null);
		
		
		BufferedImage result = wrapUp(resultData, original);
		
				
		//Release kernel, program, 
		CL.clReleaseKernel(kernel);
		CL.clReleaseProgram(program);
		releaseMemObject(objects);
		
		
		return result;
	}
}
