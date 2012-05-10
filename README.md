

# PLUGIN: 

phonegap-plugin-wizAssets<br />
version : 1.7<br />
last update : 10/05/2012<br />


# CHANGELOG: 
<br />
- Updated for Cordova 1.7


# KNOWN ISSUES:
<br />
n/a

# DESCRIPTION :

PhoneGap plugin for managing application assets with javascript asset maps. Includes( iOS background threaded) downloadFile, getFileURI, getFileURIs, deleteFile.





# INSTALL (iOS): #

Project tree<br />

<pre><code>
project
	/ www
		-index.html
		/ assets [store your app assets here]
		/ phonegap
			/ plugin
				/ wizAssets
					/ wizAssets.js	
	/ Classes
	/ Plugins
		/ WizAssetsPlugin
			/ WizAssetsPlugin.h
			/ WizAssetsPlugin.m
	-project.xcodeproj
</code></pre>



1 ) Arrange files to structure seen above.

2 ) Add to cordova.plist in the plugins array;<br />
Key : WizAssetsPlugin<br />
Type : String<br />
Value : WizAssetsPlugin<br />

3 ) Add \<script\> tag to your index.html<br />
\<script type="text/javascript" charset="utf-8" src="phonegap/plugin/wizAssets/wizAssets.js"\>\</script\><br />
(assuming your index.html is setup like tree above)


4 ) Follow example code below.






# INSTALL (Android): #

coming soon...



<br />
<br />
<br />

# EXAMPLE CODE #

<br />
<br />
Get all existing assets in the binary and other external folders (if existing)<br />

wizAssets.downloadFile(String URL, String filePathToBeStoredWithFilename, Function success, Function fail);
<br />
    * downloads a file to native App directory @ ./ + gameDir+ / +filePathToBeStoredWithFilename <br />
    * A success returns URI string like; file://documents/settings/img/cards/card001.jpg <br />
    * example;  <br />
<pre><code>
{

    wizAssets.downloadFile("http://google.com/logo.jpg" , "img/ui/logo.jpg", function(e){ alert("success "+e) } , function(e){ alert("fail "+e) } ); 

}
</code></pre>

wizAssets.deleteFiles(Array manyURIs , Function success, Function fail );
<br />
    * delete all URIs in Array like; [ "file://documents/settings/img/cards/card001.jpg" , "file://documents/settings/img/cards/card002.jpg " .. ] <br />
    * if you do specify a filename only dir, then all contents of dir will be deleted; file://documents/settings/img/cards <br />
    * the array CAN contain one URI string  <br />



wizAssets.getFileURI(String filePathWithFilename, Function success, Function fail);
<br />
    * A success returns URI string like file://documents/settings/img/cards/card001.jpg <br />
    * example;  <br />
<pre><code>
{

    wizAssets.getFileURI("img/ui/logo.jpg" , function(e){ alert("success "+e) } , function(e){ alert("fail "+e) } ); 

}
</code></pre>

wizAssets.getFileURIs(Function success, Function fail);
<br />
    * A success returns URI hashmap such as  <br />
<pre><code>
{

    img/ui/loader.gif  : "/sdcard/<appname>/img/ui/loading.gif", 
    img/cards/card001.jpg : "file://documents/settings/img/cards/card001.jpg" 

} 
</code></pre>