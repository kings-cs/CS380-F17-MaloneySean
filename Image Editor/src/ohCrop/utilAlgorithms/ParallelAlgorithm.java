package ohCrop.utilAlgorithms;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;

/**
 * Class to contain helper methods needed by methods that run in parallel.
 * @author Sean Maloney
 */
public class ParallelAlgorithm extends ImageAlgorithm{
	
	/**
	 * Private helper method used to calculate the best local size to be used.
	 * @param globalItemCount The global item count.
	 * @param device The open cl device to be used.
	 * @return The optimal local group size.
	 */
	protected static int calculateLocalSize(int globalItemCount, cl_device_id device) {
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
}
