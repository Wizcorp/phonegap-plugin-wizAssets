package jp.wizcorp.phonegap.plugin.WizAssets;

public class ConsoleLogger implements ILogger {
	public void logInfo(String tag, String message) {
		System.out.println(tag + ' ' + message);
	}

	public void logDebug(String tag, String message) {
		System.out.println(tag + ' ' + message);
	}

	public void logError(String tag, String message) {
		System.out.println(tag + ' ' + message);
	}
}