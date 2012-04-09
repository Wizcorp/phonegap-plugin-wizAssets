


# PLUGIN: 

phonegap-plugin-wizAssets



# DESCRIPTION :

PhoneGap plugin for managing application assets with javascript asset maps. Includes( iOS background threaded) downloadFile, getFileURI, getFileURIs, deleteFile.





# INSTALL (iOS): #

Project tree<br />

<pre><code>
project
	/ www
		-index.html
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

2 ) Add to phonegap.plist in the plugins array;<br />
Key : WizAssetsPlugin<br />
Type : String<br />
Value : WizAssetsPlugin<br />

3 ) Add \<script\> tag to your index.html<br />
\<script type="text/javascript" charset="utf-8" src="phonegap/plugin/wizAssets/wizAssets.js"\>\</script\><br />
(assuming your index.html is setup like tree above)


4 ) Follow example code below.






# INSTALL (Android BETA!): #

Project tree<br />

<pre><code>
project
	/ assets
		/ www
			-index.html
			/ assets [store your app assets here]
			/ phonegap
				/js
					/ phonegap.js
				/ plugin
					/ wizAssets
						/ wizAssets.js	
	/ src
		/ jp.wizcorp.phonegap.plugin.WizAssets
			/ WizAssetManager.java
			/ WizAssetPlugin.java
</code></pre>



1 ) Arrange files to structure seen above.



2 ) Remember to add the plugin to plugins.xml


3 ) Add \<script\> tag to your index.html<br />
\<script type="text/javascript" charset="utf-8" src="phonegap/plugin/wizAssets/wizAssets.js"\>\</script\><br />
(assuming your index.html is setup like tree above)


Add something like the following to your main Activity.
<pre><code>
wizAman = new WizAssetManager(this.getApplicationContext() );
</pre></code>



<br />
<br />
<br />
# EXAMPLE CODE : #

<br />
<br />
Get all existing assets in the binary and other external folders (if existing)<br />

wizAssets.downloadFile(String URL, String filePathToBeStoredWithFilename, Function success, Function fail);

    * downloads a file to native App directory @ ./ + gameDir+ / +filePathToBeStoredWithFilename
    * A success returns URI string like; file://documents/settings/img/cards/card001.jpg
    * example; 
<pre><code>
{

    wizAssets.downloadFile("http://google.com/logo.jpg" , "img/ui/logo.jpg", function(e){ alert("success "+e) } , function(e){ alert("fail "+e) } ); 

}
</code></pre>

wizAssets.deleteFiles(Array manyURIs , Function success, Function fail );

    * delete all URIs in Array like; [ "file://documents/settings/img/cards/card001.jpg" , "file://documents/settings/img/cards/card002.jpg " .. ]
    * if you do specify a filename only dir, then all contents of dir will be deleted; file://documents/settings/img/cards
    * the array CAN contain one URI string 



wizAssets.getFileURI(String filePathWithFilename, Function success, Function fail);

    * A success returns URI string like file://documents/settings/img/cards/card001.jpg
    * example; 
<pre><code>
{

    wizAssets.getFileURI("img/ui/logo.jpg" , function(e){ alert("success "+e) } , function(e){ alert("fail "+e) } ); 

}
</code></pre>

wizAssets.getFileURIs(Function success, Function fail);

    * A success returns URI hashmap such as 
<pre><code>
{

    img/ui/loader.gif  : "/sdcard/<appname>/img/ui/loading.gif", 
    img/cards/card001.jpg : "file://documents/settings/img/cards/card001.jpg" 

} 
</code></pre>