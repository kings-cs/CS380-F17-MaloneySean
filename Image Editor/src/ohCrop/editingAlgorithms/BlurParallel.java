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
		
		
		
		
		//System.out.println(source);
		
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] {source}, null, null);
		
		//Build the program
		CL.clBuildProgram(program, 0, null, null, null, null);
		
		//Create the kernel
		cl_kernel seperateKernel = CL.clCreateKernel(program, "seperateChannel_Kernel", null);
		cl_kernel stencilKernel = CL.clCreateKernel(program, "stencilChannel_Kernel", null);
		cl_kernel recombineKernel = CL.clCreateKernel(program, "recombineChannel_Kernel", null);
		

		
//		long[] kernelSize = new long[1];
//		 CL.clGetKernelWorkGroupInfo (seperateKernel, device, CL.CL_KERNEL_WORK_GROUP_SIZE, 0,
//				  	null, kernelSize);
//		 
//		 int[] kernelBuffer = new int[(int) kernelSize[0]];
//		 CL.clGetKernelWorkGroupInfo (seperateKernel, device, CL.CL_KERNEL_WORK_GROUP_SIZE, kernelBuffer.length,
//				  	Pointer.to(kernelBuffer), null);
//		 
//		 System.out.println(kernelBuffer[0]);
		
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
		CL.clSetKernelArg(stencilKernel, 7, Sizeof.cl_mem, Pointer.to(memDimension));
		

		CL.clSetKernelArg(recombineKernel, 0, Sizeof.cl_mem, Pointer.to(memResult));
		CL.clSetKernelArg(recombineKernel, 1, Sizeof.cl_mem, Pointer.to(memAlpha));
		CL.clSetKernelArg(recombineKernel, 2, Sizeof.cl_mem, Pointer.to(memRedAvg));
		CL.clSetKernelArg(recombineKernel, 3, Sizeof.cl_mem, Pointer.to(memGreenAvg));
		CL.clSetKernelArg(recombineKernel, 4, Sizeof.cl_mem, Pointer.to(memBlueAvg));


		//Set the work-item dimensions
//				long[] globalWorkSize = new long[] {resultData.length};
//				long[] localWorkSize = new long[] {1};
		 //Uncomment this and comment out work group stuff below to undo 		



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
		
		
//		System.out.println(globalGroupSize % localGroupSize);
//		System.out.println(1500 - globalGroupSize);
//		globalGroupSize = 1500;
		
//		int groups = globalSize / localSize;
		
//		divisible = false;
//		while(!divisible) {
//			if(groups % localSize == 0) {
//				divisible = true;
//			}
//			else {
//				groups++;
//			}
//		}
//		
		long[] globalWorkSize = new long[] {imageRaster.length};
		long[] localWorkSize = new long[] {localSize};
		
		
		//long[] globalWorkSize = new long[] {imageRaster.length};
		
//		long[] globalWorkSize = new long[groups];
//		
//		for(int i = 0; i < globalWorkSize.length; i++) {
//			globalWorkSize[i] = localSize;
//		}
		
		//long[] localWorkSize = new long[] {localSize};
		
		
//		System.out.println("MAX  : " + maxGroupSize);
//		System.out.println("IMAGE: " + imageRaster.length);
//		System.out.println("LOCAL: " + localSize);
//		System.out.println("GROUP: " + groups);
//		
		
		
		
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
		
		
		//They are in fact being set to 0, work groups probably not executing
//		for(int i = 0; i < resultData.length; i++) {
//			System.out.println(resultData[i]);
//		}
		
		BufferedImage result = wrapUp(resultData, original);
		
				
		//Release kernel, program, 
		CL.clReleaseKernel(seperateKernel);
		CL.clReleaseKernel(stencilKernel);
		CL.clReleaseKernel(recombineKernel);
		CL.clReleaseProgram(program);
		CL.clReleaseMemObject(memRaster);
		CL.clReleaseMemObject(memResult);
		CL.clReleaseMemObject(memRed);
		CL.clReleaseMemObject(memGreen);
		CL.clReleaseMemObject(memBlue);
		CL.clReleaseMemObject(memAlpha);
		CL.clReleaseMemObject(memRedAvg);
		CL.clReleaseMemObject(memGreenAvg);
		CL.clReleaseMemObject(memBlueAvg);
		CL.clReleaseMemObject(memDimension);
		
		return result;
	}
}
