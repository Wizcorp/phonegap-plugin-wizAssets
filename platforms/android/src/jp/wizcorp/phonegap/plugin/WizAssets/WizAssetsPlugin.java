/*
 *  __    __ _                  _     _                _                                                  ___ _             _
 * / / /\ \ (_)______ _ _ __ __| |   /_\  ___ ___  ___| |_    /\/\   __ _ _ __   __ _  __ _  ___ _ __    / _ \ |_   _  __ _(_)_ __
 * \ \/  \/ / |_  / _` | '__/ _` |  //_\\/ __/ __|/ _ \ __|  /    \ / _` | '_ \ / _` |/ _` |/ _ \ '__|  / /_)/ | | | |/ _` | | '_ \
 *  \  /\  /| |/ / (_| | | | (_| | /  _  \__ \__ \  __/ |_  / /\/\ \ (_| | | | | (_| | (_| |  __/ |    / ___/| | |_| | (_| | | | | |
 *   \/  \/ |_/___\__,_|_|  \__,_| \_/ \_/___/___/\___|\__| \/    \/\__,_|_| |_|\__,_|\__, |\___|_|    \/    |_|\__,_|\__, |_|_| |_|
 *                                                                                    |___/                           |___/
 * @author  Ally Ogilvie
 * @copyright Wizcorp Inc. [ Incorporated Wizards ] 2014
 * @file    - wizAssetManagerPlugin.java
 * @about   - Handle JavaScript API calls from PhoneGap to WizAssetsPlugin
 */

package jp.wizcorp.phonegap.plugin.WizAssets;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.zip.GZIPInputStream;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.AsyncTask;
import android.os.Build;
import android.util.Base64;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.PluginResult;
import org.apache.http.Header;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.entity.BufferedHttpEntity;
import org.apache.http.impl.client.DefaultHttpClient;

public class WizAssetsPlugin extends CordovaPlugin {

    private String TAG = "WizAssetsPlugin";
    private WizAssetManager wizAssetManager = null;

    private static final String DOWNLOAD_FILE_ACTION = "downloadFile";
    private static final String GET_FILE_URI_ACTION = "getFileURI";
    private static final String GET_FILE_URIS_ACTION = "getFileURIs";
    private static final String DELETE_FILE_ACTION = "deleteFile";
    private static final String DELETE_FILES_ACTION = "deleteFiles";

    private String pathToStorage;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        final Context applicationContext = cordova.getActivity().getApplicationContext();
        pathToStorage = applicationContext.getCacheDir().getAbsolutePath() + File.separator;
        wizAssetManager = new WizAssetManager(applicationContext);
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (action.equals(DOWNLOAD_FILE_ACTION)) {
            final String url = args.getString(0);
            final String uri = args.getString(1);
            final CallbackContext _callbackContext = callbackContext;
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    String filePathInDB = getAssetFilePathFromUri(uri);
                    String filePath = buildAssetFilePathFromUri(uri);
                    File file = new File(filePath);
                    if (filePathInDB != null && file.exists()) {
                        // File is already in cache folder, don't download it
                        Log.d(TAG, "[Is in cache] " + uri);
                        String result = "file://" + file.getAbsolutePath();
                        _callbackContext.success(result);
                    } else {
                        // File is not in cache folder, download it
                        PluginResult result = new PluginResult(PluginResult.Status.NO_RESULT);
                        result.setKeepCallback(true);
                        _callbackContext.sendPluginResult(result);
                        downloadUrl(url, uri, filePath, _callbackContext);
                    }
                }
            });
            return true;

        } else if (action.equals(GET_FILE_URI_ACTION)) {

            Log.d(TAG, "[getFileURI] search full file path for: "+ args.toString() );
            String asset = null;

            try {
                String relativePath = args.getString(0);
                asset = getAssetFilePathFromUri(relativePath);
            } catch (JSONException e) {
                Log.d(TAG, "[getFileURI] error: " + e.toString());
                callbackContext.error(e.toString());
            }

            if (asset == null) {
                callbackContext.error("NotFoundError");
            } else {
                Log.d(TAG, "[getFileURI] Returning full path: " + asset);
                callbackContext.success(asset);
            }
            return true;

        } else if (action.equals(GET_FILE_URIS_ACTION)) {

            // Return all assets as asset map object
            Log.d(TAG, "[getFileURIs] *********** >>>>>>> ");
            JSONObject assetObject = wizAssetManager.getAllAssets();
            Log.d(TAG, "[getFileURIs] RETURN *********** >>>>>>> " + assetObject.toString());
            callbackContext.success(assetObject);
            return true;

        } else if (action.equals(DELETE_FILES_ACTION)) {

            // Delete all files from given array
            Log.d(TAG, "[deleteFiles] *********** ");
            deleteAssets(args, false, new DeleteAssetsCallback(callbackContext));

            return true;

        } else if (action.equals(DELETE_FILE_ACTION)) {

            Log.d(TAG, "[deleteFile] *********** " + args.getString(0));
            String filePath = args.getString(0);
            try {
                deleteAsset(filePath, false);
            } catch (IOException e) {
                callbackContext.error("Deleting file failed.");
                return true;
            }

            // Callback success for any outcome.
            callbackContext.success();
            return true;
        }

        return false;  // Returning false results in a "MethodNotFound" error.
    }

    private String getAssetFilePathFromUri(String file) {
        String asset = wizAssetManager.getFile(file);
        if (asset == null || asset == "" || asset.contains("NotFoundError")) {
            return null;
        }

        return asset;
    }

    private String buildAssetFilePathFromUri(String uri) {
        return pathToStorage + uri;
    }

    private void deleteAssets(JSONArray files, boolean isUri, DeleteAssetsCallback callback) {
        AsyncDelete asyncDelete = new AsyncDelete(callback, isUri);
        asyncDelete.execute(files);
    }

    private void deleteAsset(String filePath, boolean isUri) throws IOException {
        // If file is in bundle we cannot delete so ignore and protect whole cache folder from being deleted
        if (filePath != "" && !filePath.contains("www/assets")) {
            if (isUri) {
                filePath = buildAssetFilePathFromUri(filePath);
            }
            File file = new File(filePath);
            boolean isDirectory = file.isDirectory();
            boolean deleteSucceed = deleteFile(file);
            if (!deleteSucceed) {
                throw new IOException(filePath + " could not be deleted.");
            }
            // Delete from database
            if (isDirectory) {
                wizAssetManager.deleteFolder(filePath);
            } else {
                wizAssetManager.deleteFile(filePath);
            }
        }
    }

    private boolean deleteFile(File file) {
        boolean deleteSucceed = true;
        if (file.isDirectory()) {
            String files[] = file.list();
            for (String temp : files) {
                File fileDelete = new File(file, temp);
                deleteSucceed = deleteFile(fileDelete) && deleteSucceed;
            }
        }
        deleteSucceed = file.delete() && deleteSucceed;
        return deleteSucceed;
    }

    @SuppressLint("NewApi")
	private void downloadUrl(String fileUrl, String uri, String filePath, CallbackContext callbackContext){
        // Download files to sdcard, or phone if sdcard not exists
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
            new AsyncDownload(fileUrl, uri, filePath, callbackContext).executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR);
        } else {
            new AsyncDownload(fileUrl, uri, filePath, callbackContext).execute();
        }
    }

    public class DeleteAssetsCallback {
        private CallbackContext callbackContext;

        public DeleteAssetsCallback(CallbackContext callbackContext) {
            this.callbackContext = callbackContext;
        }

        public void notify(int result) {
            if (result < AsyncDelete.NO_ERROR) {
                this.callbackContext.error(getErrorMessage(result));
                return;
            }
            this.callbackContext.success();
        }

        public String getErrorMessage(int errorCode) {
            switch (errorCode) {
            case AsyncDelete.JSON_TYPE_ERROR:
                return AsyncDelete.JSON_TYPE_ERROR_MESSAGE;
            case AsyncDelete.IO_ERROR:
                return AsyncDelete.IO_ERROR_MESSAGE;
            case AsyncDelete.DELETE_CANCELED_ERROR:
                return AsyncDelete.DELETE_CANCELED_ERROR_MESSAGE;
            case AsyncDelete.DELETE_PARAMETERS_LENGTH_ERROR:
                return AsyncDelete.DELETE_PARAMETERS_LENGTH_ERROR_MESSAGE;
            case AsyncDelete.CALLBACK_ERROR:
                return AsyncDelete.CALLBACK_ERROR_MESSAGE;
            default:
                return null;
            }
        }
    }

    private class AsyncDelete extends AsyncTask<JSONArray, Integer, Integer> {
        private DeleteAssetsCallback callback;
        private boolean isUri;

        private static final int NO_ERROR = 0;
        private static final int JSON_TYPE_ERROR = -1;
        private static final int IO_ERROR = -2;
        private static final int DELETE_CANCELED_ERROR = -3;
        private static final int DELETE_PARAMETERS_LENGTH_ERROR = -4;
        private static final int CALLBACK_ERROR = -5;

        private static final String JSON_TYPE_ERROR_MESSAGE = "Wrong parameters type.";
        private static final String IO_ERROR_MESSAGE = "Deleting files failed.";
        private static final String DELETE_CANCELED_ERROR_MESSAGE = "Deleting files canceled.";
        private static final String DELETE_PARAMETERS_LENGTH_ERROR_MESSAGE = "Number of parameters different than expected.";
        private static final String CALLBACK_ERROR_MESSAGE = "Call to delete callback failed.";

        // Constructor
        public AsyncDelete(DeleteAssetsCallback callback, boolean isUri) {
            this.callback = callback;
            this.isUri = isUri;
        }

        protected Integer doInBackground(JSONArray... jsonArrays) {
            int count = jsonArrays.length;
            if (count != 1) {
                return DELETE_PARAMETERS_LENGTH_ERROR;
            }

            // We only process one array, no more than one JSON array should be passed
            JSONArray jsonArray = jsonArrays[0];
            int countFiles = jsonArray.length();
            for (int i = 0; i < countFiles; i++) {
                try {
                    deleteAsset(jsonArray.getString(i), isUri);
                } catch (JSONException e) {
                    return JSON_TYPE_ERROR;
                } catch (IOException e) {
                    return IO_ERROR;
                }

                // Escape early if cancel() is called
                if (isCancelled()) {
                    return DELETE_CANCELED_ERROR;
                }
            }

            return NO_ERROR;
        }

        protected void onPostExecute(Integer result) {
            callback.notify(result);
        }
    }

    private class AsyncDownload extends AsyncTask<File, String , String> {

        private String url;
        private String uri;
        private String filePath;
        private CallbackContext callbackContext;

        // Constructor
        public AsyncDownload(String url, String uri, String filePath, CallbackContext callbackContext) {
            // Assign class vars
            this.url = url;
            this.uri = uri;
            this.filePath = filePath;
            this.callbackContext = callbackContext;
        }

        @Override
        protected String doInBackground(File... params) {
            File file = new File(this.filePath);

            // Run async download task
            File dir = file.getParentFile();
            if (dir != null && !dir.exists()) {
                // Create the directory if not existing
                dir.mkdirs();
            }

            Log.d(TAG, "[Downloading] " + file.getAbsolutePath());

            try {
                URL url = new URL(this.url);
                HttpGet httpRequest = null;
                httpRequest = new HttpGet(url.toURI());

                HttpClient httpclient = new DefaultHttpClient();

                // Credential check
                String credentials = url.getUserInfo();
                if (credentials != null) {
                    // Add Basic Authentication header
                    httpRequest.setHeader("Authorization", "Basic " + Base64.encodeToString(credentials.getBytes(), Base64.NO_WRAP));
                }

                HttpResponse response = httpclient.execute(httpRequest);
                HttpEntity entity = response.getEntity();

                InputStream is;

                Header contentHeader = entity.getContentEncoding();
                if (contentHeader != null) {
                    if (contentHeader.getValue().contains("gzip")) {
                        Log.d(TAG, "GGGGGGGGGZIIIIIPPPPPED!");
                        is = new GZIPInputStream(entity.getContent());
                    } else {
                        BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
                        is = bufHttpEntity.getContent();
                    }
                } else {
                    BufferedHttpEntity bufHttpEntity = new BufferedHttpEntity(entity);
                    is = bufHttpEntity.getContent();
                }
                byte[] buffer = new byte[1024];

                int len1 = 0;

                FileOutputStream fos = new FileOutputStream(file);

                while ((len1 = is.read(buffer)) > 0 ) {
                    fos.write(buffer,0, len1);
                }

                fos.close();
                is.close();

                // Tell Asset Manager to register this download to asset database
                String fileAbsolutePath = file.getAbsolutePath();
                Log.d(TAG, "[Downloaded ] " + fileAbsolutePath);
                wizAssetManager.downloadedAsset(this.uri, fileAbsolutePath);

                this.callbackContext.success("file://" + fileAbsolutePath);
            } catch (MalformedURLException e) {
                Log.e("WizAssetsPlugin", "Bad url : ", e);
                this.callbackContext.error("notFoundError");
            } catch (Exception e) {
                Log.e("WizAssetsPlugin", "Error : " + e);
                e.printStackTrace();
                this.callbackContext.error("unknownError");
            }
            return null;
        }
    }
}

