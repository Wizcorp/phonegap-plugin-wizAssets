package jp.wizcorp.phonegap.plugin.WizAssets;
import java.io.*;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;
import jp.wizcorp.phonegap.plugin.WizAssets.ILogger;

public final class HttpToFile {
    private static int _blockSize;
    private static char[] _emptyCharArray = new char[0];
    private static final String TAG = "WizAssetsPlugin";
    private static ILogger _logger;

    public static void setLogger(ILogger logger) {
        _logger = logger;
    }

    public static void setBlockSize(int blockSize) {
        _blockSize = blockSize;
    }

    public static boolean downloadFile(URL url, File file) throws IOException {
        _logger.logDebug(TAG, "[Downloading to] " + file.getAbsolutePath());
        BufferedInputStream inputStream = null;
        HttpURLConnection urlConnection = null;
        Boolean successfulWrite = false;
        try {
            if (!createPath(file)) {
                _logger.logError(TAG, "file path error");
            } else {
                authenticate(url);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection = handleRedirect(urlConnection);
                inputStream = new BufferedInputStream(urlConnection.getInputStream());
                successfulWrite = writeFile(inputStream, file);
            }
        } catch (Exception e) {
            printError(e);
        } finally {
            if (inputStream != null)
                inputStream.close();
            if (urlConnection != null) {
                urlConnection.disconnect();
            }
        }
        return successfulWrite;
    }

    private static HttpURLConnection handleRedirect(HttpURLConnection urlConnection) throws IOException {
        int status = urlConnection.getResponseCode();
        if (status != HttpURLConnection.HTTP_OK) {
            if (status == HttpURLConnection.HTTP_MOVED_TEMP
                    || status == HttpURLConnection.HTTP_MOVED_PERM
                    || status == HttpURLConnection.HTTP_SEE_OTHER) {
                urlConnection.disconnect();
                String newUrl = urlConnection.getHeaderField("Location");
                urlConnection = (HttpURLConnection) new URL(newUrl).openConnection();
                _logger.logDebug(TAG, "Redirect to URL : " + newUrl);
            }
        }
        _logger.logDebug(TAG, "Response Code ... " + status);
        return urlConnection;
    }

    private static Boolean createPath(File file) {
        File dir = file.getParentFile();
        if (dir == null || !(dir.mkdirs() || dir.isDirectory())) {
            _logger.logError(TAG, "Error: subdirectory could not be created");
            return false;
        }
        return true;
    }

    private static Boolean writeFile(BufferedInputStream inputStream, File file) {
        byte[] buffer = new byte[_blockSize];

        FileOutputStream fos = null;
        boolean exceptionThrown = false;
        try {
            fos = new FileOutputStream(file);
            int len1;
            while ((len1 = inputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, len1);
                String data = new String(buffer, "UTF-8");
            }
        } catch (FileNotFoundException e) {
            printError(e);
            exceptionThrown = true;
        } catch (IOException e) {
            printError(e);
            exceptionThrown = true;
        } finally {
            if (fos != null) {
                try {
                    fos.close();
                } catch (IOException e) {
                    printError(e);
                    exceptionThrown = true;
                }
            }
            try {
                inputStream.close();
            } catch (IOException e) {
                printError(e);
                exceptionThrown = true;
            }
        }

        if (exceptionThrown) {
            return false;
        }
        // Tell Asset Manager to register this download to asset database
        String fileAbsolutePath = file.getAbsolutePath();
        _logger.logDebug(TAG, "[DownloadedHttpToFile ] " + fileAbsolutePath);
        return true;
    }

    private static void authenticate(URL url) {
        final String userInfo = url.getUserInfo();
        if (userInfo != null) {
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(userInfo, _emptyCharArray);
                }
            });
        }
    }

    private static void printError(Exception e) {
        _logger.logError(TAG, "error: " + e);
    }
}