package ohCrop.tests;

import org.jocl.CL;
import org.junit.Before;
import org.junit.Test;

import ohCrop.editingAlgorithms.RedEyeParallel;
import ohCrop.utilAlgorithms.ParallelSetUp;

/**
 * Class to test functionality of pieces used in red eye removal.
 * @author Sean Maloney
 */
public class RedEyeTest {

	/**
	 * Enables OpenCL exceptions.
	 */
	@Before
	public void enableExceptions() {
		CL.setExceptionsEnabled(true);
	}
	
	/**
	 * Tests the averaging of the channels.
	 */
	@Test
	public void testAverageChannels() {
		int[] data = {1, 2, 3, 4, 5, 6};
		
		ParallelSetUp setup = new ParallelSetUp();
		
		int[] result = RedEyeParallel.redEyeRemoval(setup.getContext(), setup.getCommandQueue(), setup.getDevice(), data);
		
		
		for(int i = 0; i < result.length; i++) {
			System.out.println(result[i]);
		}
	}
	
}
