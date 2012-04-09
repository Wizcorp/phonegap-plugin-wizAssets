/* Wizard Asset Manager PhoneGap Plugin
 *
 * @author Ally Ogilvie 
 * @copyright WizCorp Inc. [ Incorporated Wizards ] 2011
 * @file - wizAssetsPlugin.java
 * @about - Asset Manager Plugin for Phonegap
 *
 *
*/
package jp.wizcorp.phonegap.plugin.WizAssets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Array;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import jp.wizcorp.android.shell.AndroidShellActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Environment;
import android.util.Log;

import com.phonegap.api.Plugin;
import com.phonegap.api.PluginResult;


public class WizAssetsPlugin extends Plugin {


	
	final static JSONObject bundleMap = new JSONObject();
	final static JSONObject internalMap = new JSONObject();
	final static JSONObject externalMap = new JSONObject();
	public String PGroot;
	String storagePath;
	String internalPath;
	public String separator;
	
	@SuppressWarnings("unused")
	@Override
	 public PluginResult execute(String action, JSONArray args, String callbackId) {
		
		PluginResult result = null;
		
		if (action.equals("downloadFile")) {
			
			
			// Log.d("WizAssetsPlugin", "[downloadFiles] *********** "+args.toString() );
			
			try {
				// split by "/"
				String[] splitURL = args.getString(1).split("/");
				
				// last element is name
				String fileName = splitURL[splitURL.length-1];

				// build directory
				Resources appR = ctx.getApplicationContext().getResources();
				// dir name includes game name as folder root.. ie. "ZombieJombie"
				CharSequence txt = appR.getText(appR.getIdentifier("app_name", "string", ctx.getApplicationContext().getPackageName()))+"/"; 

				String dirName = ""+txt;
				for (int i=0; i<splitURL.length-1; i++) {
					dirName = dirName+splitURL[i]+"/";
				}

				downloadUrl(args.getString(0), dirName, fileName, "true", callbackId );
			} catch (JSONException e) {
				 result = new PluginResult(PluginResult.Status.ERROR, "Param errrors");
			}
			
			result = new PluginResult(PluginResult.Status.NO_RESULT);
			result.setKeepCallback(true);
			
			return result;
			
		} else if (action.equals("getFileURI")) {
			
			Log.d("WizAssetsPlugin", "GET FILE URI *********** for "+ args.toString() );
			String returnUri = "";
			
			// get app name
			Resources appR = ctx.getApplicationContext().getResources();
			CharSequence txt = appR.getText(appR.getIdentifier("app_name", "string", ctx.getApplicationContext().getPackageName()));
			// get storage path
			String storage = ctx.getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+separator+txt+separator;
			
			String relpath = null;
			try {
				relpath = args.getString(0);
			} catch (JSONException e) {
				Log.d("WizAssetsPlugin", "[getFileURI] *********** BROKEN >>>>>>> "+e);
			}
			if (relpath == null) {
				result = new PluginResult(PluginResult.Status.ERROR, "noParam");
			} else {
				// start checking
				if (storage != null) {
					// We have storage, check in storage
					String storageUri = storage+relpath;
					File storageFile = new File(storageUri);
					if ( storageFile.isFile() ) {
						returnUri = storageUri;
					} else {
						returnUri = "/android_asset/www/assets/"+relpath;
					}
				} else {
					// create uri from bundle assets
					returnUri = "/android_asset/www/assets/"+relpath;
				}
				Log.d("WizAssetsPlugin", "GET FILE URI *********** returnUri "+returnUri );
				result = new PluginResult(PluginResult.Status.OK, returnUri);
			}
			
			
					
		} else if (action.equals("getFileURIs")) {
			// get file paths
			 
			Log.d("WizAssetsPlugin", "GET FILE URIs *********** >>>>>>> ");
			 
			Resources appR = ctx.getApplicationContext().getResources();
			CharSequence txt = appR.getText(appR.getIdentifier("app_name", "string", ctx.getApplicationContext().getPackageName())); 

			
			
			
			// get bundle asset from... (example - file:///android_asset/www/img/logo.jpg)
			final String bundleBasePath = "www/assets";
			final AssetManager aMan = ctx.getApplicationContext().getAssets();
			PGroot = "/android_asset/www/assets";
			separator = "/";
			
			try {
				scanFiles( aMan, bundleBasePath, bundleMap);
				// Log.d("WizAssetsPlugin", "bundleMap >> "+bundleMap.toString());
			} catch (Exception e) {
				Log.d("WizAssetsPlugin", "scan bundle *********** BROKEN >>>>>>> "+e);
			}
					
			
			
			
			
			
			/*
			// get internal asset from... (example - /data/data/jp.wizcorp.android.shell/files/ZombieJombie/)
			internalPath = ctx.getFilesDir() + separator + txt + separator;
			Log.d("WizAssetsPlugin", "INTERNAL DIR *********** >>>>>>> "+ internalPath);
			try {
				File manager = new File(internalPath);
				scanInternalFiles( manager, internalMap);
				Log.d("WizAssetsPlugin", "internalMap >> "+internalMap.toString());
			} catch (Exception e) {
				Log.d("WizAssetsPlugin", "scan internal *********** BROKEN >>>>>>> "+e);
			}
			*/
			
			
			
			
			// get the storage folder for this device
			Log.d("WizAssetsPlugin", "STORAGE FOLDER IS *********** >>>>>>> "+ ctx.getApplicationContext().getExternalFilesDir(null) );
			// for example /mnt/sdcard/Android/data/jp.wizcorp.android.shell/files/
			

			
			
			// externalPath = Environment.getExternalStorageDirectory().getAbsolutePath() + separator + txt + separator;
			storagePath = ctx.getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+separator+txt+separator;
			Log.d("WizAssetsPlugin", "EXTERNAL DIR *********** >>>>>>> "+ storagePath);
			
			if (storagePath != null) {
				try {
					File manager = new File(storagePath);
					scanExternalFiles( manager, externalMap);
					Log.d("WizAssetsPlugin", "externalMap >> "+externalMap.toString());
				} catch (Exception e) {
					Log.d("WizAssetsPlugin", "scan external *********** BROKEN >>>>>>> "+e);
				}
	
				// merge and return asset map as object in PluginResult
				JSONObject merged = new JSONObject();
				JSONObject[] objs = new JSONObject[] { bundleMap, externalMap };
				for (JSONObject obj : objs) {
				    Iterator it = obj.keys();
				    while (it.hasNext()) {
				        String key = (String)it.next();
				        try {
							merged.put(key, obj.get(key));
						} catch (JSONException e) {
							// TODO Auto-generated catch block
							Log.d("WizAssetsPlugin", "merge error *********** BROKEN >>>>>>> "+e);
						}
				    }
				}
				
				Log.d("WizAssetsPlugin", "merged >> "+merged.toString());
				result = new PluginResult(PluginResult.Status.OK, merged);
			} else {
				// just return bundleMap
				result = new PluginResult(PluginResult.Status.OK, bundleMap);
			}
			
		 
		} else if (action.equals("deleteFiles")) {
			
			Log.d("WizAssetsPlugin", "[deleteFiles] *********** ");
			
			try {
				for (int i = 0; i<args.length(); i++) {
					
					String filepath = args.getString(i);
					// if file is in bundle we cannot delete so ignore
					if (!filepath.contains("www/assets")) {
						// delete the file, we do not care success or fail
						// if you want to check file is deleted, delete() returns boolean 
						// so you can implement a check after delete() if necessary
						File file = new File(filepath);
						file.delete();
						
						Log.d("WizAssetsPlugin", "trying to delete >>>>>>> "+filepath);
					}
				}
			} catch (JSONException e) {
				Log.d("WizAssetsPlugin", "failed to parse download strings *********** "+e);
			}
			
			// again, we do not care about success or fail...
			result = new PluginResult(PluginResult.Status.OK);	
		} else {
			result = new PluginResult(PluginResult.Status.INVALID_ACTION);
		}
	 
		return result;
	}
	
	

	private void scanFiles (AssetManager mgr, String path, JSONObject results) throws JSONException, IOException {
		String list[];
		String filepath;
		list = mgr.list(path);

		if (list == null) {
			return;
		}
		for (int i = 0; i < list.length; i++) {
			filepath = path + "/" + list[i];

			if (list[i].contains(".")) {
				// consider it a file, not a directory
				// remove the www/assets
				filepath = filepath.replace("www/assets", "");
				// remove first slash for game assetMap
				String gamePath = filepath.substring(1);
				results.put(gamePath, PGroot+filepath);
			} else {
				// consider it a directory
				scanFiles(mgr, filepath, results);
			}
		}
	}

	private void scanExternalFiles (File mgr, JSONObject results) throws JSONException, IOException {
		String[] list;
		String filepath;
		list = mgr.list();

		if (list == null || list.length == 0) {
			return;
		} else {
			for (int i = 0; i < list.length; i++) {
				filepath = mgr.getAbsolutePath()+"/"+list[i];
				if ( list[i].contains(".") ) {
					// consider it a file, not a directory
					// create gamepath
					String gamePath = filepath.replace(storagePath, "");
					results.put(gamePath, filepath);
				} else {
					// consider it a directory
					File newDir = new File(filepath);
					scanExternalFiles(newDir, results);
				}
			}
		}
		
	}
	
	
	/*
	private void scanInternalFiles (File mgr, JSONObject results) throws JSONException, IOException {
		String[] list;
		String filepath;
		list = mgr.list();

		if (list == null || list.length == 0) {
			return;
		} else {
			for (int i = 0; i < list.length; i++) {
				filepath = mgr.getAbsolutePath()+"/"+list[i];
				if ( list[i].contains(".") ) {
					Log.d("WizAssetsPlugin", "[scanFiles] *********** FILE filepath "+filepath);
					// consider it a file, not a directory
					// create gamepath
					String gamePath = filepath.replace("internalPath", "");
					results.put(filepath, gamePath);
				} else {
					// consider it a directory
					Log.d("WizAssetsPlugin", "[scanFiles] *********** DIR filepath "+filepath);
					File newDir = new File(filepath);
					scanExternalFiles(newDir, results);
				}
			}
		}
		
	}
	*/
	
	

	 private void downloadUrl(String fileUrl, String dirName, String fileName, String overwrite, String callbackId){
		 // download files to sdcard, or phone if sdcard not exists
		 PluginResult result;
		 try {
			 
			 String pathTostorage = ctx.getApplicationContext().getExternalFilesDir(null).getAbsolutePath()+separator;
			 File dir = new File(pathTostorage+dirName);
			 if(!dir.exists()) {
				 // create the directory is not existing
				 // Log.d("WizAssetsPlugin", "directory /sdcard/"+dirName+" created");
				 dir.mkdirs();
			 }
	 
			 File file = new File(pathTostorage+dirName+"/"+fileName);
			 // Log.d("WizAssetsPlugin", "[downloadUrl] *********** pathTostorage pathTostorage+dirName+fileName > "+file.getAbsolutePath() );

	 
			 if(overwrite.equals("false") && file.exists()){
				 Log.d("WizAssetsPlugin", "File already exist");
				 result = new PluginResult(PluginResult.Status.OK, "exist");
			 }
	 
			 URL url = new URL(fileUrl);
			 HttpURLConnection ucon = (HttpURLConnection) url.openConnection();
			 ucon.setRequestMethod("GET");
			 ucon.setDoOutput(true);
			 ucon.connect();
		 
			 Log.d("WizAssetsPlugin", "[downloadUrl] :" + url);
			 
			 InputStream is = ucon.getInputStream();
			 
			 byte[] buffer = new byte[1024];
			 
			 int len1 = 0;
		 
		 		FileOutputStream fos = new FileOutputStream(file);
		 
			 while ( (len1 = is.read(buffer)) > 0 ) {
				 fos.write(buffer,0, len1);
			 }
		 
			 fos.close();
			 
			 // Log.d("WizAssetsPlugin", "Download complete > " + fileName);
		 
			 result = new PluginResult(PluginResult.Status.OK, file.getAbsolutePath() );
			 
		 } catch (IOException e) {
		 
			 Log.d("WizAssetsPlugin", "Error: " + e);
			 // ignore error
		 	 result = new PluginResult(PluginResult.Status.OK, "/android_asset/"+dirName+"/"+fileName);
		 
		 }
	 
		 
		 success(result, callbackId);
		 
 
	}	
	
	
	
}
