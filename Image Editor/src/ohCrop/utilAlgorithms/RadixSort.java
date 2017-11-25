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
 * Sorts elements using Radix Sort.
 * 
 * @author Sean Maloney
 */
public class RadixSort extends ParallelAlgorithm {
	
	/**
	 * The time of the kernel execution.
	 */
	private static long TIME;

	/**
	 * Sorts elements non comparatively based on bit values.
	 * 
	 * @param data
	 *            The data to be sorted.
	 * @param keys
	 * 			  The keys associated with the data.
	 * @param result
	 *            The sorted data.
	 * @param resultKeys
	 * 			  The sorted keys.
	 * @param context
	 *            The Open CL context.
	 * @param commandQueue
	 *            The Open CL command queue.
	 * @param device
	 *            The Open CL device.
	 * @return The amount of time taken to sort.
	 */
	public static long sort(int[] data, int[]keys, int[] result, int[] resultKeys, cl_context context, cl_command_queue commandQueue,
			cl_device_id device) {
		TIME = 0;
		CL.setExceptionsEnabled(true);

		String source = KernelReader.readFile("Kernels/Radix_Sort_Kernel");
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] { source }, null, null);
		CL.clBuildProgram(program, 0, null, null, null, null);

		int[] sorting = data;
		int[] sortingKeys = keys;
		for (int i = 0; i < 32; i++) {
			int currentBitPosition = i;

			int[] p = new int[sorting.length];
			int[] notP = new int[sorting.length];

			applyPredicate(sorting, p, notP, currentBitPosition, context, commandQueue, device, program);

			int[] scannedP = new int[p.length];
			int[] scannedNotP = new int[notP.length];

			TIME += ParallelScan.scan(p, scannedP, context, commandQueue, device, "blelloch_scan");
			TIME += ParallelScan.scan(notP, scannedNotP, context, commandQueue, device, "blelloch_scan");

			scatterElements(sorting, sortingKeys, p, notP, scannedP, scannedNotP, result, resultKeys,
					 context, commandQueue, device, program);
					
			
			sorting = result;
			sortingKeys = resultKeys;
			
			
		}

		CL.clReleaseProgram(program);
		return TIME;
	}

	/**
	 * Private helper method to apply the predicate and calcaulte the results of
	 * that and their negation.
	 * 
	 * @param data
	 *            The data to be sorted.
	 * @param predicated
	 *            The predicated data.
	 * @param notPredicated
	 *            The negation of the predicated data.
	 * @param bitPosition
	 *            The bit position to be used in the current predicate.
	 * @param context
	 *            The OpenCL context.
	 * @param commandQueue
	 *            The OpenCL command queue.
	 * @param device
	 *            The OpenCL device.
	 * @param program
	 *            The OpenCL program.
	 */
	private static void applyPredicate(int[] data, int[] predicated, int[] notPredicated, int bitPosition,
			cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {

		int[] bitArray = { bitPosition };

		Pointer ptrData = Pointer.to(data);
		Pointer ptrPredicated = Pointer.to(predicated);
		Pointer ptrNotPredicated = Pointer.to(notPredicated);
		Pointer ptrBitPosition = Pointer.to(bitArray);

		cl_mem memData = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * data.length, ptrData, null);
		cl_mem memPredicated = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * predicated.length, ptrPredicated, null);
		cl_mem memNotPredicated = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * notPredicated.length, ptrNotPredicated, null);
		cl_mem memBitPosition = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * bitArray.length, ptrBitPosition, null);

		cl_kernel kernel = CL.clCreateKernel(program, "apply_predicate", null);

		cl_mem[] objects = {memData, memPredicated, memNotPredicated, memBitPosition};
		setKernelArgs(objects, kernel);


		long[] globalWorkSize = new long[] { data.length };
		long[] localWorkSize = new long[] { calculateLocalSize(data.length, device) };

		long startTime = System.nanoTime();
		
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize, 0, null, null);
		
		long endTime = System.nanoTime();
		
		long timeTaken = endTime - startTime;

		CL.clEnqueueReadBuffer(commandQueue, memPredicated, CL.CL_TRUE, 0, predicated.length * Sizeof.cl_float,
				ptrPredicated, 0, null, null);

		CL.clEnqueueReadBuffer(commandQueue, memNotPredicated, CL.CL_TRUE, 0, notPredicated.length * Sizeof.cl_float,
				ptrNotPredicated, 0, null, null);
		
		TIME += timeTaken;
		
		
		releaseMemObject(objects);
		CL.clReleaseKernel(kernel);
	}

	/**
	 * Calculates addresses for elements to be written to and scatters them accordingly.
	 * @param data The data to be scattered.
	 * @param keys The keys associated with the data.
	 * @param predicated The predicated data.
	 * @param notPredicated The negation of the predicated data.
	 * @param scannedP The scan of the predicated data.
	 * @param notScannedP The scan of the negated predicate data.
	 * @param results The result of the scatter.
	 * @param resultKeys The keys associated with the results.
	 * @param context The OpenCL context.
	 * @param commandQueue The OpenCL command queue.
	 * @param device The OpenCL device.
	 * @param program The OpenCL program.
	 */
	private static void scatterElements(int[] data, int[] keys, int[] predicated, int[] notPredicated, int[] scannedP,
			int[] notScannedP, int results[], int[] resultKeys,
			cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {

		Pointer ptrData = Pointer.to(data);
		Pointer ptrKeys = Pointer.to(keys);
		Pointer ptrPredicated = Pointer.to(predicated);
		Pointer ptrNotPredicated = Pointer.to(notPredicated);
		Pointer ptrScannedP = Pointer.to(scannedP);
		Pointer ptrNotScannedP = Pointer.to(notScannedP);
		Pointer ptrResults = Pointer.to(results);
		Pointer ptrResultKeys = Pointer.to(resultKeys);
		
		cl_mem memData = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * data.length, ptrData, null);
		cl_mem memKeys = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * keys.length, ptrKeys, null);
		cl_mem memPredicated = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * predicated.length, ptrPredicated, null);
		cl_mem memNotPredicated = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * notPredicated.length, ptrNotPredicated, null);
		cl_mem memScannedP = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * scannedP.length, ptrScannedP, null);
		cl_mem memNotScannedP = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * notScannedP.length, ptrNotScannedP, null);
		cl_mem memResults = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * results.length, ptrResults, null);
		cl_mem memResultKeys = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR,
				Sizeof.cl_int * resultKeys.length, ptrResultKeys, null);
		
		cl_kernel kernel = CL.clCreateKernel(program, "scatter_elements", null);
		
		cl_mem[] objects = {memData, memKeys, memPredicated, memNotPredicated, memScannedP, 
				memNotScannedP, memResults, memResultKeys};
		setKernelArgs(objects, kernel);

		
		long[] globalWorkSize = new long[] { data.length };
		long[] localWorkSize = new long[] { calculateLocalSize(data.length, device) };
		
		
		long startTime = System.nanoTime();
		
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize, 0, null, null);
		
		long endTime = System.nanoTime();
		
		long timeTaken = endTime - startTime;
		
		
		
		CL.clEnqueueReadBuffer(commandQueue, memResults, CL.CL_TRUE, 0, results.length * Sizeof.cl_float,
				ptrResults, 0, null, null);

		CL.clEnqueueReadBuffer(commandQueue, memResultKeys, CL.CL_TRUE, 0, resultKeys.length * Sizeof.cl_float,
				ptrResultKeys, 0, null, null);
		
		TIME += timeTaken;
		
		
		releaseMemObject(objects);
		CL.clReleaseKernel(kernel);

	}

}
