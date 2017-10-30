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
		int numBins = 256;
		
		
		int[] imageRaster = strip(original);
		int[] histogramData = new int[imageRaster.length];
		int[] dimensions = {numBins, imageRaster.length};
		
		
		
		
		Pointer ptrRaster = Pointer.to(imageRaster);
		Pointer ptrHistogram = Pointer.to(histogramData);
		Pointer ptrDimensions = Pointer.to(dimensions);

		
		cl_mem memRaster = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * imageRaster.length, ptrRaster, null);
		cl_mem memHistogram = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * histogramData.length, ptrHistogram, null);
		cl_mem memDimensions = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * dimensions.length, ptrDimensions, null);

		
		//KERNEL EXECUTION, SHOULD PROBABLY SPLIT THESE UP
		
		//Create the program from the source code
		//Create the OpenCL kernel from the program
		String source = KernelReader.readFile("Kernels/Histogram_Equalization_Kernel");
		
		//System.out.println(source);
		
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] {source}, null, null);
		
		
		//Build the program
		CL.clBuildProgram(program, 0, null, null, null, null);
		
		//Create the kernel
		cl_kernel kernel = CL.clCreateKernel(program, "calculate_histogram", null);
		
		//Set the arguments for the kernel
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memRaster));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memHistogram));
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
		CL.clEnqueueReadBuffer(commandQueue, memHistogram, 
				CL.CL_TRUE, 0, histogramData.length * Sizeof.cl_float,
				ptrHistogram, 0, null, null);
		
		//STEP 2
		
		int[] distribution = HistogramEquilization.cumulativeFrequencyDistribution(histogramData);
		
		//cl_mem memDistribution = parallelHelperA(histogramData.length, context, commandQueue, device, memHistogram, memDimensions, "cumulative_frequency_distribution");
		
	
		
		float[] distributionData = new float[histogramData.length];
		float[] histoFloat = new float[histogramData.length];
		for(int i = 0; i < histogramData.length; i++) {
			histoFloat[i] = histogramData[i];
		}
		ParallelScan.scan(histoFloat, distributionData, context, 
				commandQueue, device, "hillis_steele_scan");
		
		Pointer ptrDistribution = Pointer.to(distributionData);
		

		
		cl_mem memDistribution = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * distributionData.length, ptrDistribution, null);
		
		
		//STEP 1
		
		int[] ideal = HistogramEquilization.idealizeHistogram(histogramData, imageRaster.length);

		
//		int[] idealHisto = new int[histogramData.length];
//		cl_mem memIdeal = parallelHelperA(idealHisto, context, commandQueue, device, memHistogram, memDimensions, "idealize_histogram");
		
		
		//STEP 2
		
		int[] idealCum = HistogramEquilization.cumulativeFrequencyDistribution(ideal);
		
		
		//cl_mem memIdealCum = parallelHelperA(histogramData.length, context, commandQueue, device, memIdeal, memDimensions, "cumulative_frequency_distribution");

		float[] idealCumData = new float[histogramData.length];
		float[] idealFloat = new float[histogramData.length];
		for(int i = 0; i < histogramData.length; i++) {
			idealFloat[i] = ideal[i];
			
		ParallelScan.scan(idealFloat, idealCumData, context, 
					commandQueue, device, "hillis_steele_scan");
		}
		
		Pointer ptrIdealCum = Pointer.to(idealCumData);
		
		cl_mem memIdealCum = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * idealCumData.length, ptrIdealCum, null);
		
		
		//STEP 3
		int[] mapping = HistogramEquilization.mapHistogram(distribution, idealCum);
		
		int[] mapDesign = new int[distribution.length];
		cl_mem memMapping = parallelHelperA(mapDesign, context, commandQueue, device, memDistribution, memIdealCum, "map_histogram");
		
		
		//STEP 4
		int[] resultData = HistogramEquilization.mapPixels(mapping, imageRaster);
		
		int[] result = new int[imageRaster.length];
		
		
		parallelHelperA(result, context, commandQueue, device, memMapping, memRaster, "map_pixel");
		
		BufferedImage resultImage = wrapUp(resultData, original);
		
				
		//Release kernel, program, 
		CL.clReleaseKernel(kernel);
		CL.clReleaseProgram(program);
		CL.clReleaseMemObject(memRaster);
		CL.clReleaseMemObject(memHistogram);
		CL.clReleaseMemObject(memDimensions);
		//CL.clReleaseMemObject(memIdeal);
		CL.clReleaseMemObject(memIdealCum);
		CL.clReleaseMemObject(memMapping);
		
		return resultImage;
	}
	
	/**
	 * Helper method to perform the second step of a histogram equalization in Parallel.
	 * 
	 * @param outputData The data to be calculated.
	 * @param context The OpenCL context used.
	 * @param commandQueue THe OpenCL commandQueue used.
	 * @param device The OpenCL device used.
	 * @param memInputData The mem object for input data.
	 * @param memOtherInput Mem object for the second input parameter. (Commonly used for dimensions array).
	 * @param methodName The name of the method in the kernel to be called.
	 * @return The Cumulative frequecy distribution. 
	 */
	private static cl_mem parallelHelperA(int[] outputData, cl_context context, cl_command_queue commandQueue, cl_device_id device, 
			cl_mem memInputData, cl_mem memOtherInput, String methodName) {
		int outputLength = outputData.length;
		
	
		Pointer ptrOutput = Pointer.to(outputData);

		
		cl_mem memOutput = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * outputData.length, ptrOutput, null);
		
		//KERNEL EXECUTION, SHOULD PROBABLY SPLIT THESE UP
		
		//Create the program from the source code
		//Create the OpenCL kernel from the program
		String source = KernelReader.readFile("Kernels/Histogram_Equalization_Kernel");
		
		//System.out.println(source);
		
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] {source}, null, null);
		
		
		//Build the program
		CL.clBuildProgram(program, 0, null, null, null, null);
		
		//Create the kernel
		cl_kernel kernel = CL.clCreateKernel(program, methodName, null);
		
		//Set the arguments for the kernel
		
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memInputData));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memOutput));
		CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memOtherInput));

	
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
		CL.clEnqueueReadBuffer(commandQueue, memOutput, 
				CL.CL_TRUE, 0, outputData.length * Sizeof.cl_float,
				ptrOutput, 0, null, null);
		
		
		
		
		return memOutput;
	}
}
