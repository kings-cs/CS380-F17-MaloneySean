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
		int[] dimensions = {numBins};
		
		
		
		
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
		
		
		
		int[] distribution = HistogramEquilization.cumulativeFrequencyDistribution(histogramData);
		
		cl_mem distributionData = parallelCumulativeFrequencyDistribution(histogramData, context, commandQueue, device, memHistogram, memDimensions);
		
		int[] ideal = HistogramEquilization.idealizeHistogram(histogramData, imageRaster.length);

		int[] idealCum = HistogramEquilization.cumulativeFrequencyDistribution(ideal);

		int[] mapping = HistogramEquilization.mapHistogram(distribution, idealCum);

		int[] resultData = HistogramEquilization.mapPixels(mapping, imageRaster);
		
		
		BufferedImage result = wrapUp(resultData, original);
		
				
		//Release kernel, program, 
		CL.clReleaseKernel(kernel);
		CL.clReleaseProgram(program);
		CL.clReleaseMemObject(memRaster);
		CL.clReleaseMemObject(memHistogram);
		CL.clReleaseMemObject(memDimensions);

		
		return result;
	}
	
	/**
	 * Helper method to perform the second step of a histogram equalization in Parallel.
	 * 
	 * @param histogramData The histogram bin data.
	 * @param context The OpenCL context used.
	 * @param commandQueue THe OpenCL commandQueue used.
	 * @param device The OpenCL device used.
	 * @param memHistogram The mem object for the used histogram.
	 * @param memDimensions The mem object for the number of bins used.
	 * @return The Cumulative frequecy distribution. 
	 */
	private static cl_mem parallelCumulativeFrequencyDistribution(int[] histogramData, cl_context context, cl_command_queue commandQueue, cl_device_id device, 
			cl_mem memHistogram, cl_mem memDimensions) {
		
		int[] distribution = new int[histogramData.length];
		
		
	
		
		
		
	
		Pointer ptrDistribution = Pointer.to(distribution);

		
		cl_mem memDistribution = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * distribution.length, ptrDistribution, null);
		
		//KERNEL EXECUTION, SHOULD PROBABLY SPLIT THESE UP
		
		//Create the program from the source code
		//Create the OpenCL kernel from the program
		String source = KernelReader.readFile("Kernels/Histogram_Equalization_Kernel");
		
		//System.out.println(source);
		
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] {source}, null, null);
		
		
		//Build the program
		CL.clBuildProgram(program, 0, null, null, null, null);
		
		//Create the kernel
		cl_kernel kernel = CL.clCreateKernel(program, "cumulative_frequency_distribution", null);
		
		//Set the arguments for the kernel
		
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memHistogram));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memDistribution));
		CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memDimensions));

	
		//WORK GROUP STUFF		
		

		long[] size = new long[1];
		CL.clGetDeviceInfo(device, CL.CL_DEVICE_MAX_WORK_GROUP_SIZE, 0, 
				null, size);

		int[] sizeBuffer = new int[(int) size[0]];
		CL.clGetDeviceInfo(device, CL.CL_DEVICE_MAX_WORK_GROUP_SIZE, 
				sizeBuffer.length, Pointer.to(sizeBuffer), null);



		int maxGroupSize = sizeBuffer[0];
		int globalSize = histogramData.length;
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
		long[] globalWorkSize = new long[] {histogramData.length};
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
		CL.clEnqueueReadBuffer(commandQueue, memDistribution, 
				CL.CL_TRUE, 0, distribution.length * Sizeof.cl_float,
				ptrDistribution, 0, null, null);
		
		
		
		
		return memDistribution;
	}
}
