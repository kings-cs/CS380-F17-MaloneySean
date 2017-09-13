package ohCrop.algorithms;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.Sizeof;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_kernel;
import org.jocl.cl_mem;
import org.jocl.cl_platform_id;
import org.jocl.cl_program;

/**
 * Class used to handle the set up required to run a parallel algorithm.
 * @author Sean Maloney
 */
public class ParallelSetUp {
	
	//Selecting the first device for now
	/**
	 * Final field stating the index of the platform used.
	 */
	private final int platformIndex = 0;
	
	/**
	 * Controls what kinds of devices can be used.
	 */
	private final long deviceType = CL.CL_DEVICE_TYPE_ALL;
	
	/**
	 * Final field stating the index of the device used.
	 */
	private final int deviceIndex = 0;
	
	/**
	 * The platform used by the parallelism.
	 */
	private cl_platform_id platform;
	
	/**
	 * The context used by the parallelism.
	 */
	private cl_context context;
	
	/**
	 * The id of the device used in the execution of the kernel.
	 */
	private cl_device_id device;
	
	/**
	 * The command queue controlling the parallelism across the device.
	 */
	private cl_command_queue commandQueue;
	
	/**
	 * Constructor to set up the fields of the class using various private methods.
	 */
	public ParallelSetUp() {
		platform = choosePlatform();
		device = findDevice();
		context = createContext();
		commandQueue = createCommandQueue();
	}
	
	/**
	 * Private helper method used to choose the platform used.
	 * @return The id of the platform.
	 */
	private cl_platform_id choosePlatform() {
		//Obtain the number of platforms
		int[] numPlatformsArray = new int[1];
		CL.clGetPlatformIDs(0, null, numPlatformsArray);
		int numPlatforms = numPlatformsArray[0];

		//Obtain a platform ID
		cl_platform_id[] platforms = new cl_platform_id[numPlatforms];
		CL.clGetPlatformIDs(platforms.length, platforms, null);
		cl_platform_id platform = platforms[platformIndex];

		return platform;
	}
	
	/**
	 * Private helper method used to find the id of the device to be used.
	 * @return The id of the device.
	 */
	private cl_device_id findDevice() {
		//Obtain the number of devices for the platform
		int numDevicesArray[] = new int[1];
		CL.clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
		int numDevices = numDevicesArray[0];

		//Obtain a device ID
		cl_device_id[] devices = new cl_device_id[numDevices];
		CL.clGetDeviceIDs(platform, deviceType, numDevices, devices, null);

		cl_device_id device = devices[deviceIndex];
		
		return device;
	}
	
	/**
	 * Private helper used to make the OpenCL context.
	 * @return The context used.
	 */
	private cl_context createContext() {
		//Initialize the context properties
		cl_context_properties contextProperties = new cl_context_properties();
		contextProperties.addProperty(CL.CL_CONTEXT_PLATFORM, platform);
		
		
		
		//Create a contest for the selected device
		cl_context context = CL.clCreateContext(contextProperties, 1, new cl_device_id[] {device}, null, null, null);
		
		
		return context;
	}
	
	/**
	 * Private helper used to create the OpenCL command queue.
	 * @return The command queue.
	 */
	private cl_command_queue createCommandQueue() {
		 commandQueue = CL.clCreateCommandQueue(context, device, 0, null);
		 
		 return commandQueue;
	}
	
	/**
	 * Method used to allocate the memory objects used in the kernel.
	 */
	public void parallelAddition() {
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
		String source = readFile("Kernels/Hello_Kernel");
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
		
		
		for(float current : result) {
			System.out.println(current);
		}
		
		//Release kernel, program, 
		CL.clReleaseKernel(kernel);
		CL.clReleaseProgram(program);
		CL.clReleaseMemObject(memArrayA);
		CL.clReleaseMemObject(memArrayB);
		CL.clReleaseMemObject(memResult);
		CL.clReleaseCommandQueue(commandQueue);
		CL.clReleaseContext(context);
	}
	
	/**
	 * Private helper to read a kernel from a file and convert it to a string.
	 * @param filePath The location of the kernel.
	 * @return The kernel as a string.
	 */
	private String readFile(String filePath) {
		File helloKernel = new File(filePath);
		StringBuffer kernelString = new StringBuffer();
		
		
		try {
			Scanner fileReader = new Scanner(helloKernel);
			
			while(fileReader.hasNextLine()) {
				String current = fileReader.nextLine();
				kernelString.append(current).append("\n");
			}
			
			fileReader.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return kernelString.toString();
	}
	
	/**
	 * TEST.
	 * @param args NOT USED.
	 */
	public static void main(String[] args) {
		CL.setExceptionsEnabled(true);
		ParallelSetUp test = new ParallelSetUp();
		
		test.parallelAddition();
		
		System.out.println("DONE");
	}
}
