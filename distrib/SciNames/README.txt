SciNames/0.2
============

Please report any bugs you find to https://github.com/gaurav/scinames

This distribution includes a Windows application (SciNames-0.2.exe) and a macOS application (SciNames-0.2.app).
You may need to install Java from http://www.oracle.com/technetwork/java/javase/downloads/jre9-downloads-3848532.html

On Macs, you will have to give the application permission to run without being signed. Try right-clicking on it
and clicking "Open". Both Windows and Mac application will default to using a maximum of 6 GB.

If all else fails, you can run the JAR file directly using:

  java -Xmx12G -jar SciNames-0.2.jar

This will use a maximum of 12 GB of memory; please adjust this for larger datasets.

The 'examples/' folder contains a set of files that SciNames can open. Try opening them from within SciNames, using
the menu item 'File' -> 'Load project from file ...'. You can then examine the datasets by double-clicking on the 
dataset row, or examine the changes by right-clicking the row and selecting 'View Changes'.
