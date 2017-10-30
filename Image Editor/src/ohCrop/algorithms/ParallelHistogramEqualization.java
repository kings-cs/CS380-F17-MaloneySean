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
 * Computes a Histogram Equalizaion in Parallel.
 * @author Sean Maloney
 */
public class ParallelHistogramEqualization extends ImageAlgorithm{
	
	/**
	 * Field to keep trakc of the total time taken for all kernels to execute.
	 */
	private static long TOTAL_TIME;
	
	/**
	 * Converts the individual pixels of an image t be in shades of gray computed using parallelism.
	 * 
	 * @param context The OpenCL context used for the parallel computing.
	 * @param commandQueue The OpenCL commandQueue used for the parallel computing.
	 * @param original The image to be colored.
	 * @param device The device used by OpenCL.
	 * 
	 * @return The newly colored image.
	 */
	public static BufferedImage parallelHistogramEq(cl_context context, cl_command_queue commandQueue, cl_device_id device, BufferedImage original) {
		//Create the program from the source code
		//Create the OpenCL kernel from the program
		TOTAL_TIME = 0;
		String source = KernelReader.readFile("Kernels/Histogram_Equalization_Kernel");
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] {source}, null, null);
		
		
		
		int numBins = 256;
		
		 
		
		int[] imageRaster = strip(original);

		
				
		int[] dimensions = {numBins, imageRaster.length, original.getHeight(), original.getWidth()};
		
		
		
		
		Pointer ptrRaster = Pointer.to(imageRaster);
		Pointer ptrDimensions = Pointer.to(dimensions);

		
		cl_mem memRaster = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * imageRaster.length, ptrRaster, null);
		cl_mem memDimensions = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * dimensions.length, ptrDimensions, null);

		
		//STEP 1. CALCULATE HISTOGRAM (TESTED!!!)
		//TODO: This has to be tested in parallel. Why is parallel so much slower? Remember to uncomment the atomic add!!!
		//TODO: Idea to find time culprit. Change individual steps back to sequential after getting all working in parallel.
		 
		
		
		
		
		int[] histogram = new int[numBins];
		cl_mem memHistogram = parallelHelper(memRaster, memDimensions, histogram, imageRaster.length, "calculate_histogram", program, context, commandQueue, device);
		
		//TODO: This is doing some fuckity shit
		int[] localHistogramsCollection = new int[original.getHeight() * numBins];
		cl_mem memOptHistogram = parallelHelper(memRaster, memDimensions, localHistogramsCollection, imageRaster.length / original.getWidth(), "optimized_calculate_histogram", program, context, commandQueue, device);
		
		//STEP 2. CUMULATIVE FREQUENCY DISTRIBUTION (TESTED!!!)		
		float[] histoFloat = new float[histogram.length];
		for(int i = 0; i < histogram.length; i++) {
			histoFloat[i] = (float) histogram[i];
		}
		
		
		float[] distributionData = new float[histogram.length]; //OUTPUT
		
		ParallelScan.scan(histoFloat, distributionData, context, 
				commandQueue, device, "hillis_steele_scan");
		
		Pointer ptrDistribution = Pointer.to(distributionData);
		
		cl_mem memDistribution = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * distributionData.length, ptrDistribution, null); //MAKE MEM OBJECT SO THIS CAN BE USED AS PARAM LATER
		

				
		//STEP 3. IDEALIZED HISTOGRAM (TESTED!!!)	
		//int[] ideal = HistogramEquilization.idealizeHistogram(histogram, imageRaster.length);
		int[] ideal = new int[histogram.length];
		cl_mem memIdeal = parallelHelper(memHistogram, memDimensions, ideal, ideal.length, "idealize_histogram", program, context, commandQueue, device);
		
		
		//STEP 4. IDEAL CUMULATIVE FREQUNCY DISTRIBUTION (TESTED!!!)
		float[] idealFloat = new float[histogram.length];
		for(int i = 0; i < histogram.length; i++) {
			idealFloat[i] = ideal[i];
			
		}
		
		float[] idealCumData = new float[histogram.length];
		ParallelScan.scan(idealFloat, idealCumData, context, 
					commandQueue, device, "hillis_steele_scan");
		
		
		Pointer ptrIdealCum = Pointer.to(idealCumData);
		
		cl_mem memIdealCum = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * idealCumData.length, ptrIdealCum, null);
				
		
		//STEP 5. DESIGN MAPPING (TESTED!!!)
		int[] mapping = new int[distributionData.length];
		cl_mem memMapping = parallelHelper(memDistribution, memIdealCum, mapping, mapping.length, "map_histogram", program, context, commandQueue, device);
		
		
		//STEP 6. MAP PIXELS (TESTED!!!)		
		int[] resultData = new int[imageRaster.length];
	
		parallelHelper(memMapping, memRaster, resultData, resultData.length, "map_pixels", program, context, commandQueue, device);
		
		
		
		double miliSeconds = TOTAL_TIME / 1000000.0;
		JOptionPane.showMessageDialog(null, "Time Taken: " + miliSeconds + " (ms)");
		
		
		BufferedImage resultImage = wrapUp(resultData, original);
		
				
		//Release program and memory objects
		CL.clReleaseProgram(program);
		CL.clReleaseMemObject(memRaster);
		CL.clReleaseMemObject(memHistogram);
		CL.clReleaseMemObject(memDimensions);
		CL.clReleaseMemObject(memIdeal);
		CL.clReleaseMemObject(memIdealCum);
		CL.clReleaseMemObject(memMapping);
		
		return resultImage;
	}
	
	/**
	 * Private Helper to run a method in the kernel.
	 *  
	 * @param memInputData The input data to be used as the first parameter.
	 * @param memOtherInput The input data to be used as the second parameter.
	 * @param outputData The array for output to be written to.
	 * @param globalItemCount The amount of items to be calculated in this kenerl method.
	 * @param methodName The name of the method to be called.
	 * @param program The OpenCL program used.
	 * @param context The OpenCL context used.
	 * @param commandQueue The OpenCL command queue used.
	 * @param device The OpenCL device used.
	 * @return The memory object created during this methods execution, returned so that it can be later released and used as future parameters.
	 */
	private static cl_mem parallelHelper(cl_mem memInputData, cl_mem memOtherInput, int[] outputData, int globalItemCount, String methodName, 
			cl_program program, cl_context context, cl_command_queue commandQueue, cl_device_id device) {
		
		
	
		Pointer ptrOutput = Pointer.to(outputData);
		cl_mem memOutput = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * outputData.length, ptrOutput, null);
		
		
		//Build the program
		CL.clBuildProgram(program, 0, null, null, null, null);
		
		//Create the kernel
		cl_kernel kernel = CL.clCreateKernel(program, methodName, null);
		
		//Set the arguments for the kernel
		
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memInputData));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memOtherInput));
		CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memOutput));

	
		//WORK GROUP STUFF		
		
		int localSize = 0;
		if(methodName.equals("optimized_calculate_histogram")) {
			localSize = 1;
		}
		else {
			localSize = calculateLocalSize(globalItemCount, device);
		}
		
		//Set the work-item dimensions
		long[] globalWorkSize = new long[] {globalItemCount};
		long[] localWorkSize = new long[] {localSize};
		
		
		//Execute the kernel
		long startTime = System.nanoTime();
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);

		long endTime = System.nanoTime();
		
		long timeTaken = endTime - startTime;
		TOTAL_TIME += timeTaken;
		
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memOutput, 
				CL.CL_TRUE, 0, outputData.length * Sizeof.cl_float,
				ptrOutput, 0, null, null);
		
		
		
		CL.clReleaseKernel(kernel);
		
		return memOutput;
	}
	
	/**
	 * Private helper method used to calculate the best local size to be used.
	 * @param globalItemCount The global item count.
	 * @param device The open cl device to be used.
	 * @return The optimal local group size.
	 */
	private static int calculateLocalSize(int globalItemCount, cl_device_id device) {
		long[] size = new long[1];
		CL.clGetDeviceInfo(device, CL.CL_DEVICE_MAX_WORK_GROUP_SIZE, 0, 
				null, size);

		int[] sizeBuffer = new int[(int) size[0]];
		CL.clGetDeviceInfo(device, CL.CL_DEVICE_MAX_WORK_GROUP_SIZE, 
				sizeBuffer.length, Pointer.to(sizeBuffer), null);
		
		int maxGroupSize = sizeBuffer[0];
		int globalSize = globalItemCount;
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
		
		return localSize;
	}
	
//	public static void main(String[] args) {
//		ParallelSetUp setup = new ParallelSetUp();
//		CL.setExceptionsEnabled(true);
//		cl_context context = setup.getContext();
//		cl_command_queue commandQueue = setup.getCommandQueue();
//		cl_device_id device = setup.getDevice();
//		
//		String source = KernelReader.readFile("Kernels/Histogram_Equalization_Kernel");
//		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] {source}, null, null);
//		
//		
//		
//		int numBins = 5;
//		
//		int[] imageRaster = {0, 1, 1, 2,
//							 3, 4, 1, 2,
//							 1, 0, 2, 2,
//							 3, 3, 3, 3};
//		
//
//
//		
//				
//		int[] dimensions = {numBins, 16, 4, 4};
//		
//		
//		
//		
//		Pointer ptrRaster = Pointer.to(imageRaster);
//		Pointer ptrDimensions = Pointer.to(dimensions);
//
//		
//		cl_mem memRaster = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
//				Sizeof.cl_int * imageRaster.length, ptrRaster, null);
//		cl_mem memDimensions = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
//				Sizeof.cl_int * dimensions.length, ptrDimensions, null);
//
//		
//		int[] localHistogramsCollection = new int[4 * numBins];
//		cl_mem memOptHistogram = parallelHelper(memRaster, memDimensions, localHistogramsCollection, imageRaster.length / 4, 
//				"optimized_calculate_histogram", program, context, commandQueue, device);
//		
//		int count = 0;
//		for(int i : localHistogramsCollection) {
//			System.out.print(i + " | ");
//			
//			if(count == 4) {
//				System.out.println();
//				count = 0;
//			}
//			else {
//				count++;
//			}
//			
//			
//		}
//		
//		CL.clReleaseProgram(program);
//		CL.clReleaseMemObject(memRaster);
//		CL.clReleaseMemObject(memOptHistogram);
//		CL.clReleaseMemObject(memDimensions);
//		CL.clReleaseCommandQueue(commandQueue);
//	}
}
