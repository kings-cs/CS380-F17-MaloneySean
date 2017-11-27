package ohCrop.tests;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

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
		//int[] data = {1, 2, 3, 4, 5, 6};
		
		File currentPicture = new File("Images//crop.png");
		BufferedImage original = null;
		try {
			BufferedImage ri = ImageIO.read(currentPicture);
			original = ImageIO.read(currentPicture);
			original = new BufferedImage(ri.getWidth(), ri.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics g = original.getGraphics();
			g.drawImage(ri, 0 , 0 , null );			
		
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "The Image Could Not Be Read From A File At The Given Path", "Oops", 
					JOptionPane.ERROR_MESSAGE);
		}
		
		

		
		int[] result = new int[3];
		
		ParallelSetUp setup = new ParallelSetUp();
		
		RedEyeParallel.redEyeRemoval(setup.getContext(), setup.getCommandQueue(), setup.getDevice(), original, result);
		
//		
//		for(int i = 0; i < result.length; i++) {
//			System.out.println(result[i]);
//		}
	}
	
}