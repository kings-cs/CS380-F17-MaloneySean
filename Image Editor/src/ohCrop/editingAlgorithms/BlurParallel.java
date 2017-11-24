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
		
		Pointer ptrRaster = Pointer.to(imageRaster);
		Pointer ptrResult = Pointer.to(resultData);
		
		Pointer ptrRed = Pointer.to(redArray);
		Pointer ptrGreen = Pointer.to(greenArray);
		Pointer ptrBlue = Pointer.to(blueArray);
		Pointer ptrAlpha = Pointer.to(alphaArray);
		
		Pointer ptrRedAvg = Pointer.to(redAvg);
		Pointer ptrGreenAvg = Pointer.to(greenAvg);
		Pointer ptrBlueAvg = Pointer.to(blueAvg);
		
		Pointer ptrDimension = Pointer.to(dimension);
		
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
		
		cl_mem memDimension = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * dimension.length, ptrDimension, null);
		
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
		
		long timeTaken = endTime - startTime;
		
		double miliSeconds = timeTaken / 1000000.0;
		JOptionPane.showMessageDialog(null, "Time Taken: " + miliSeconds + " (ms)");
		
		
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
		
		cl_mem[] allObjects = {memRaster, memResult, memRed, memGreen, memBlue, memAlpha,
				memRedAvg, memGreenAvg, memBlueAvg, memDimension};

		releaseMemObject(allObjects);
		
		return result;
	}
}
