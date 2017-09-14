package ohCrop.gui;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.HashMap;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import org.jocl.cl_device_id;

public class DeviceListGUI extends JFrame{
	
	private JComboBox<String> deviceList;

	private JButton okay;
	
	private JButton cancel;
	
	private String result;
	/**
	 * 
	 */
	private static final long serialVersionUID = 7591127670610543268L;
	
	public DeviceListGUI(HashMap<String, cl_device_id> deviceMap) {
		this.setLayout(new GridLayout(2, 1));
		
		okay = new JButton("Okay");
		cancel = new JButton("Cancel");
		
		JLabel device = new JLabel("Devices: ");
		
		deviceList = new JComboBox<String>();
		
		for(String current : deviceMap.keySet()) {
			deviceList.addItem(current);
		}
		
		result = deviceList.getItemAt(0);
		
		JPanel centerPanel = new JPanel(new FlowLayout());
		centerPanel.add(device);
		centerPanel.add(deviceList);
		
		this.add(centerPanel);
		
		JPanel bottomPanel = new JPanel(new FlowLayout());
		bottomPanel.add(cancel);
		bottomPanel.add(okay);
		
		
		this.add(bottomPanel);
		
		ActionEvents ae = new ActionEvents();
		okay.addActionListener(ae);
		cancel.addActionListener(ae);
		
		WindowCloser wc = new WindowCloser();
		this.addWindowListener(wc);
	}
	
	public String getResult() {
		return result;
	}
	
	public void popUp() {
		this.setVisible(true);
		this.setSize(250, 250);
	}
	
	/**
	 * Private inner class used to listen for Action Events such as button clicks.
	 * @author Sean Maloney
	 */
	private class ActionEvents implements ActionListener {

		@Override
		public void actionPerformed(ActionEvent e) {
			if(e.getSource() == okay) {
				result = (String) deviceList.getSelectedItem();
				exit();
			}
			else if(e.getSource() == cancel) {
				exit();
			}
			
		}
	}
	
	/**
	 * Makes sure the GUI closes properly.
	 * @author Sean Maloney
	 */
	private class WindowCloser extends WindowAdapter {
		/**
		 * Closes the Frame when it is done being used.
		 */
		
		@Override
		public void windowClosing(WindowEvent arg0) {
			exit();
		}
	}
	
	/**
	 * Private helper method used for exiting.
	 */
	private void exit() {
		this.setVisible(false);
		dispose();
		//System.exit(0);
	}
	
	
	
}
