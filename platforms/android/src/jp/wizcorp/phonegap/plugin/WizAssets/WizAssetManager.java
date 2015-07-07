/* 
 *
 *  __    __ _                  _     _                _                                              
 * / / /\ \ (_)______ _ _ __ __| |   /_\  ___ ___  ___| |_    /\/\   __ _ _ __   __ _  __ _  ___ _ __ 
 * \ \/  \/ / |_  / _` | '__/ _` |  //_\\/ __/ __|/ _ \ __|  /    \ / _` | '_ \ / _` |/ _` |/ _ \ '__|
 *  \  /\  /| |/ / (_| | | | (_| | /  _  \__ \__ \  __/ |_  / /\/\ \ (_| | | | | (_| | (_| |  __/ |   
 *   \/  \/ |_/___\__,_|_|  \__,_| \_/ \_/___/___/\___|\__| \/    \/\__,_|_| |_|\__,_|\__, |\___|_|   
 *                                                                                    |___/          
 * @author Ally Ogilvie  
 * @copyright Wizcorp Inc. [ Incorporated Wizards ] 2014
 * @file    - wizAssetManager.java
 * @about   - JavaScript download and update asset example for PhoneGap
 */
package jp.wizcorp.phonegap.plugin.WizAssets;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.json.JSONException;
import org.json.JSONObject;

import android.content.Context;
import android.database.Cursor;
import android.database.CursorIndexOutOfBoundsException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.util.Log;

public class WizAssetManager {

    // private vars
    private String TAG = "WizAssetManager";
    private String DATABASE_EXTERNAL_FILE_PATH;
    private String DATABASE_INTERNAL_FILE_PATH = "www/phonegap/plugin/wizAssets/";

    // to manipulate the home of the db change this string
    private static final String DATABASE_NAME = "assets.db";
    private static final String DATABASE_TABLE_NAME = "assets";
    private SQLiteDatabase database;

    boolean initialiseDatabase;
    Context that;

    public WizAssetManager(Context context) {
        Log.d(TAG, "Booting Wizard Asset Manager.");

        // context is application context
        that = context;
        DATABASE_EXTERNAL_FILE_PATH =  that.getCacheDir().getAbsolutePath();
        Log.d(TAG, "external database file path -- " + DATABASE_EXTERNAL_FILE_PATH + File.separator + DATABASE_NAME);
        initialiseDatabase = (new File(DATABASE_EXTERNAL_FILE_PATH + File.separator + DATABASE_NAME)).exists();
        if (initialiseDatabase == false) {
            buildDB();
        } else {
            database = SQLiteDatabase.openDatabase(DATABASE_EXTERNAL_FILE_PATH + File.separator + DATABASE_NAME, null, SQLiteDatabase.OPEN_READWRITE);
            Log.d(TAG, "DB already initiated.");
        }
    }

    public JSONObject getAllAssets() {

        // try to open database from external storage (we should have moved it there), 
        // if nothing in the external storage move the app version out to external
        // if not existing internal, return empty object and we can stream the assets in
        JSONObject returnObject = new JSONObject();
        Log.d(TAG, "Read DB");

        try {
            // select all and put to JSONObject
            Cursor cursor;
            cursor = database.rawQuery("select * from " + DATABASE_TABLE_NAME, null);
            String uri;
            String filePath;

            Log.d(TAG, "move cursor");
            while (cursor.moveToNext()) {
                uri = cursor.getString(cursor.getColumnIndex("uri"));
                filePath = cursor.getString(cursor.getColumnIndex("filePath"));
                // push to object 
                try {
                    returnObject.put(uri, filePath);
                } catch (JSONException e) {
                    // log, ignore
                    Log.e(TAG, "JSON error -- " + e.getMessage(), e);
                }
            }

            cursor.close();
            Log.d(TAG, "returnObject -> " + returnObject.toString()); 

        } catch (SQLiteException e3) {
            // ignore
            Log.e(TAG, "error -- " + e3.getMessage(), e3);
            // on exception we send back an empty object
        }
        return returnObject;
    }

    private void buildDB() {
        // Init DB from bundle assets out to storage
        Log.d(TAG, "Init DB");

        // Open the .db file from assets directory
        try {
            InputStream is = that.getAssets().open(DATABASE_INTERNAL_FILE_PATH + DATABASE_NAME);

            // Copy the database into the destination
            OutputStream os = new FileOutputStream(DATABASE_EXTERNAL_FILE_PATH + File.separator + DATABASE_NAME);

            byte[] buffer = new byte[1024];
            int length;
            while ((length = is.read(buffer)) > 0){
                os.write(buffer, 0, length);
            }
            os.flush();

            os.close();
            is.close();

            database = SQLiteDatabase.openDatabase(DATABASE_EXTERNAL_FILE_PATH + File.separator + DATABASE_NAME, null, SQLiteDatabase.OPEN_READWRITE);
            Log.d(TAG, "Init DB Finish");

        } catch (IOException e1) {
            // log, ignore and send back nothing
            Log.e(TAG, "IO error -- " + e1.getMessage(), e1);
        }

    }

    public void downloadedAsset(String relativePath, String absolutePath) {
        // Downloaded file, add / edit database
        try {
            // Will replace if exists
            String sqlInsert = "insert or replace into " + DATABASE_TABLE_NAME + " values(?,?)";
            database.execSQL(sqlInsert, new Object[] { relativePath, absolutePath });
        } catch (Exception e) {
            Log.e(TAG, "error -- " + e.getMessage(), e);
        }
    }

    public String getFile(String relpath) {
        // returns file path to asset from database
        Cursor cursor = null;
        try {
            // Will replace if exists
            String sqlSearch = "select filePath from " + DATABASE_TABLE_NAME + " where uri= ?";
            cursor = database.rawQuery(sqlSearch, new String[] { relpath });
        } catch (Exception e) {
            Log.e(TAG, "error -- " + e.getMessage(), e);
            return "";
        }

        String result;
        try {
            if (cursor.moveToFirst()) {
                result = cursor.getString(0);               
            } else {
                // Cursor move error
                result = "NotFoundError";
            }
        } catch (CursorIndexOutOfBoundsException e) {
            Log.e(TAG, "cursor not found error: " + e );
            result = "NotFoundError";
        } catch (Exception ex) {
            Log.e(TAG, "cursor error: " + ex );
            result = "NotFoundError";
        } finally {
            cursor.close();
        }

        return result;
    }

    public void deleteFile(String filePath) {
        try {
            // Delete file
            database.delete(DATABASE_TABLE_NAME, "filePath= ?", new String[] { filePath });
        } catch (Exception e) {
            Log.e(TAG, "Delete file error -- " + e.getMessage(), e);
        }
    }

    public void deleteFolder(String filePath) {
        try {
            // Delete all entries starting with the file path
            database.delete(DATABASE_TABLE_NAME, "filePath like ?", new String[] { filePath + '%' });
        } catch (Exception e) {
            Log.e(TAG, "Delete file error -- " + e.getMessage(), e);
        }
    }
}