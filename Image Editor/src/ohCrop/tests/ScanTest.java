package ohCrop.tests;

import static org.junit.Assert.assertEquals;

import org.jocl.CL;
import org.junit.Before;
import org.junit.Test;

import ohCrop.utilAlgorithms.ParallelScan;
import ohCrop.utilAlgorithms.ParallelSetUp;

/**
 * Class to tests implementation of Hillis-Steele (Inclusive) and Blelloch (Exclusive) Scan.
 * @author Sean Maloney
 */
public class ScanTest {
	
	/**
	 * Enables OpenCL exceptions before running tests.
	 */
	@Before
	public void setExceptions() {
		CL.setExceptionsEnabled(true);
	}
	
	/**
	 * Tests if Hillis-Steele generates the correct output from an input array of all ones.
	 */
	@Test
	public void testHillisSteele() {
		
		int size = 8;
		int[] data = new int[size];
		
		for(int i = 0; i < size; i++) {
			data[i] = 1;
		}
		
		int[] result = new int[size];
		
		int[] expected = new int[size];
		
		int sum = 0;
		for(int i = 0; i < data.length; i++) {
			sum = sum + data[i];
			expected[i] = sum;			
		}
		
		
		ParallelSetUp setup = new ParallelSetUp();
	
		ParallelScan.scan(data, result, setup.getContext(), 
				setup.getCommandQueue(), setup.getDevice(), "hillis_steele_scan");
	
		
		
		
		for(int i = 0; i < data.length; i++) {
			assertEquals("Index " + i + " should contain " + expected[i], expected[i], result[i], 0);
		}
	}
	

	
	/**
	 * Tests if Blelloch generates the correct output from an input array of all ones.
	 */
	@Test
	public void testBlelloch() {
		int size = 8;
		int[] data = new int[size];
		
		for(int i = 0; i < size; i++) {
			data[i] = 1;
		}
		
		int[] result = new int[size];
		
		int[] expected = new int[size];
		
		int sum = 0;
		for(int i = 0; i < data.length; i++) {
			expected[i] = sum;
			sum = sum + data[i];
		}
		
		ParallelSetUp setup = new ParallelSetUp();
		
		ParallelScan.scan(data, result, setup.getContext(), 
				setup.getCommandQueue(), setup.getDevice(), "blelloch_scan");
		
		for(int i = 0; i < data.length; i++) {
			assertEquals("Index " + i + " should contain " + expected[i], expected[i], result[i], 0);
		}
	}
	
	/**
	 * Tests if Hillis-Steele generate the correct output from an input array of all ones whose size is larger than group_size.
	 */
	@Test
	public void testBlellochLargeArray() {
		//TODO: Determine Size
		int size = 2000;
		int[] data = new int[size];
		
		for(int i = 0; i < size; i++) {
			data[i] = 1;
		}
		
		int[] result = new int[size];
		
		int[] expected = new int[size];
		
		int sum = 0;
		for(int i = 0; i < data.length; i++) {
			expected[i] = sum;
			sum = sum + data[i];
		}
		
		
		ParallelSetUp setup = new ParallelSetUp();
	
		ParallelScan.scan(data, result, setup.getContext(), 
				setup.getCommandQueue(), setup.getDevice(), "blelloch_scan");
		
	
		for(int i = 0; i < data.length; i++) {
			assertEquals("Index " + i + " should contain " + expected[i], expected[i], result[i], 0);
			//System.out.println(i + ": " + result[i]);
		}
	}
	
	/**
	 * Tests if Hillis-Steele generate the correct output from an input array of all ones whose size is larger than group_size ^ 2.
	 */
	@Test
	public void testBlellochHugeArray() {
		//TODO: Determine Size
		int size = (1024 * 1024) + 1000;
		int[] data = new int[size];
		
		for(int i = 0; i < size; i++) {
			data[i] = 1;
		}
		
		int[] result = new int[size];
		
		int[] expected = new int[size];
		
		int sum = 0;
		for(int i = 0; i < data.length; i++) {
			expected[i] = sum;
			sum = sum + data[i];
		}
		
		ParallelSetUp setup = new ParallelSetUp();
	
		ParallelScan.scan(data, result, setup.getContext(), 
				setup.getCommandQueue(), setup.getDevice(), "blelloch_scan");
		
	
		for(int i = 0; i < data.length; i++) {
			assertEquals("Index " + i + " should contain " + expected[i], expected[i], result[i], 0);
		}
	}
}
