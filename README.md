

# PLUGIN: 

phonegap-plugin-wizAssets<br />
version : 2.3<br />
last update : 23/01/2013<br />


# CHANGELOG: 
<br />
- Updated for Cordova 2.3
- Updated for Cordova 1.9
- Changed deleteFile error handling.
- Removed unimplemented plugin methods.
- Updated error handling to more closely follow the w3c file api error
  conventions http://www.w3.org/TR/FileAPI/


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

    wizAssets.downloadFile("http://google.com/logo.jpg", "img/ui/logo.jpg", successCallback, failCallback );

}
</code></pre>

wizAssets.deleteFile(string URI, Function success, Function fail);
<br />
    * deletes the file specified by the URI <br />
    * if the URI does not exist fail will be called with error NotFoundError <br />
    * if the URI cannot be deleted (i.e. file resides in read-only memory) fail will be called with error NotModificationAllowedError <br />
<pre><code>
{
    wizAssets.deleteFile("file://documents/settings/img/cards/card001.jpg", successCallback, failCallback);
}
</code></pre>

wizAssets.deleteFiles(Array manyURIs, Function success, Function fail );
<br />
    * delete all URIs in Array like; [ "file://documents/settings/img/cards/card001.jpg", "file://documents/settings/img/cards/card002.jpg " .. ] <br />
    * if you do not specify a filename only dir, then all contents of dir will be deleted; file://documents/settings/img/cards <br />
    * the array CAN contain one URI string  <br />



wizAssets.getFileURI(String filePathWithFilename, Function success, Function fail);
<br />
    * A success returns URI string like file://documents/settings/img/cards/card001.jpg <br />
    * example;  <br />
<pre><code>
{

    wizAssets.getFileURI("img/ui/logo.jpg", successCallback, failCallback );

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
