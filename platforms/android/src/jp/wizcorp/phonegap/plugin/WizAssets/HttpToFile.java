package jp.wizcorp.phonegap.plugin.WizAssets;
import java.io.*;
import java.net.Authenticator;
import java.net.HttpURLConnection;
import java.net.PasswordAuthentication;
import java.net.URL;

public final class HttpToFile {
    private static int _blockSize;
    private static final String TAG = "WizAssetsPlugin";
    private static ILogger _logger;

    public static void setLogger(ILogger logger) {
        _logger = logger;
    }

    public static void setBlockSize(int blockSize) {
        _blockSize = blockSize;
    }

    public static int downloadFile(URL url, File file) throws IOException, Exception {
        _logger.logDebug(TAG, "[Downloading to] " + file.getAbsolutePath());
        BufferedInputStream inputStream = null;
        HttpURLConnection urlConnection = null;
        int httpStatus = -1;
        try {
            if (!createPath(file)) {
                _logger.logError(TAG, "file path error");
            } else {
                authenticate(url);

                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection = handleRedirect(urlConnection);
                httpStatus = urlConnection.getResponseCode();
                inputStream = new BufferedInputStream(urlConnection.getInputStream());
                writeFile(inputStream, file);
            }
        } catch (Exception e) {
            printError(e);
            throw e;
        } finally {
            try {
                closeStream(inputStream);
            } finally {
                if (urlConnection != null) {
                    urlConnection.disconnect();
                }
            }
        }
        return httpStatus;
    }

    private static HttpURLConnection handleRedirect(HttpURLConnection urlConnection) throws IOException {
        int status = urlConnection.getResponseCode();
        while (status != HttpURLConnection.HTTP_OK
                && (status == HttpURLConnection.HTTP_MOVED_TEMP
                || status == HttpURLConnection.HTTP_MOVED_PERM
                || status == HttpURLConnection.HTTP_SEE_OTHER)) {
            urlConnection.disconnect();
            String newUrl = urlConnection.getHeaderField("Location");
            urlConnection = (HttpURLConnection) new URL(newUrl).openConnection();
            _logger.logDebug(TAG, "Redirect to URL : " + newUrl);
            status = urlConnection.getResponseCode();
        }
        _logger.logDebug(TAG, "Response Code ... " + status);
        return urlConnection;
    }

    private static boolean createPath(File file) {
        File dir = file.getParentFile();
        if (dir == null || !(dir.mkdirs() || dir.isDirectory())) {
            _logger.logError(TAG, "Error: subdirectory could not be created");
            return false;
        }
        return true;
    }

    private static void writeFile(BufferedInputStream inputStream, File file) throws IOException {
        byte[] buffer = new byte[_blockSize];

        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            int len1;
            while ((len1 = inputStream.read(buffer)) > 0) {
                fos.write(buffer, 0, len1);
                String data = new String(buffer, "UTF-8");
            }
        } finally {
            try {
                closeStream(fos);
            }
            finally {
                closeStream(inputStream);
            }
        }
    }

    private static void closeStream(Closeable stream) throws IOException {
        if (stream != null) {
            try {
                stream.close();
            } catch (IOException e) {
                printError(e);
                throw e;
            }
        }
    }

    private static void authenticate(URL url) {
        final String userInfo = url.getUserInfo();
        if (userInfo != null) {
            String[] infoArray = userInfo.split(":");
            final String userName = infoArray[0];
            final char[] pw;
            if (infoArray.length > 1) {
                pw = infoArray[1].toCharArray();
            } else {
                pw = new char[0];
            }
            Authenticator.setDefault(new Authenticator() {
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(userName, pw);
                }
            });
        }
    }

    private static void printError(Exception e) {
        _logger.logError(TAG, "error: " + e);
    }
}