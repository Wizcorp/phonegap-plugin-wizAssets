package jp.wizcorp.phonegap.plugin.WizAssets;
import android.utils.Log;

public class AndroidLogger implements ILogger {
    public void logInfo(String tag, String message) {
        Log.i(tag, message);
    }

    public void logDebug(String tag, String message) {
        Log.d(tag, message);
    }

    public void logError(String tag, String message) {
        Lod.e(tag, message);
    }
}