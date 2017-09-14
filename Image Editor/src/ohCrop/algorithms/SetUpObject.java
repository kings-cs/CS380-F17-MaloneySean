package ohCrop.algorithms;

import org.jocl.cl_device_id;
import org.jocl.cl_platform_id;

/**
 * Object used to associate deviceIDs to platformIDs.
 * @author Sean Maloney
 *
 */
public class SetUpObject {
	
	/**
	 * The id of the device.
	 */
	private cl_device_id deviceId;
	
	/**
	 * The id of the platform assocaited to the device.
	 */
	private cl_platform_id platformId;

	/**
	 * Constructor to intialize the two fields.
	 * @param device The id of the device.
	 * @param platform The id of the assocaited platform.
	 */
	public SetUpObject(cl_device_id device, cl_platform_id platform) {
		this.deviceId = device;
		this.platformId = platform;
	}

	/**
	 * Getter for the device id.
	 * @return The id of the device.
	 */
	public cl_device_id getDeviceId() {
		return deviceId;
	}

	/**
	 * Getter for the platform id.
 	 * @return The id of the platform.
	 */
	public cl_platform_id getPlatformId() {
		return platformId;
	}
	
}
