package jp.wizcorp.phonegap.plugin.WizAssets;

public interface ILogger {
	public void logInfo(String tag, String message);
	public void logDebug(String tag, String message);
	public void logError(String tag, String message);
}