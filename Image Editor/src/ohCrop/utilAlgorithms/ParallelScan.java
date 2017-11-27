package ohCrop.utilAlgorithms;


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
 * Class to implement Hillis-Steele and Blelloch scan.
 * @author Sean Maloney
 */
public class ParallelScan extends ParallelAlgorithm{
	
	/**
	 * The time of the kernel execution.
	 */
	private static long TIME;
	
	/**
	 * Method used to setup for parallelism and designate which kernel to run.
	 * @param data The input data.
	 * @param results The output of the scan.
	 * @param context The OpenCL context to be used.
	 * @param device The OpenCL device to be used.
	 * @param commandQueue The OpenCL commandQueue to be used.
	 * @param kernelMethod The name of the method in the kernel to be called.
	 * @return The amount of time taken.
	 */
	public static long scan(final int[] data, int[] results, cl_context context, cl_command_queue commandQueue, cl_device_id device, String kernelMethod) {
		String source = KernelReader.readFile("Kernels/Scan_Kernel");
		
		
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] {source}, null, null);
		//Build the program
		CL.clBuildProgram(program, 0, null, null, null, null);
		
		int maxSize = getMaxWorkGroupSize(device);
		
		int padSize = getPadSize(data, maxSize);
		int[] paddedData = new int[padSize];
		TIME += padArray(data, paddedData, padSize, maxSize, context, commandQueue, device, program);
		
		int[] paddedResults = new int[data.length];
		
		
		int globalSize = paddedData.length;
		int localSize = calculateLocalSize(globalSize, device);
	
		
	
		
		int[] accumulator = new int[globalSize / localSize];
		
		
		int[][] params = {paddedData, paddedResults};
		
		Pointer[] pointers = createPointers(params);
		Pointer ptrResult = pointers[1];
		
		
		cl_mem[] objects = createMemObjects(params, pointers, context) ;
		cl_mem memResult = objects[1];
		
		cl_mem memAccumulator = null;
		
		//KERNEL EXECUTION, SHOULD PROBABLY SPLIT THESE UP
		
		//Create the program from the source code
		//Create the OpenCL kernel from the program

		
		//Create the kernel
		cl_kernel kernel = CL.clCreateKernel(program, kernelMethod, null);
	
		
		
		//Set the work-item dimensions
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};
		
		
		//Set the arguments for the kernel
		setKernelArgs(objects, kernel);
		

		
		Pointer ptrAccumulator = null;
		if(kernelMethod.equals("hillis_steele_scan")) {
			CL.clSetKernelArg(kernel, 2, Sizeof.cl_int * localWorkSize[0], null);
			
		}
		else {
			ptrAccumulator = Pointer.to(accumulator);
			
			
			memAccumulator = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
					Sizeof.cl_int * accumulator.length, ptrAccumulator, null);
			
			CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memAccumulator));
		}
		
		CL.clSetKernelArg(kernel, 3, Sizeof.cl_int * localWorkSize[0], null);
		
		//Execute the kernel
		
		long startTime = System.nanoTime();
		
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);

		long endTime = System.nanoTime();
		
		long timeTaken = endTime - startTime;
		
		TIME += timeTaken;
		
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memResult, 
				CL.CL_TRUE, 0, results.length * Sizeof.cl_int,
				ptrResult, 0, null, null);
		
		if(kernelMethod.equals("blelloch_scan")) {
			CL.clEnqueueReadBuffer(commandQueue, memAccumulator, 
					CL.CL_TRUE, 0, accumulator.length * Sizeof.cl_int,
					ptrAccumulator, 0, null, null);
		}
		
	
		int[] increments = new int[accumulator.length];
		
		
		if(kernelMethod.equals("blelloch_scan")) {
			if(paddedData.length > 1) {
				scan(accumulator, increments, context, commandQueue, device, "blelloch_scan");
			}
		}
		
		
		
		//Add the increments
		int[] incrementedValues = new int[paddedResults.length];
		incrementScanResult(paddedResults, incrementedValues, increments, context, commandQueue, device, maxSize);
		
		
		
		
		//Release kernel, program, 
		CL.clReleaseKernel(kernel);
		
		releaseMemObject(objects);
		
		padArray(incrementedValues, results, results.length, maxSize, context, commandQueue, device, program);
		
		
		CL.clReleaseProgram(program);
		return TIME;
	}
	
	/**
	 * Method called to execute a kernel that will add the results of the scanned accumulator to the correct places in the data.
	 * @param data The result of an exclusive scan.
	 * @param results The result of added the increments to the data array.
	 * @param increments The values to be added to the groups in the data.
	 * @param context The OpenCL context.
	 * @param commandQueue The OpenCL commandQueue.
	 * @param device The OpenCL device used.
	 * @param maxSize The max work group size as set by the device.
	 */
	private static void incrementScanResult(final int[] data, int[] results, int[] increments, cl_context context, cl_command_queue commandQueue, cl_device_id device, int maxSize) {
		int globalSize = data.length;
		int localSize = calculateLocalSize(globalSize, device);
		
		int[] maxGroupSize = {maxSize};
		
		int[][] params = {data, results, increments, maxGroupSize};
		
		Pointer[] pointers = createPointers(params);
		Pointer ptrResult = pointers[1];
		
		cl_mem[] objects = createMemObjects(params, pointers, context) ;
		cl_mem memResult = objects[1];
	
		String source = KernelReader.readFile("Kernels/Scan_Kernel");	
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] {source}, null, null);
		CL.clBuildProgram(program, 0, null, null, null, null);
		cl_kernel kernel = CL.clCreateKernel(program, "add_increments", null);
		
		
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};
		
		
		setKernelArgs(objects, kernel);

		
		long startTime = System.nanoTime();
		
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);

		long endTime = System.nanoTime();
		
		long timeTaken = endTime - startTime;
		
		TIME += timeTaken;
		
		
		CL.clEnqueueReadBuffer(commandQueue, memResult, 
				CL.CL_TRUE, 0, results.length * Sizeof.cl_int,
				ptrResult, 0, null, null);
		
		
		releaseMemObject(objects);
		CL.clReleaseKernel(kernel);
		CL.clReleaseProgram(program);
	}
	
	

	
	

}
