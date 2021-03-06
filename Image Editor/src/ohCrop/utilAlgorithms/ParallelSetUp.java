package ohCrop.utilAlgorithms;

import java.util.HashMap;

import org.jocl.CL;
import org.jocl.Pointer;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_context_properties;
import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;

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
	 * The list of platforms on this device.
	 */
	private cl_platform_id[] platforms;
	
	/**
	 * The number of platforms on this device.
	 */
	private int numPlatforms;
	
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
	 * Constructor to set up the fields of the class using various private methods and a specific device.
	 * @param deviceID The index of the desired device.
	 * @param platformID The id of the platform associated to the device.
	 */
	public ParallelSetUp(cl_device_id deviceID, cl_platform_id platformID) {
		platform = platformID;
		device = deviceID;
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
		numPlatforms = numPlatformsArray[0];

		//Obtain a platform ID
		platforms = new cl_platform_id[numPlatforms];
		CL.clGetPlatformIDs(platforms.length, platforms, null);
		cl_platform_id platform = platforms[platformIndex];

		return platform;
	}
	
	/**
	 * Private helper method used to set up a default device to be used.
	 * @return The id of the device.
	 */
	private cl_device_id findDevice() {
		//Obtain the number of devices for the platform
		int[] numDevicesArray = new int[1];
		CL.clGetDeviceIDs(platform, deviceType, 0, null, numDevicesArray);
		int numDevices = numDevicesArray[0];
		

		//Obtain a device ID
		cl_device_id[] devices = new cl_device_id[numDevices];
		CL.clGetDeviceIDs(platform, deviceType, numDevices, devices, null);
		
		
		cl_device_id device = devices[deviceIndex];
		
		int index = 0;
		boolean foundGPU = false;
		while(!foundGPU && index < devices.length) {
			cl_device_id currentDevice = devices[index];
			
			long[] size = new long[1];
			CL.clGetDeviceInfo(currentDevice, CL.CL_DEVICE_TYPE, 0, 
					null, size);

			long[] typeBuffer = new long[(int)size[0]];
			CL.clGetDeviceInfo(currentDevice, CL.CL_DEVICE_TYPE, 
					typeBuffer.length, Pointer.to(typeBuffer), null);
			
			if(typeBuffer[0] == CL.CL_DEVICE_TYPE_GPU) {
				device = currentDevice;
				
				foundGPU = true;
			}
			
			index++;
		}
		
		
		
		return device;
	}
	
	
	/**
	 * Public method used to generate a map used to allow the user select what computational device they would like to use.
	 * @return The list of devices.
	 */
	public HashMap<String,SetUpObject> listDevices() {
		//TODO: Need to ensure that a GPU is defaultly selected.
		HashMap<String, SetUpObject> deviceList = new HashMap<String, SetUpObject>();

		
		for(cl_platform_id current  : platforms) {


			int[] numDevicesArray = new int[1];
			CL.clGetDeviceIDs(current, CL.CL_DEVICE_TYPE_ALL, 0, null, numDevicesArray);
			int numDevices = numDevicesArray[0];

			cl_device_id[] devicesArray = new cl_device_id[numDevices];
			CL.clGetDeviceIDs(current, CL.CL_DEVICE_TYPE_ALL, numDevices, devicesArray, null);
			
			

			for(int i = 0; i < devicesArray.length; i++) {

				
				
				long[] size = new long[1];
				CL.clGetDeviceInfo(devicesArray[i], CL.CL_DEVICE_NAME, 0, 
						null, size);

				byte[] buffer = new byte[(int)size[0]];
				CL.clGetDeviceInfo(devicesArray[i], CL.CL_DEVICE_NAME, 
						buffer.length, Pointer.to(buffer), null);

				String deviceName = new String(buffer, 0, buffer.length - 1);
				
				
				SetUpObject nextObject = new SetUpObject(devicesArray[i], current);
				


				//TODO: ASK JUMP ABOUT THIS
						
				CL.clGetDeviceInfo(device, CL.CL_DEVICE_TYPE, 0, 
						null, size);

				long[] typeBuffer = new long[(int)size[0]];
				CL.clGetDeviceInfo(device, CL.CL_DEVICE_TYPE, 
						typeBuffer.length, Pointer.to(typeBuffer), null);
				
				if(typeBuffer[0] == CL.CL_DEVICE_TYPE_GPU) {
					deviceName = "(GPU) " + deviceName;
				}
				else if(typeBuffer[0] == CL.CL_DEVICE_TYPE_CPU) {
					deviceName = "(CPU) " + deviceName;
				}
				else {
					deviceName = "(X) " + deviceName;
				}
				
				
				//System.out.println(deviceName);
				deviceList.put(deviceName, nextObject);
				//String deviceType = new String(typeBuffer, 0, typeBuffer.length - 1);
				//System.out.println(deviceType);
			}
		}
		return deviceList;
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
	 * Getter for context.
	 * @return context.
	 */
	public cl_context getContext() {
		return context;
	}

	/**
	 * Getter for the command queue.
	 * @return commandQueue.
	 */
	public cl_command_queue getCommandQueue() {
		return commandQueue;
	}
	
	/**
	 * Getter for the device.
	 * @return device.
	 */
	public cl_device_id getDevice() {
		return device;
	}


	
	
}
