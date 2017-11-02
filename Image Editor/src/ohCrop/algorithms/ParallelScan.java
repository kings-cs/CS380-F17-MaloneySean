package ohCrop.algorithms;


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
public class ParallelScan {
	
	
	/**
	 * Method used to setup for parallelism and designate which kernel to run.
	 * @param data The input data.
	 * @param results The output of the scan.
	 * @param context The OpenCL context to be used.
	 * @param device The OpenCL device to be used.
	 * @param commandQueue The OpenCL commandQueue to be used.
	 * @param kernelMethod The name of the method in the kernel to be called.
	 */
	public static void scan(final float[] data, float[] results, cl_context context, cl_command_queue commandQueue, cl_device_id device, String kernelMethod) {
		//TODO: I have this sinking feeling this may not always work.
		float[] paddedData = padArray(data);
		
		int globalSize = paddedData.length;
		int localSize = getLocalSize(globalSize, device);
		
		
		
		float[] accumulator = new float[localSize];
		
		CL.setExceptionsEnabled(true);
		Pointer ptrData = Pointer.to(paddedData);
		Pointer ptrResult = Pointer.to(results);
		
		
		cl_mem memData = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * paddedData.length, ptrData, null);
		cl_mem memResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * results.length, ptrResult, null);
		cl_mem memAccumulator = null;
		
		//KERNEL EXECUTION, SHOULD PROBABLY SPLIT THESE UP
		
		//Create the program from the source code
		//Create the OpenCL kernel from the program
		String source = KernelReader.readFile("Kernels/Scan_Kernel");
		
		//System.out.println(source);
		
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] {source}, null, null);
		
		
		//Build the program
		CL.clBuildProgram(program, 0, null, null, null, null);
		
		//Create the kernel
		cl_kernel kernel = CL.clCreateKernel(program, kernelMethod, null);
	
		
		
		//Set the work-item dimensions
		long[] globalWorkSize = new long[] {globalSize};
		long[] localWorkSize = new long[] {localSize};
		
		
		//Set the arguments for the kernel
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memData));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memResult));

//		CL.clSetKernelArg(kernel, 2, Sizeof.cl_float * data.length, null);
//		CL.clSetKernelArg(kernel, 3, Sizeof.cl_float * data.length, null);
		
		
		if(kernelMethod.equals("hillis_steele_scan")) {
			CL.clSetKernelArg(kernel, 2, Sizeof.cl_float * localWorkSize[0], null);
			
		}
		else {
			Pointer ptrAccumulator = Pointer.to(accumulator);
			memAccumulator = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
					Sizeof.cl_float * accumulator.length, ptrAccumulator, null);
			
			CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memAccumulator));
		}
		
		CL.clSetKernelArg(kernel, 3, Sizeof.cl_float * localWorkSize[0], null);
		
		//Execute the kernel
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);

		
		
		
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memResult, 
				CL.CL_TRUE, 0, results.length * Sizeof.cl_float,
				ptrResult, 0, null, null);
		

		
				
		//Release kernel, program, 
		CL.clReleaseKernel(kernel);
		CL.clReleaseProgram(program);
		CL.clReleaseMemObject(memData);
		CL.clReleaseMemObject(memResult);
	}
	
	/**
	 * Private helper method to computer local size.
	 * @param globalSize The global size.
	 * @param device The OpenCL device used.
	 * @return The local size.
	 */
	private static int getLocalSize(int globalSize, cl_device_id device) {
		long[] size = new long[1];
		CL.clGetDeviceInfo(device, CL.CL_DEVICE_MAX_WORK_GROUP_SIZE, 0, 
				null, size);

		int[] sizeBuffer = new int[(int) size[0]];
		CL.clGetDeviceInfo(device, CL.CL_DEVICE_MAX_WORK_GROUP_SIZE, 
				sizeBuffer.length, Pointer.to(sizeBuffer), null);
		
		int maxGroupSize = sizeBuffer[0];

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
	
	/**
	 * Pads the array to be a length that is a power of 2.
	 * @param data The input data.
	 * @return The padded array.
	 */
	private static float[] padArray(float[] data) {
		float[] result = data;
		
		double power = Math.log(data.length) / Math.log(2);
		
		double cieling = Math.ceil(power);
		int size = (int) Math.pow(2, cieling);
		
		if(size != data.length) {
			
			result = new float[size];
			
			for(int i = 0; i < data.length; i++) {
				result[i] = data[i];
			}
		}
		
		return result;
	}
}
