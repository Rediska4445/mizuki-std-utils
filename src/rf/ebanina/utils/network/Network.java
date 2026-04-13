package rf.ebanina.utils.network;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class Network {
    public static InputStream followRedirects(URL originalUrl) throws IOException {
        return followRedirects(originalUrl, 5, HttpURLConnection.HTTP_OK);
    }

    public static InputStream followRedirects(URL originalUrl, int maxRedirects) throws IOException {
        return followRedirects(originalUrl, maxRedirects, HttpURLConnection.HTTP_OK);
    }

    public static InputStream followRedirects(URL originalUrl, int maxRedirects, int targetStatus) throws IOException {
        HttpURLConnection conn = null;
        int redirects = 0;
        URL url = originalUrl;

        while (redirects++ < maxRedirects) {
            conn = (HttpURLConnection) url.openConnection();
            conn.setInstanceFollowRedirects(false);
            conn.setRequestProperty("User-Agent",
                    "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
            conn.setConnectTimeout(10000);
            conn.setReadTimeout(10000);

            int code = conn.getResponseCode();

            if (code == targetStatus) {
                return conn.getInputStream();
            }

            if (code < 300 || code >= 400) {
                throw new IOException("HTTP " + code + " for " + url);
            }

            String location = conn.getHeaderField("Location");
            if (location == null) {
                throw new IOException("No Location header");
            }

            url = new URL(url, location);
        }

        throw new IOException("Too many redirects");
    }
}
