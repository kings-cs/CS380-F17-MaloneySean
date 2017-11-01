# CS380-F17-MaloneySean
Oh Crop Image Editor

Author: Sean Maloney
Last Updated: 09/14/2017


   *Table Of Contents*

1. Running The Program
2. Supported File Types
3. Features
4. Timing
5. Limitations


**************************

1. There are no special requirements for compilation/execution of this program. Simply run the class 'EditorGUI' in eclipse
once the project has been imported into a workspace.

2. The following file formats for images are currently supported:
	- png
	- jpg
	
	Note: Some other file formats may happen to work with this program but formats that do not
	appear on this list have not been tested.

3. Features:

   The main panel displays a default image of a crop icon at start up/after closing an image that can be modified and zoomed in on. The location
   of this file can NOT be over written through the save functions of the program.
   
   There are two Java menus contained in the menu bar at the top of the program. They are File and View.
   
   Under File:
      -Exit: This button exits the program WITHOUT saving any alterations that have been made to the image currently being viewed.
      -Open: This button will open a JFileChooser dialog that can be used to choose a new image to displayed. This will close the image
      currently being viewed.
      -Close: Closes the image currently being displayed after prompting the user if they would like to save the image.
      -Save: Saves the image and any changes made to it to the location it was opened from or from the file path designated by 'save as' if
      that function has been used since opening the current image.
      -Save As...: Opens a JFileChooser dialog to prompt the user for a new save location. The currently open image and any changes made to it
      will be saved to this new location. The file extension MUST be typed in at the end of the file path when saving in this manner.
      
      
   Under View:
   	  -Resize: Prompts the user for a height and width that will be used to change the size of the GUI window. This does NOT alter the dimensions
   	  of the image currently being viewed.
   	  -Zoom: Prompts the user to enter a level of zoom (expressed as a percentage) and zooms the image accordingly. This value can NOT exceed 
   	  500% or go below 20%.
   	  
   The program also features a Tool Bar that is initially displayed below the menu bar. This tool bar can be dragged to a new different edge 
   (excluding the bottom) of the frame or pulled away as to be  floating tool bar. In order to do so the user must click and drag the dotted 
   section at the beginning of the tool bar.
   
   The Tool Bar also contains several buttons with various features:
   
   Gray Scale: Recolors all pixels of the current image to be a shade of a gray. 
   
   Gray Scale (Parallel): Recolors all pixels of the current image to be a shade of a gray but is computer via parallelism making it much faster. 
   
   Sepia Tone: Recolors all pixels of the current image to be in Sepia Tone.
   
   Sepia Tone (Parallel): Recolors all pixels of the current image to be in Sepia Tone but is computer via parallelism making it much faster. 
   
   Blur: Blurs all pixels of the current image. 
   
   Blur (Parallel): Blurs all pixels of the current image, computed via parallel.  
   
   Mosaic: Creates a Mosaic image based off a give number of tile points.
   
   Mosaic (Parallel): Creates a Mosaic image based off a give number of tile points, computed in parallel.
      
   Histogram Equalization: Performs a Histogram Equalization on a blurry image.   
   
   Histogram Equalization (Parallel): Performs a Histogram Equalization on a blurry image, computed in parallel.
   
   Histogram Equalization (Atomic Parallel): Performs a Histogram Equalization on a blurry image, computed in parallel and uses atomic operations in the computation of the Histogram.   
      
   A separate panel at the bottom of the main window contains additional zoom controls and a device selector. Here, the current level of zoom is displayed and to the 
   left of this text are buttons labeled as + and -. The + will zoom in the picture by 10% and the - will zoom out by 10%. If the current level of
   zoom is not divisible of 10, these buttons will move to the nearest divisible number by 10 in the desired direction.
   
   If an image is too large to be displayed on the main window, scroll bars will accomodate the image.
   
   To the left of the zoom control, is a drop down box that lists all of the computation devices on the computer running the program that support parallelism via OpenCL. The user can use this drop box menu to change the device that the calculations are being made on.
  
4. Timing:

	The follow is a table of the amount of time taken to run various image editing algorithms used in the program. Time is reported in Miliseconds (ms).

<table style = "width50%">
	<tr>
		<th>Algorithm</th>
		<th>Image Size</th>
		<th>Time</th>
		<th>Computational Device</th>
	</tr>
	<tr>
		<th>Grayscale Sequential</th>
		<th>1920x1080p</th>
		<th>20.516101 (ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Grayscale Parallel</th>
		<th>1920x1080p</th>
		<th>02.611009 (ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Sepia Sequential</th>
		<th>1920x1080p</th>
		<th>25.81792 (ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Sepia Parallel</th>
		<th>1920x1080p</th>
		<th>04.34982 (ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Blur Sequential</th>
		<th>1920x1080p</th>
		<th>371.913135(ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Blur Parallel</th>
		<th>1920x1080p</th>
		<th>25.222637 (ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Mosaic Sequential</th>
		<th>1920x1080p</th>
		<th>10810.747899(ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Mosaic Parallel</th>
		<th>1920x1080p</th>
		<th>4.634023 (ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
		<tr>
		<th>Horizontal Flip Parallel</th>
		<th>1920x1080p</th>
		<th>2.431387 (ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Vertical Flip Parallel</th>
		<th>1920x1080p</th>
		<th>2.40289 (ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Rotate Right Parallel</th>
		<th>1920x1080p</th>
		<th>2.483131 (ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Rotate Left Parallel</th>
		<th>1920x1080p</th>
		<th>2.385131 (ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Grayscale Sequential</th>
		<th>1280x791p</th>
		<th>15.688924 (ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Grayscale Parallel</th>
		<th>1280x791p</th>
		<th>01.563283 (ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Sepia Sequential</th>
		<th>1280x791p</th>
		<th>20.99364 (ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Sepia Parallel</th>
		<th>1280x791p</th>
		<th>06.704361 (ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Blur Sequential</th>
		<th>1280x791p</th>
		<th>194.004677(ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Blur Parallel</th>
		<th>1280x791p</th>
		<th>13.250703 (ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
		<tr>
		<th>Mosaic Sequential</th>
		<th>1280x791p</th>
		<th>5452.02955(ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Mosaic Parallel</th>
		<th>1280x791p</th>
		<th>3.134753 (ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Horizontal Flip Parallel</th>
		<th>1280x791p</th>
		<th>1.768885 (ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Vertical Flip Parallel</th>
		<th>1280x791p</th>
		<th>1.744453 (ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Rotate Right Parallel</th>
		<th>1280x791p</th>
		<th>1.78382 (ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	<tr>
		<th>Rotate Left Parallel</th>
		<th>1280x791p</th>
		<th>1.754335 (ms)</th>
		<th>GeForce GTX 745</th>
	</tr>
	
</table>
	
5. Current Limitations: May not function for file formats not explicitly listed. Histogram Equalization only works on grayscale images.
