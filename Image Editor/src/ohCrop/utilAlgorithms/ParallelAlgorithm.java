package ohCrop.utilAlgorithms;

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
 * Class to contain helper methods needed by methods that run in parallel.
 * @author Sean Maloney
 */
public class ParallelAlgorithm extends ImageAlgorithm{
	

	/**
	 * Helper method to create pointers from a set of input arrays.
	 * @param params A collections of arrays for pointers to be made based off of.
	 * @return The collection of pointers.
	 */
	protected static Pointer[] createPointers(int[][] params) {
		int length = params.length;
		Pointer[] pointers = new Pointer[length];
		
		for(int i = 0; i < length; i++) {
			pointers[i] = Pointer.to(params[i]);
		}
		
		return pointers;
	}
	
	/**
	 * Helper method to create memObjects from the given parameters.
	 * @param params A collection of arrays to be turned into mem objects.
	 * @param pointers Pointers to the arrays.
	 * @param context The context used to create the mem objects.
	 * @return An array of the created memory objects.
	 */
	protected static cl_mem[] createMemObjects(int[][] params, Pointer[] pointers, cl_context context) {
		int length = params.length;
		cl_mem[] objects = new cl_mem[length];
		
		for(int i = 0; i < length; i++){
			int[] current = params[i];
			
			Pointer ptrCurrent = pointers[i];
			cl_mem memCurrent = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
					Sizeof.cl_int * current.length, ptrCurrent, null);
			
			objects[i] = memCurrent;
		}
		
		return objects;
	}
	
	/**
	 * Method to query the OpenCL device for the max work group size.
	 * @param device The OpenCL device to be queried
	 * @return The max work group size.
	 */
	protected static int getMaxWorkGroupSize(cl_device_id device) {
		int maxSize = 0;
		
		long[] size = new long[1];
		CL.clGetDeviceInfo(device, CL.CL_DEVICE_MAX_WORK_GROUP_SIZE, 0, 
				null, size);

		int[] sizeBuffer = new int[(int) size[0]];
		CL.clGetDeviceInfo(device, CL.CL_DEVICE_MAX_WORK_GROUP_SIZE, 
				sizeBuffer.length, Pointer.to(sizeBuffer), null);
		
		maxSize = sizeBuffer[0];
		return maxSize;
	}
	
	
	/**
	 * Helper method used to calculate the best local size to be used.
	 * @param globalItemCount The global item count.
	 * @param device The open cl device to be used.
	 * @return The optimal local group size.
	 */
	protected static int calculateLocalSize(int globalItemCount, cl_device_id device) {
		
		
		int maxGroupSize = getMaxWorkGroupSize(device);
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
	
	/**
	 * Helper method to release memObjects.
	 * @param objects The memObjects to be realesed.
	 */
	protected static void releaseMemObject(cl_mem[] objects) {
		for(cl_mem current : objects) {
			CL.clReleaseMemObject(current);
		}
	}
	
	/**
	 * Helper method to set arguments for a kernel.
	 * @param objects The objects to be set.
	 * @param kernel The kernel for the arguments.
	 */
	protected static void setKernelArgs(cl_mem[] objects, cl_kernel kernel) {
		for(int i = 0; i < objects.length; i++) {
			cl_mem current = objects[i];
			
			CL.clSetKernelArg(kernel, i, Sizeof.cl_mem, Pointer.to(current));
		}
	}
	
	/**
	 * Helper method to display the time taken in milliseconds to the user.
	 * @param startTime The start time (computed via System.nanoTime()).
	 * @param endTime	The end time (computed via System.nanoTime()).
	 */
	protected static void displayTimeTaken(long startTime, long endTime) {
		long timeTaken = endTime - startTime;
		
		double miliSeconds = timeTaken / 1000000.0;
		JOptionPane.showMessageDialog(null, "Time Taken: " + miliSeconds + " (ms)");
	}
	
	/***
	 * Helper method to build the OpenCL program.
	 * @param kernelLocation The file path for the kernel.
	 * @param context The context used.
	 * @return The OpenCL program.
	 */
	protected static cl_program buildProgram(String kernelLocation, cl_context context) {
		String source = KernelReader.readFile(kernelLocation);
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] { source }, null, null);
		CL.clBuildProgram(program, 0, null, null, null, null);
		
		return program;
	}
	
	
	/**
	 * Private helper method to calculate the size that an array should be padded to.
	 * @param dataLength The original array length.
	 * @param maxSize The max size of a work group as set by the OpenCL device.
	 * @return The size to be padded to.
	 */
	protected static int getPadSize(int dataLength, int maxSize) {
		int result = 0;
		int newSize = dataLength;
		if(dataLength > maxSize) {
			while(newSize % maxSize != 0) {
				newSize++;
			}
			
			
		}
		else {
			
			int check = (newSize & (newSize - 1));
			if(check != 0) {
				int powerSize = 1;
				
				while(powerSize < dataLength) {
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
	 * @return The time taken to perform the padding of the array.
	 */
	protected static long padArray(int[] data, int[] result, int padSize, int maxSize, cl_context context, cl_command_queue commandQueue, cl_device_id device, cl_program program) {
		int[] padSizeArray = {padSize};
		
		int[][] params = {data, result, padSizeArray};
		
		Pointer[] pointers = createPointers(params);
		Pointer ptrResult = pointers[1];
		
		cl_mem[] objects = createMemObjects(params, pointers, context) ;
		cl_mem memResult = objects[1];
		
		
		cl_kernel kernel = CL.clCreateKernel(program, "pad_array", null);

		setKernelArgs(objects, kernel);
		
		long[] globalWorkSize = new long[] { data.length };
		long[] localWorkSize = new long[] { calculateLocalSize(data.length, device) };
		
		long startTime = System.nanoTime();
		
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null, globalWorkSize, localWorkSize, 0, null, null);
		
		long endTime = System.nanoTime();
		
		long timeTaken = endTime - startTime;
		
		
		
		
		CL.clEnqueueReadBuffer(commandQueue, memResult, CL.CL_TRUE, 0, result.length * Sizeof.cl_float,
				ptrResult, 0, null, null);
	
		
		
		releaseMemObject(objects);

		return timeTaken;
	}
}
