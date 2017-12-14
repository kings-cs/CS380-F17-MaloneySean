package ohCrop.tests;

import java.awt.Graphics;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

import javax.imageio.ImageIO;
import javax.swing.JOptionPane;

import org.jocl.CL;
import org.jocl.cl_command_queue;
import org.jocl.cl_context;
import org.jocl.cl_device_id;
import org.jocl.cl_program;
import org.junit.Before;
import org.junit.Test;

import ohCrop.editingAlgorithms.SeamlessCloneParallel;
import ohCrop.utilAlgorithms.ParallelAlgorithm;
import ohCrop.utilAlgorithms.ParallelSetUp;

/**
 * Runs the seamless clone algorithm to check for compilation issues.
 * @author Sean Maloney
 */
public class CloneTest  extends ParallelAlgorithm{

	/**
	 * The clone being used.
	 */
	private BufferedImage clone;
	
	/**
	 * The scene being used.
	 */
	private BufferedImage scene;
	
	/**
	 * The setup object being used.
	 */
	private ParallelSetUp setup;
	
	/**
	 * The OpenCL program.
	 */
	@SuppressWarnings("unused")
	private cl_program program;
	
	/**
	 * The OpenCL context.
	 */
	private cl_context context;
	
	/**
	 * The OpenCL command queue.
	 */
	private cl_command_queue commandQueue;
	
	/**
	 * The OpenCL device.
	 */
	private cl_device_id device;
	
	/**
	 * Enables OpenCL exceptions.
	 */
	@Before
	public void enableExceptions() {
		CL.setExceptionsEnabled(true);
		
		
		File cloneFile = new File("Images//SeamlessClone-Bear.png");
		 clone = null;
		File sceneFile = new File("Images//SeamlessClone-BearScene.png");
		 scene = null;
		
		try {
			BufferedImage ri = ImageIO.read(cloneFile);
			clone = ImageIO.read(cloneFile);
			clone = new BufferedImage(ri.getWidth(), ri.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics g = clone.getGraphics();
			g.drawImage(ri, 0 , 0 , null );		
			
		
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "The Image Could Not Be Read From A File At The Given Path", "Oops", 
					JOptionPane.ERROR_MESSAGE);
		}
		
		
		try {
			BufferedImage oRi = ImageIO.read(sceneFile);
			scene = ImageIO.read(sceneFile);
			scene = new BufferedImage(oRi.getWidth(), oRi.getHeight(), BufferedImage.TYPE_INT_ARGB);
			Graphics gO = scene.getGraphics();
			gO.drawImage(oRi, 0 , 0 , null );	
			
		} catch (IOException e) {
			JOptionPane.showMessageDialog(null, "The Image Could Not Be Read From A File At The Given Path", "Oops", 
					JOptionPane.ERROR_MESSAGE);
		}
		
		setup = new ParallelSetUp();
		
		context = setup.getContext();
		commandQueue = setup.getCommandQueue();
		device = setup.getDevice();
		
		program = buildProgram("Kernels/Red_Eye_Kernel", context);
		
	}
	
	/**
	 * Runs the clone algorithm.
	 */
	@Test
	public void runClone() {

		SeamlessCloneParallel.seamlessClone(context, commandQueue, device, scene, clone, 1);
	}

}
