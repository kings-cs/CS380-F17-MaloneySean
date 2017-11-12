package ohCrop.utilAlgorithms;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Scanner;

/**
 * Class containing a single static method used to convert kernel files into strings.
 * @author Sean Maloney
 */
public class KernelReader {
	/**
	 * Private helper to read a kernel from a file and convert it to a string. 
	 * @param filePath The location of the kernel.
	 * @return The kernel as a string.
	 */
	public static String readFile(String filePath) {
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
}
