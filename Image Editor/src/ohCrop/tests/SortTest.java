package ohCrop.tests;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

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
		int[] expected = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11};
		
		//TODO: FUCKIN CALL RADIX SORT HERE
		
		
		for(int i = 0; i < data.length; i++) {
			assertEquals("The value at index " + i + " should be : " + expected[i], expected[i], data[i]);
		}
		
		
	}
	
	
	
	/**
	 * Tests that radix sort works on a medium data set.
	 */
	@Test
	public void testMediumSet() {
		int[] data = new int[2000];
		int[] expected = new int[2000];
		
		
		//TODO: Ask Jump if a backwards data unsorted enough to test this on
		for(int i = data.length; i >= 0; i--) {
			data[i] = i;
		}
		
		for(int i = 0; i < expected.length; i++) {
			expected[i] = i;
		}
		
		//TODO: FUCKING CALL RADIX SORT HERE
		
		for(int i = 0; i < data.length; i++) {
			assertEquals("The value at index " + i + " should be : " + expected[i], expected[i], data[i]);
		}
	}
	
	/**
	 * Tests that radix sort works on a large data set.
	 */
	@Test
	public void testLargeSet() {
		int size = (1024 * 1024) + 1;
		int[] data = new int[size];
		int[] expected = new int[size];
		
		
		//TODO: Ask Jump if a backwards data unsorted enough to test this on
		for(int i = data.length; i >= 0; i--) {
			data[i] = i;
		}
		
		for(int i = 0; i < expected.length; i++) {
			expected[i] = i;
		}
		
		//TODO: FUCKING CALL RADIX SORT HERE
		
		for(int i = 0; i < data.length; i++) {
			assertEquals("The value at index " + i + " should be : " + expected[i], expected[i], data[i]);
		}
	}
	
	
	
}
