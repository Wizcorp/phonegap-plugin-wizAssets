

## phonegap-plugin-wizAssets 

cordova version : 2.6<br />
last update : 13/05/2013<br />

### Description :

PhoneGap plugin for managing application assets with javascript asset maps. Includes( iOS background threaded) downloadFile, getFileURI, getFileURIs, deleteFile.

### Changelog: 

- Updated for Cordova 2.6
- Updated for Cordova 2.5
- Updated for Cordova 2.4
- Updated for Cordova 2.3
- Changed deleteFile error handling.
- Removed unimplemented plugin methods.
- Updated error handling to more closely follow the w3c file api error
  conventions http://www.w3.org/TR/FileAPI/
- Added Android version.

### Install (iOS):

Project tree<br />

```
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
```

1 ) Arrange files to structure seen above.

2 ) Add to cordova.plist in the plugins array;<br />
Key : WizAssetsPlugin<br />
Type : String<br />
Value : WizAssetsPlugin<br />

3 ) Add \<script\> tag to your index.html

```<script type="text/javascript" charset="utf-8" src="phonegap/plugin/wizAssets/wizAssets.js"></script>```

^ assuming your index.html is setup like tree above


4 ) Follow example code below...

### Install (Android):

Project tree<br />

```
project
	/ assets
		/ www
			-cordova-2.6.0.js
			-index.html
			/ assets [store your app assets here]
			/ phonegap
				/ plugin
					/ wizAssets
						-wizAssets.js
						-assets.db [list your bundled assets here]
	/ src
		/ jp.wizcorp.phonegap.plugin.WizAssets
			-WizAssetManager.java
			-WizAssetsPlugin.java
```
1 ) Arrange source files to structure seen above.

2 ) Add to res/xml/config.xml the following line;
```<plugin name="WizAssetsPlugin" value="jp.wizcorp.phonegap.plugin.WizAssets.WizAssetsPlugin"/>```

3 ) Add \<script\> tag to your index.html
```<script type="text/javascript" charset="utf-8" src="phonegap/plugin/wizAssets/wizAssets.js"></script>```
(assuming your index.html is setup like tree above)

4 ) Follow example code below...

### API
#### downloadFile()
wizAssets.downloadFile(String URL, String filePathToBeStoredWithFilename, Function success, Function fail);

- downloads a file to native App directory @ ./ + gameDir+ / +filePathToBeStoredWithFilename <br />
- A success returns URI string like; file://documents/settings/img/cards/card001.jpg <br />
- example;

``` 
wizAssets.downloadFile("http://google.com/logo.jpg", "img/ui/logo.jpg", successCallback, failCallback);
```
#### deleteFile()
wizAssets.deleteFile(string URI, Function success, Function fail);

- deletes the file specified by the URI <br />
- if the URI does not exist fail will be called with error NotFoundError <br />
- if the URI cannot be deleted (i.e. file resides in read-only memory) fail will be called with error NotModificationAllowedError

```
wizAssets.deleteFile("file://documents/settings/img/cards/card001.jpg", successCallback, failCallback);
```
#### deleteFiles()
wizAssets.deleteFiles(Array manyURIs, Function success, Function fail );

- delete all URIs in Array like; [ "file://documents/settings/img/cards/card001.jpg", "file://documents/settings/img/cards/card002.jpg " .. ] <br />
- if you do not specify a filename only dir, then all contents of dir will be deleted; file://documents/settings/img/cards <br />
- the array CAN contain one URI string

#### getFileURI()
wizAssets.getFileURI(String filePathWithFilename, Function success, Function fail);

- A success returns URI string like file://documents/settings/img/cards/card001.jpg <br />
- example;

```
wizAssets.getFileURI("img/ui/logo.jpg", successCallback, failCallback);
```
#### getFileURIs()
wizAssets.getFileURIs(Function success, Function fail);
- A success returns URI hashmap such as

```
{

    "img/ui/loader.gif"  : "/sdcard/<appname>/img/ui/loading.gif", 
    "img/cards/card001.jpg" : "file://documents/settings/img/cards/card001.jpg" 

} 
```
