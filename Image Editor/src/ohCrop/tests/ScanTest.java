package ohCrop.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ohCrop.algorithms.ParallelScan;
import ohCrop.algorithms.ParallelSetUp;

/**
 * Class to tests implementation of Hillis-Steele (Inclusive) and Blelloch (Exclusive) Scan.
 * @author Sean Maloney
 */
public class ScanTest {
	
	/**
	 * Tests if Hillis-Steele generates the correct output from an input array of all ones.
	 */
	@Test
	public void testHillisSteele() {
		int size = 8;
		float[] data = new float[size];
		
		for(int i = 0; i < size; i++) {
			data[i] = 1;
		}
		
		float[] result = new float[size];
		
		
		ParallelSetUp setup = new ParallelSetUp();
	
		ParallelScan.scan(data, result, setup.getContext(), 
				setup.getCommandQueue(), setup.getDevice(), "hillis_steele_scan");
	
		assertEquals("Index 0 should contain 1", 1, result[0], 0);
		assertEquals("Index 1 should contain 2", 2, result[1], 0);
		assertEquals("Index 2 should contain 3", 3, result[2], 0);
		assertEquals("Index 3 should contain 4", 4, result[3], 0);
		assertEquals("Index 4 should contain 5", 5, result[4], 0);
		assertEquals("Index 5 should contain 6", 6, result[5], 0);
		assertEquals("Index 6 should contain 7", 7, result[6], 0);
		assertEquals("Index 7 should contain 8", 8, result[7], 0);
	}
	
	/**
	 * Tests if Blelloch generates the correct output from an input array of all ones.
	 */
	@Test
	public void testBlelloch() {
		int size = 8;
		float[] data = new float[size];
		
		for(int i = 0; i < size; i++) {
			data[i] = 1;
		}
		
		float[] result = new float[size];
		
		ParallelSetUp setup = new ParallelSetUp();
		
		ParallelScan.scan(data, result, setup.getContext(), 
				setup.getCommandQueue(), setup.getDevice(), "blelloch_scan");
		
		
		for(int i = 0; i < result.length; i++) {
			System.out.println(result[i]);
		}
		
		assertEquals("Index 0 should contain 0", 0, result[0], 0);
		assertEquals("Index 1 should contain 1", 1, result[1], 0);
		assertEquals("Index 2 should contain 2", 2, result[2], 0);
		assertEquals("Index 3 should contain 3", 3, result[3], 0);
		assertEquals("Index 4 should contain 4", 4, result[4], 0);
		assertEquals("Index 5 should contain 5", 5, result[5], 0);
		assertEquals("Index 6 should contain 6", 6, result[6], 0);
		assertEquals("Index 7 should contain 7", 7, result[7], 0);
	}
}
