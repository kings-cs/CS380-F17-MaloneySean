package ohCrop.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import ohCrop.editingAlgorithms.ParallelSetUp;
import ohCrop.utilAlgorithms.RadixSort;

/**
 * Class used to test the functionality of Radix Sort.
 * @author Sean Maloney
 */
public class SortTest {

	/**
	 * Tests that radix sort works on a small data set.
	 */
	@Test
	public void testSmallSet() {
		int[] data = {10, 11, 2, 9, 0, 6, 1, 4, 7, 3, 8, 5};
		int[] keys = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
		int[] expected = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
		
//		int[] data = {2, 0, 6, 1, 4, 7, 3, 8, 5};
//		int[] expected = {0, 1, 2, 3, 4, 5, 6, 7, 8};
		
		ParallelSetUp setup = new ParallelSetUp();
		
		int[] results = new int[data.length];
		int[] resultsKeys = new int[data.length];
		long time = RadixSort.sort(data, keys, results, resultsKeys, setup.getContext(), setup.getCommandQueue(), setup.getDevice());
		
		
		
		for(int i = 0; i < data.length; i++) {
			assertEquals("The value at index " + i + " should be : " + expected[i], expected[i], results[i]);
		}
		
		double timeTaken = time / 1000000.0;
		System.out.println(timeTaken);
	}
	
	
	
	/**
	 * Tests that radix sort works on a medium data set.
	 */
	@Test
	public void testMediumSet() {
		int[] data = new int[2000];
		int[] keys = new int[2000];
		int[] expected = new int[2000];
		
		
		//TODO: Ask Jump if a backwards data unsorted enough to test this on
		for(int i = data.length - 1; i >= 0; i--) {
			data[i] = i;
		}
		
		for(int i = 0; i < expected.length; i++) {
			keys[i] = i;
			expected[i] = i;
		}
		
		ParallelSetUp setup = new ParallelSetUp();
		
		int[] results = new int[data.length];
		int[] resultsKeys = new int[data.length];
		long time = RadixSort.sort(data, keys, results, resultsKeys, setup.getContext(), setup.getCommandQueue(), setup.getDevice());
		
		for(int i = 0; i < data.length; i++) {
			assertEquals("The value at index " + i + " should be : " + expected[i], expected[i], results[i]);
		}
		
		double timeTaken = time / 1000000.0;
		System.out.println(timeTaken);
	}
	
	/**
	 * Tests that radix sort works on a large data set.
	 */
	@Test
	public void testLargeSet() {
		int size = (1024 * 1024) + 1;
		int[] data = new int[size];
		int[] keys = new int[size];
		int[] expected = new int[size];
		
		
		//TODO: Ask Jump if a backwards data unsorted enough to test this on
		for(int i = data.length - 1; i >= 0; i--) {
			data[i] = i;
		}
		
		for(int i = 0; i < expected.length; i++) {
			keys[i] = i;
			expected[i] = i;
		}
		
		ParallelSetUp setup = new ParallelSetUp();
		
		int[] results = new int[data.length];
		int[] resultsKeys = new int[data.length];
		long time = RadixSort.sort(data, keys, results, resultsKeys, setup.getContext(), setup.getCommandQueue(), setup.getDevice());
		
		
		for(int i = 0; i < data.length; i++) {
			assertEquals("The value at index " + i + " should be : " + expected[i], expected[i], results[i]);
		}
		
		double timeTaken = time / 1000000.0;
		System.out.println(timeTaken);
	}
	
	
	
}
