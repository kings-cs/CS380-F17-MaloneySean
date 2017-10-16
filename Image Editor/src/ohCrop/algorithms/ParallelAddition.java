package ohCrop.algorithms;


import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_program;

/**
 * Class used to add the entries in two arrays using parallelism.
 * @author seanmaloney
 *
 */
public class ParallelAddition {
	/**
	 * Method used to allocate the memory objects used in the kernel.
	 * 
	 * @param context The OpenCL context used for the parallel computing.
	 * @param commandQueue The OpenCL commandQueue used for the parallel computing.
	 * 
	 * @return An array containing the added entries.
	 */ 
	public float[] parallelAddition(cl_context context, cl_command_queue commandQueue) {
		float[] arrayA = {3, 6 , 4};
		float[] arrayB = {9, 1, 4};
		float[] result = new float[3];
		
		Pointer ptrArrayA = Pointer.to(arrayA);
		Pointer ptrArrayB = Pointer.to(arrayB);
		Pointer ptrResult = Pointer.to(result);
		
		cl_mem memArrayA = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * arrayA.length, ptrArrayA, null);
		cl_mem memArrayB = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * arrayB.length, ptrArrayB, null);
		cl_mem memResult = CL.clCreateBuffer(context, CL.CL_MEM_READ_ONLY | CL.CL_MEM_COPY_HOST_PTR, 
				Sizeof.cl_float * result.length, ptrResult, null);
		
		//KERNEL EXECUTION, SHOULD PROBABLY SPLIT THESE UP
		
		//Create the program from the source code
		//Create the OpenCL kernel from the program
		String source = KernelReader.readFile("Kernels/Hello_Kernel");
		cl_program program = CL.clCreateProgramWithSource(context, 1, new String[] {source}, null, null);
		
		
		//Build the program
		CL.clBuildProgram(program, 0, null, null, null, null);
		
		//Create the kernel
		cl_kernel kernel = CL.clCreateKernel(program, "hello_kernel", null);
		
		//Set the arguments for the kernel
		CL.clSetKernelArg(kernel, 0, Sizeof.cl_mem, Pointer.to(memArrayA));
		CL.clSetKernelArg(kernel, 1, Sizeof.cl_mem, Pointer.to(memArrayB));
		CL.clSetKernelArg(kernel, 2, Sizeof.cl_mem, Pointer.to(memResult));
	
		//Set the work-item dimensions
		long[] globalWorkSize = new long[] {result.length};
		long[] localWorkSize = new long[] {1};
		
		//Execute the kernel
		CL.clEnqueueNDRangeKernel(commandQueue, kernel, 1, null,
				globalWorkSize, localWorkSize, 
				0, null, null);
		
		//Read the output data
		CL.clEnqueueReadBuffer(commandQueue, memResult, 
				CL.CL_TRUE, 0, result.length * Sizeof.cl_float,
				ptrResult, 0, null, null);
		
				
		//Release kernel, program, 
		CL.clReleaseKernel(kernel);
		CL.clReleaseProgram(program);
		CL.clReleaseMemObject(memArrayA);
		CL.clReleaseMemObject(memArrayB);
		CL.clReleaseMemObject(memResult);
		CL.clReleaseCommandQueue(commandQueue);
		CL.clReleaseContext(context);
		
		return result;
	}
	
	
	/**
	 * TEST.
	 * @param args NOT USED.
	 */
	public static void main(String[] args) {
		CL.setExceptionsEnabled(true);
		ParallelSetUp test = new ParallelSetUp();
		ParallelAddition adder = new ParallelAddition();
		
		
		float[] result = adder.parallelAddition(test.getContext(), test.getCommandQueue());
		for(float current : result) {
			System.out.println(current);
		}
		
		//test.parallelAddition();
		
		System.out.println("DONE");
	}
	
}
