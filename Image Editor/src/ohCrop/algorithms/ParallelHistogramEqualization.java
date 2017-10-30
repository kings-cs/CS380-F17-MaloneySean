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
		String source = KernelReader.readFile("Kernels/Histogram_Equalization_Kernel");
		
		//System.out.println(source);
		
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] {source}, null, null);
		
		
		
		int numBins = 256;
		
		
		int[] imageRaster = strip(original);
		//int[] histogramData = new int[numBins];
		int[] dimensions = {numBins, imageRaster.length};
		
		
		
		
		Pointer ptrRaster = Pointer.to(imageRaster);
		Pointer ptrDimensions = Pointer.to(dimensions);

		
		cl_mem memRaster = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * imageRaster.length, ptrRaster, null);
		cl_mem memDimensions = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * dimensions.length, ptrDimensions, null);

		
		//STEP 1. CALCULATE HISTOGRAM (UNTESTED)

		long startTime = System.nanoTime();
		
		int[] histogram = HistogramEquilization.calculateHistogram(imageRaster);
		
		
		Pointer ptrHistogram = Pointer.to(histogram);
		cl_mem memHistogram = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * histogram.length, ptrHistogram, null);
		
		//cl_mem memHistogram = parallelHelperA(memRaster, memDimensions, histogramData, "calculate_histogram",  program, context, commandQueue, device);
		
		
		//STEP 2. CUMULATIVE FREQUENCY DISTRIBUTION (TESTED!!!)
		//int[] distribution = HistogramEquilization.cumulativeFrequencyDistribution(histogram);
		
		
		
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
		
//		int[] distribution = new int[distributionData.length];
//		for(int i = 0; i < distributionData.length; i++) {
//			distribution[i] = (int) distributionData[i];
//			//TODO: REMOVE LOOP ONCE ALL IN PARALLEL
//		}
		
		
		
		//STEP 3. IDEALIZED HISTOGRAM (TESTED!!!)
		
		//int[] ideal = HistogramEquilization.idealizeHistogram(histogram, imageRaster.length);

		
		int[] ideal = new int[histogram.length];
		cl_mem memIdeal = parallelHelperA(memHistogram, memDimensions, ideal, "idealize_histogram", program, context, commandQueue, device);
		
		
		//STEP 4. IDEAL CUMULATIVE FREQUNCY DISTRIBUTION (TESTED!!!)
		
		//int[] idealCum = HistogramEquilization.cumulativeFrequencyDistribution(ideal);
		
		
		
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
		
		
//		int[] idealCum = new int[idealCumData.length];
//		for(int i = 0; i < idealCumData.length; i++) {
//			idealCum[i] = (int) idealCumData[i];
//			//TODO: REMOVE LOOP ONCE ALL IN PARALLEL
//		}
		
		
		
		//STEP 5. DESIGN MAPPING (TESTED!!!)
		//int[] mapping = HistogramEquilization.mapHistogram(distribution, idealCum);
		
		
		int[] mapping = new int[distributionData.length];
		cl_mem memMapping = parallelHelperA(memDistribution, memIdealCum, mapping, "map_histogram", program, context, commandQueue, device);
		
		
		//STEP 6. MAP PIXELS (TESTED!!!)
		//int[] resultData = HistogramEquilization.mapPixels(mapping, imageRaster);
		
		
		int[] resultData = new int[imageRaster.length];
	
		parallelHelperA(memMapping, memRaster, resultData, "map_pixels", program, context, commandQueue, device);
		
		long endTime = System.nanoTime();
		
		long timeTaken = endTime - startTime;
		
		double miliSeconds = timeTaken / 1000000.0;
		JOptionPane.showMessageDialog(null, "Time Taken: " + miliSeconds + " (ms)");
		
		
		BufferedImage resultImage = wrapUp(resultData, original);
		
				
		//Release kernel, program, 
	
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
	 * @param methodName The name of the method to be called.
	 * @param program The OpenCL program used.
	 * @param context The OpenCL context used.
	 * @param commandQueue The OpenCL command queue used.
	 * @param device The OpenCL device used.
	 * @return The memory object created during this methods execution, returned so that it can be later released and used as future parameters.
	 */
	public static cl_mem parallelHelperA(cl_mem memInputData, cl_mem memOtherInput, int[] outputData, String methodName, 
			cl_program program, cl_context context, cl_command_queue commandQueue, cl_device_id device) {
		
		//TODO: MAKE SURE TO CHANGE THIS BACK TO PRIVATE
		
		int outputLength = outputData.length;
		
	
		Pointer ptrOutput = Pointer.to(outputData);

		
		cl_mem memOutput = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * outputData.length, ptrOutput, null);
		
		//KERNEL EXECUTION, SHOULD PROBABLY SPLIT THESE UP

		
		
		//Build the program
		CL.clBuildProgram(program, 0, null, null, null, null);
		
		//Create the kernel
		cl_kernel kernel = CL.clCreateKernel(program, methodName, null);
		
		//Set the arguments for the kernel
		
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memInputData));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memOtherInput));
		CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memOutput));

	
		//WORK GROUP STUFF		
		

		long[] size = new long[1];
		CL.clGetDeviceInfo(device, CL.CL_DEVICE_MAX_WORK_GROUP_SIZE, 0, 
				null, size);

		int[] sizeBuffer = new int[(int) size[0]];
		CL.clGetDeviceInfo(device, CL.CL_DEVICE_MAX_WORK_GROUP_SIZE, 
				sizeBuffer.length, Pointer.to(sizeBuffer), null);



		int maxGroupSize = sizeBuffer[0];
		int globalSize = outputLength;
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
		long[] globalWorkSize = new long[] {outputLength};
		long[] localWorkSize = new long[] {localSize};

		
		//Execute the kernel
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
		
		
		
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memOutput, 
				CL.CL_TRUE, 0, outputData.length * Sizeof.cl_float,
				ptrOutput, 0, null, null);
		
		
		
		CL.clReleaseKernel(kernel);
		
		return memOutput;
	}
}
