Oh Crop Image Editor

Author: Sean Maloney
Last Updated: 09/05/2017


   *Table Of Contents*

1. Running The Program
2. Supported File Types
3. Features



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
      will be saved to this new location. 
      
      
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
         
   A separate panel at the bottom of the main window contains additional zoom controls. Here, the current level of zoom is displayed and to the 
   left of this text are buttons labeled as + and -. The + will zoom in the picture by 10% and the - will zoom out by 10%. If the current level of
   zoom is not divisible of 10, these buttons will move to the nearest divisible number by 10 in the desired direction.
   
   If an image is too large to be displayed on the main window, scroll bars will accomodate the image.
  