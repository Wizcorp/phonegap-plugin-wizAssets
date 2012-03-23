/* Download and show, PhoneGap Example
 *
 * @author WizCorp Inc. [ Incorporated Wizards ] 
 * @copyright 2011
 * @file - wizAssets.js
 * @about - JavaScript download and update asset example for PhoneGap
 *
 *
 */


var wizAssets = { 
    
    
	downloadFile: function(a, b, s, f) {
	    
	    window.setTimeout(function () {
	                      PhoneGap.exec(s, f, "WizAssetsPlugin", "downloadFile", [a, b]);
	                      }, 0);
	},
	    
	deleteFile: function(a, s, f) {
	    
	    return PhoneGap.exec(s, f, "WizAssetsPlugin", "deleteFile", [a]);
	    
	},
	    
	    
	deleteFiles: function(a, s, f) {
	    
	    return PhoneGap.exec(s, f, "WizAssetsPlugin", "deleteFiles", a );
	    
	},
	    
	    
	getFileURIs: function(s, f) {
	    
	    return PhoneGap.exec(s, f, "WizAssetsPlugin", "getFileURIs", [] );
	    
	},
	    
	    
	getFileURI: function(a, s, f) {
	    
	    return PhoneGap.exec(s, f, "WizAssetsPlugin", "getFileURI", [a] );
	    
	},
	    
	createProgressBar: function(s, f) {
	    
	    return PhoneGap.exec(s, f, "WizAssetsPlugin", "createProgressBar", [] );
	    
	},
	    
	removeProgressBar: function() {
	    
	    return PhoneGap.exec(null, null, "WizAssetsPlugin", "removeProgressBar", [] );
	    
	},
	    
	purgeEmptyDirectories: function(s, f) {
	    // TODO: implement this function
	    s();
	    
	    
	    //return PhoneGap.exec(null, null, "WizAssetsPlugin", "removeProgressBar", [] );
	    
	}
    
    
};
