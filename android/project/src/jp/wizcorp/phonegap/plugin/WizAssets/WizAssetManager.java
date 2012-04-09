/* Wizard Asset Manager
 *
 * @author Ally Ogilvie 
 * @copyright WizCorp Inc. [ Incorporated Wizards ] 2011
 * @file - wizAssetsManager.java
 * @about - Asset Manager Class for Phonegap Plugin
 *
 *
*/
package jp.wizcorp.phonegap.plugin.WizAssets;

import java.io.IOException;

import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Handler;
import android.util.Log;


public class WizAssetManager {

	// Scanner Handlers
	static Handler scannerHandler;
	static Runnable scanner;
	
	Context that;
	
	public WizAssetManager(Context context) {
		
		that = context;
		

		
		/*
	     * 	Scanner Handlers
	     */
		scannerHandler = new Handler();
        // Create runnables for posting
		scanner = new Runnable() {
            public void run() {
            	
            	
            	final JSONObject bundleMap = new JSONObject();
    			// get bundle asset from... (example - file:///android_asset/www/img/logo.jpg)
    			final String bundleBasePath = "www/assets";
    			 
    			final AssetManager mgr = that.getAssets();
    			
    			scanFiles( mgr, "", 0, bundleMap , bundleBasePath);
    			
    			
    			String test = bundleMap.toString();

    			Log.e("WizAssetsPlugin","printing assetMap ||  " + test );
            }
            
        };
        
	}
	
	
	private void scanFiles (AssetManager mgr, String path, int level, JSONObject assetmap, String bundleBasePath) {

	    try {
	    	String list[];
	    	if (path == "") {
		        list = mgr.list(bundleBasePath);
	    	} else {
		        list = mgr.list(bundleBasePath + "/" + path);
	    	}
	        //Log.d("WizAssetsPlugin"," list this mofo path || "+bundleBasePath + "/" + path+"  ("+ Arrays.asList(list) +")");
	        if (list != null) {

	            for (int i=0; i<list.length; i++) {
                    if( level>=1 ) {
                    	//Log.d("WizAssetsPlugin","writing to assetMap (>1) ||  ("+list[i]+")");
            	    	// add this to hashmap
                    	try {
	            	    	assetmap.put("/android_asset/"+path+"/"+list[i], path+"/"+list[i]);
                    	} catch (Exception e) {
                    		// fail silently
                    		Log.d("WizAssetsPlugin","WRITE FAIL 1");
                    	}
                    	scanFiles(mgr, path + "/" + list[i], level+1, assetmap, bundleBasePath);
                    } else {
                    	//Log.d("WizAssetsPlugin","writing to assetMap (0) || "+path+ " || ("+list[i]+")");
            	    	// add this to hashmap
                    	if (path == "") {
                    		scanFiles(mgr, list[i], level+1, assetmap, bundleBasePath);
                    	} else {
                    		scanFiles(mgr, path + "/" + list[i], level+1, assetmap, bundleBasePath);
                    	}
                    }
	            }
	          }
	    } catch (IOException e) {
	        Log.d("WizAssetsPlugin","List error: can't list" + path);
	    }

	}
	
	
	
	public static Object scanFiles (JSONObject assetMap) {
		
		scannerHandler.post(scanner);

		
		
		return null;
	}
	
	
}


