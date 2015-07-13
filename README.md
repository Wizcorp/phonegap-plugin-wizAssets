# phonegap-plugin-wizAssets 

- PhoneGap Version : 3.0
- last update : 15/11/2013

# Description

PhoneGap plugin for managing application assets with javascript asset maps. Includes (iOS background threaded) downloadFile, getFileURI, getFileURIs, deleteFile, deleteFiles.


## Install (with Plugman) 

	cordova plugin add https://github.com/Wizcorp/phonegap-plugin-wizAssets
	cordova build ios
	
	< or >
	
	phonegap local plugin add https://github.com/Wizcorp/phonegap-plugin-wizAssets
	phonegap build ios



## APIs

### downloadFile()

**wizAssets.downloadFile(String remoteURL, String assetId, Function success, Function fail);**

- downloads a file to native App directory @ ./ + gameDir+ / +filePathToBeStoredWithFilename <br />
- A success returns a local URL string like; file://documents/settings/img/cards/card001.jpg <br />
- example;

``` 
wizAssets.downloadFile("http://google.com/logo.jpg", "img/ui/logo.jpg", successCallback, failCallback);
```

### deleteFile()

**wizAssets.deleteFile(string assetId, Function success, Function fail);**

- deletes the file specified by the asset id <br />
- if the asset id does not exist fail will be called with error NotFoundError <br />
- if the asset id cannot be deleted (i.e. file resides in read-only memory) fail will be called with error NotModificationAllowedError

```
wizAssets.deleteFile("file://documents/settings/img/cards/card001.jpg", successCallback, failCallback);
```

### deleteFiles()

**wizAssets.deleteFiles(Array assetIds, Function success, Function fail );**

- delete files specified by their asset id in Array like; [ "img/cards/card001.jpg", "img/cards/card002.jpg " .. ] <br />
- if an asset id uses a path format and matches a folder, then the folder content will be deleted; img/cards <br />
- the array CAN contain one asset id

### getFileURI()

**wizAssets.getFileURI(String assetId, Function success, Function fail);**

- A success returns a local URL string like file://documents/settings/img/cards/card001.jpg <br />
- example;

```
wizAssets.getFileURI("img/ui/logo.jpg", successCallback, failCallback);
```

### getFileURIs()

**wizAssets.getFileURIs(Function success, Function fail);**

- A success returns a hashmap of asset id matching its local URL such as

```
{

    "img/ui/loader.gif"  : "/sdcard/<appname>/img/ui/loading.gif", 
    "img/cards/card001.jpg" : "file://documents/settings/img/cards/card001.jpg" 

} 
```
