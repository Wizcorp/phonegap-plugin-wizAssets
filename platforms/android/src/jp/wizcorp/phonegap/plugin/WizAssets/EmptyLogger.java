package jp.wizcorp.phonegap.plugin.WizAssets;

public class EmptyLogger implements ILogger {
    public final static EmptyLogger INSTANCE = new EmptyLogger();

	private EmptyLogger() {

	}

	public void logInfo(String tag, String message) {

	}

	public void logDebug(String tag, String message) {

	}

	public void logError(String tag, String message) {

	}
}