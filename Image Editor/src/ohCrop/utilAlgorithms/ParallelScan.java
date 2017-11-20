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
		padArray(data, paddedData, padSize, maxSize, context, commandQueue, device, program);
		
		int[] paddedResults = new int[data.length];
		
		
		int globalSize = paddedData.length;
		int localSize = getLocalSize(globalSize, maxSize);
	
		
	
		
		int[] accumulator = new int[globalSize / localSize];
		
		
		CL.setExceptionsEnabled(true);
		Pointer ptrData = Pointer.to(paddedData);
		Pointer ptrResult = Pointer.to(paddedResults);
		
		
		cl_mem memData = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * paddedData.length, ptrData, null);
		cl_mem memResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * paddedResults.length, ptrResult, null);
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
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memData));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memResult));
		
//		CL.clSetKernelArg(kernel, 2, Sizeof.cl_int * data.length, null);
//		CL.clSetKernelArg(kernel, 3, Sizeof.cl_int * data.length, null);
		
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
		
		//TODO: Scan again on the accumulator, base case?????
		int[] increments = new int[accumulator.length];
		
		
		if(kernelMethod.equals("blelloch_scan")) {
			if(paddedData.length > 1) {
				scan(accumulator, increments, context, commandQueue, device, "blelloch_scan");
			}
		}
		
		
		
		//Add the increments
		int[] incrementedValues = new int[paddedResults.length];
		incrementScanResult(paddedResults, incrementedValues, increments, context, commandQueue, maxSize);
		
		
		
		
		//Release kernel, program, 
		CL.clReleaseKernel(kernel);
		CL.clReleaseProgram(program);
		
		cl_mem[] objects = {memData, memResult};
		releaseMemObject(objects);
//		CL.clReleaseMemObject(memData);
//		CL.clReleaseMemObject(memResult);
		
		//Put the results in an array of the correct size.
//		for(int i = 0; i < results.length; i++) {
//			results[i] = incrementedValues[i];
//		}
		
		padArray(incrementedValues, results, results.length, maxSize, context, commandQueue, device, program);
		
		return TIME;
	}
	
	/**
	 * Method called to execute a kernel that will add the results of the scanned accumulator to the correct places in the data.
	 * @param data The result of an exclusive scan.
	 * @param results The result of added the increments to the data array.
	 * @param increments The values to be added to the groups in the data.
	 * @param context The OpenCL context.
	 * @param commandQueue The OpenCL commandQueue.
	 * @param maxSize The max work group size as set by the device.
	 */
	private static void incrementScanResult(final int[] data, int[] results, int[] increments, cl_context context, cl_command_queue commandQueue, int maxSize) {
		int globalSize = data.length;
		int localSize = getLocalSize(globalSize, maxSize);
		
		int[] maxGroupSize = {maxSize};
		
		Pointer ptrData = Pointer.to(data);
		Pointer ptrResult = Pointer.to(results);
		Pointer ptrIncrement = Pointer.to(increments);
		Pointer ptrMaxGroupSize = Pointer.to(maxGroupSize);
		
		cl_mem memData = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * data.length, ptrData, null);
		cl_mem memResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * results.length, ptrResult, null);
		cl_mem memIncrements = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * increments.length, ptrIncrement, null);
		cl_mem memMaxGroupSize = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_int * maxGroupSize.length, ptrMaxGroupSize, null);
		
		String source = KernelReader.readFile("Kernels/Scan_Kernel");	
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] {source}, null, null);
		CL.clBuildProgram(program, 0, null, null, null, null);
		cl_kernel kernel = CL.clCreateKernel(program, "add_increments", null);
		
		
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};
		
		
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memData));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memResult));
		CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memIncrements));
		CL.clSetKernelArg(kernel, 3, Sizeof.cl_mem, Pointer.to(memMaxGroupSize));
		
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
		
		cl_mem[] objects = {memData, memResult, memIncrements, memMaxGroupSize};
		releaseMemObject(objects);
		CL.clReleaseKernel(kernel);
		CL.clReleaseProgram(program);
	}
	
	
	
	/**
	 * Private helper method to computer local size.
	 * @param globalSize The global size.
	 * @param maxSize The max size of a work group as set by a device.
	 * @return The local size.
	 */
	private static int getLocalSize(int globalSize, int maxSize) {
		

		int localSize = maxSize;

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
	
	/**
	 * Private helper method to get the max work group size of the device.
	 * @param device The device to be queried.
	 * @return The max work group size.
	 */
	private static int getMaxWorkGroupSize(cl_device_id device) {
		long[] size = new long[1];
		CL.clGetDeviceInfo(device, CL.CL_DEVICE_MAX_WORK_GROUP_SIZE, 0, 
				null, size);

		int[] sizeBuffer = new int[(int) size[0]];
		CL.clGetDeviceInfo(device, CL.CL_DEVICE_MAX_WORK_GROUP_SIZE, 
				sizeBuffer.length, Pointer.to(sizeBuffer), null);
		
		int maxGroupSize = sizeBuffer[0];
		
		return maxGroupSize;
	}
	
	
	/**
	 * Private helper method to calculate the size that an array should be padded to.
	 * @param data The data to be padded.
	 * @param maxSize The max size of a work group as set by the OpenCL device.
	 * @return The size to be padded to.
	 */
	private static int getPadSize(int[] data, int maxSize) {
		int result = 0;
		int newSize = data.length;
		if(data.length > maxSize) {
			while(newSize % maxSize != 0) {
				newSize++;
			}
			
			
		}
		else {
			
			int check = (newSize & (newSize - 1));
			if(check != 0) {
				int powerSize = 1;
				
				while(powerSize < data.length) {
					powerSize *= 2;
				}
				
				newSize = powerSize;
			}
		}
		
		result = newSize;
		
		return result;
	}
	
	/**
	 * Private helper to pad or compress an array in parallel.
	 * @param data The data to be modified.
	 * @param result The resulting array.
	 * @param padSize The size for the array to be changed to.
	 * @param maxSize The max size of a work group as set by the device.
	 * @param context The OpenCL context.
	 * @param commandQueue The OpenCL commandQueue.
	 * @param device The OpenCL device.
	 * @param program The OpenCL program.
	 */
	private static void padArray(int[] data, int[] result, int padSize, int maxSize, cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
		int[] padSizeArray = {padSize};
		
		Pointer ptrData = Pointer.to(data);
		Pointer ptrResult = Pointer.to(result);
		Pointer ptrPadSize = Pointer.to(padSizeArray);
		
		cl_mem memData = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * data.length, ptrData, null);
		cl_mem memResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * result.length, ptrResult, null);
		cl_mem memPadSize = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * padSizeArray.length, ptrPadSize, null);
		
		
		cl_kernel kernel = CL.clCreateKernel(program, "pad_array", null);

		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memData));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memResult));
		CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memPadSize));
		
		long[] globalWorkSize = new long[] { data.length };
		long[] localWorkSize = new long[] { getLocalSize(data.length, maxSize) };
		
		long startTime = System.nanoTime();
		
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize, 0, null, null);
		
		long endTime = System.nanoTime();
		
		long timeTaken = endTime - startTime;
		
		TIME += timeTaken;
		
		
		CL.clEnqueueReadBuffer(commandQueue, memResult, CL.CL_TRUE, 0, result.length * Sizeof.cl_float,
				ptrResult, 0, null, null);
	
		
		cl_mem[] objects = {memData, memResult, memPadSize};
		releaseMemObject(objects);
	}
}
