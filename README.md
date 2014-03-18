temp
====
To include this tool in your app you need to copy the resource files with the "request_" prefix.

Also copy:

src/
	de.jamoo.appgetter/
		RequestActivity.java
		
		helpers/
			AppInfo.java
			SquareGridLayout.java	

Files to look into:
res/
	layout/
		request_item_grid.xml


assets folder
manifest.xml
-----------

Format of appfilter.xml
You should use the format that is given from Nova Launcher when you long press and tap on "Edit".
-----------

Libraries
You need to import the android support library. Right click on your project -> Android Tool -> Add support Library

Enjoy!

Alex Besler