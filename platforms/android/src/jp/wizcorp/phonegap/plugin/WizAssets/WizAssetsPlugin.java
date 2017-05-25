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
import java.net.URISyntaxException;
import java.net.URL;
import java.util.zip.GZIPInputStream;

import java.net.HttpURLConnection;
import java.io.BufferedInputStream;

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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class WizAssetsPlugin extends CordovaPlugin {

    private String TAG = "WizAssetsPlugin";
    private boolean initialized = false;

    public static final String PLUGIN_FOLDER = "wizAssets";
    public static final String ASSETS_FOLDER = "assets";

    private static final String INITIALIZE_ACTION = "initialize";
    private static final String DOWNLOAD_FILE_ACTION = "downloadFile";
    private static final String GET_FILE_URI_ACTION = "getFileURI";
    private static final String GET_FILE_URIS_ACTION = "getFileURIs";
    private static final String DELETE_FILE_ACTION = "deleteFile";
    private static final String DELETE_FILES_ACTION = "deleteFiles";

    private static final int NO_ERROR = 0;
    private static final int ARGS_MISSING_ERROR = 1;
    private static final int INVALID_URL_ERROR = 2;
    private static final int CONNECTIVITY_ERROR = 3;
    private static final int HTTP_REQUEST_ERROR = 4;
    private static final int HTTP_REQUEST_CONTENT_ERROR = 5;
    private static final int DIRECTORY_CREATION_ERROR = 6;
    private static final int FILE_CREATION_ERROR = 7;
    private static final int JSON_CREATION_ERROR = 8;
    private static final int INITIALIZATION_ERROR = 9;
    private static final int NOT_FOUND_ERROR = 10;
    private static final int UNREFERENCED_ERROR = 11;

    private static final String DEPRECATED_DATABASE_NAME = "assets.db";

    private String pathToAssets;

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);

        final Context applicationContext = cordova.getActivity().getApplicationContext();

        String pathToCache = applicationContext.getCacheDir().getAbsolutePath() + File.separator;

        String pathToPlugin = pathToCache + PLUGIN_FOLDER;
        pathToAssets = pathToPlugin + File.separator + ASSETS_FOLDER;

        if (!createFolderIfRequired(pathToPlugin)) {
            Log.e(TAG, "error -- unable to create plugin folder: " + pathToPlugin);
            return;
        } else if (!createFolderIfRequired(pathToAssets)) {
            Log.e(TAG, "error -- unable to create assets folder: " + pathToAssets);
            return;
        }

        pathToAssets += File.separator;

        int blockSize = getBlockSize();

        HttpToFile.setBlockSize(blockSize);
        HttpToFile.setLogger(new AndroidLogger());

        removeDeprecatedDatabases(pathToCache);

        disableConnectionReuseIfNecessary();

        initialized = true;
        Log.d(TAG, "initialized");
    }

    public void removeDeprecatedDatabases(String pathToCache) {
        // Removing database version 6.x.x
        String deprecatedDatabasePath = pathToCache + PLUGIN_FOLDER + File.separator + DEPRECATED_DATABASE_NAME;
        if (!deleteIfExists(deprecatedDatabasePath)) {
            Log.e(TAG, "Unable to delete deprecated database version 6.x.x at path: " + deprecatedDatabasePath);
        }

        // Removing database version <= 5.x.x
        deprecatedDatabasePath = pathToCache + DEPRECATED_DATABASE_NAME;
        if (!deleteIfExists(deprecatedDatabasePath)) {
            Log.e(TAG, "Unable to delete deprecated database version <= 5.x.x at path: " + deprecatedDatabasePath);
        }
    }

    @SuppressWarnings("deprecation")
    @SuppressLint("NewApi")
    private void getBlockSize() {
        android.os.StatFs stat = new android.os.StatFs(pathToAssets);
        long blockSizeLong;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR2) {
            blockSizeLong = stat.getBlockSizeLong();
        } else {
            blockSizeLong = stat.getBlockSize();
        }
        if (blockSizeLong < 1024) {
            blockSizeLong = 1024;
        } else if (blockSizeLong > 16384) {
            blockSizeLong = 16384;
        }
        blockSize = (int)blockSizeLong;
        return blockSize;
    }

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if (!initialized) {
            callbackContext.error(INITIALIZATION_ERROR);
            return true;
        }

        if (action.equals(INITIALIZE_ACTION)) {
            callbackContext.success();
            return true;
        } else if (action.equals(DOWNLOAD_FILE_ACTION)) {
            final String url = args.getString(0);
            final String uri = args.getString(1);
            final CallbackContext _callbackContext = callbackContext;
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    String filePath = buildAssetFilePathFromUri(uri);
                    File file = new File(filePath);
                    if (file.exists()) {
                        // File is already in cache folder, don't download it
                        Log.d(TAG, "[Is in cache] " + uri);
                        String result = buildLocalFileUrl(file.getAbsolutePath());
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

            String uri = args.getString(0);
            String filePath = buildAssetFilePathFromUri(uri);
            File file = new File(filePath);
            if (file.exists()) {
                String asset = buildLocalFileUrl(filePath);
                Log.d(TAG, "[getFileURI] Returning full path: " + asset);
                callbackContext.success(asset);
            } else {
                callbackContext.error(NOT_FOUND_ERROR);
            }
            return true;
        } else if (action.equals(GET_FILE_URIS_ACTION)) {
            // Return all assets as asset map object
            Log.d(TAG, "[getFileURIs] returning all assets as map");
            try {
                JSONObject assetObject = getAllAssets();
                callbackContext.success(assetObject);
            } catch (JSONException e) {
                Log.d(TAG, "[getFileURIs] error: " + e.toString());
                callbackContext.error(JSON_CREATION_ERROR);
            }
            return true;
        } else if (action.equals(DELETE_FILES_ACTION)) {
            // Delete all files from given array
            Log.d(TAG, "[deleteFiles] *********** ");
            deleteFiles(args, callbackContext);

            return true;
        } else if (action.equals(DELETE_FILE_ACTION)) {
            Log.d(TAG, "[deleteFile] *********** " + args.getString(0));
            String uri = args.getString(0);
            try {
                deleteAsset(uri);
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

    private JSONObject getAllAssets() throws JSONException {
        JSONObject assets = new JSONObject();
        File assetsRoot = new File(pathToAssets);
        getAllAssets(assetsRoot, assets);
        return assets;
    }

    private void getAllAssets(File file, JSONObject assets) throws JSONException {
        if (file.isDirectory()) {
            String files[] = file.list();
            for (String filename : files) {
                File subFile = new File(file, filename);
                getAllAssets(subFile, assets);
            }
        } else {
            String filePath = file.getAbsolutePath();
            assets.put(buildUriFromAssetFilePath(filePath), buildLocalFileUrl(filePath));
        }
    }

    private String buildUriFromAssetFilePath(String filePath) {
        return filePath.substring(pathToAssets.length());
    }

    private String buildAssetFilePathFromUri(String uri) {
        if (uri.charAt(0) == File.separatorChar) {
            return pathToAssets + uri.substring(1);
        }
        return pathToAssets + uri;
    }

    private String buildLocalFileUrl(String filePath) {
        return "file://" + filePath;
    }

    private void deleteFiles(JSONArray uris, CallbackContext callbackContext) {
        DeleteAssetsCallback callback = new DeleteAssetsCallback(callbackContext);
        AsyncDelete asyncDelete = new AsyncDelete(callback);
        asyncDelete.execute(uris);
    }

    private void deleteAsset(String uri) throws IOException {
        if (uri != "") {
            String filePath = buildAssetFilePathFromUri(uri);
            File file = new File(filePath);
            boolean deleteSucceed = deleteFile(file);
            if (!deleteSucceed) {
                throw new IOException(filePath + " could not be deleted.");
            }
        }
    }

    public static boolean deleteFile(File file) {
        boolean deleteSucceed = true;
        if (file.isDirectory()) {
            String files[] = file.list();
            for (String temp : files) {
                File fileDelete = new File(file, temp);
                deleteSucceed = deleteFile(fileDelete) && deleteSucceed;
            }
        }
        boolean deleteResult = file.delete();
        if (!deleteResult) {
            deleteResult = !file.exists();
        }
        deleteSucceed = deleteResult && deleteSucceed;
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

    private boolean createFolderIfRequired(String folderPath) {
        File folder = new File(folderPath);
        if (folder.exists()) {
            return folder.isDirectory();
        }
        return folder.mkdir();
    }

    private boolean deleteIfExists(String path) {
        File file = new File(path);
        return !file.exists() || file.delete();
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
        public AsyncDelete(DeleteAssetsCallback callback) {
            this.callback = callback;
        }

        protected Integer doInBackground(JSONArray... jsonArrays) {
            int count = jsonArrays.length;
            if (count != 1) {
                return DELETE_PARAMETERS_LENGTH_ERROR;
            }

            // We only process one array, no more than one JSON array should be passed
            JSONArray jsonArray = jsonArrays[0];
            int countUris = jsonArray.length();
            for (int i = 0; i < countUris; i++) {
                try {
                    deleteAsset(jsonArray.getString(i));
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

    private class AsyncDownload extends AsyncTask<File, Void, Void> {

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
        protected Void doInBackground(File... params) {
            File file = null;
            try {
                try {
                    if (!initialized) {
                        Log.e(TAG, "Plugin not initialized, call initialize");
                        throw new Exception("Plugin not initialized, call initialize");
                    }
                    file = new File(this.filePath);
                    URL url = new URL(this.url);
                    boolean success = HttpToFile.downloadFile(url, file);
                    if (!success) {
                        this.callbackContext.error(createDownloadFileError(CONNECTIVITY_ERROR));
                    }
                } catch (IOException e) {
                    this.callbackContext.error(createDownloadFileError(CONNECTIVITY_ERROR));
                } catch (Exception e) {
                    this.callbackContext.error(createDownloadFileError(CONNECTIVITY_ERROR));
                }
                if (file == null) {
                    Log.d(TAG, "ERROR: could not get file");
                    this.callbackContext.error(createDownloadFileError(CONNECTIVITY_ERROR));
                } else {
                    String fileAbsolutePath = file.getAbsolutePath();
                    Log.d(TAG, "[DownloadedPlugin ] " + fileAbsolutePath);
                    callbackContext.success(fileAbsolutePath);
                }
            } catch (JSONException e) {
                this.callbackContext.error(JSON_CREATION_ERROR); // TODO: fix errors
            }
            return null;
        }
    }

    JSONObject createDownloadFileError(int code) throws JSONException {
        return createDownloadFileError(code, -1, null);
    }

    JSONObject createDownloadFileError(int code, int status) throws JSONException {
        return createDownloadFileError(code, status, null);
    }

    JSONObject createDownloadFileError(int code, String message) throws JSONException {
        return createDownloadFileError(code, -1, message);
    }

    JSONObject createDownloadFileError(int code, int status, String message) throws JSONException {
        JSONObject errorObject = new JSONObject();
        errorObject.put("code", code);
        if (status != -1) {
            errorObject.put("status", status);
        }
        if (message != null) {
            errorObject.put("message", message);
        } else {
            errorObject.put("message", "No description");
        }
        return errorObject;
    }

    private void disableConnectionReuseIfNecessary() {
        // Work around pre-Froyo bugs in HTTP connection reuse.
        if (Integer.parseInt(Build.VERSION.SDK) < Build.VERSION_CODES.FROYO) {
            System.setProperty("http.keepAlive", "false");
        }
    }
}
