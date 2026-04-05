package rf.ebanina.utils.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

public class Request {
    private URL url;
    private String method;
    private Proxy proxy;

    public Request(URL url) {
        this.url = url;
        this.method = "GET";
    }

    public Request setProxy(String ip, int port) {
        this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port));
        return this;
    }

    public Proxy getProxy() {
        return proxy;
    }

    public Request setProxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }

    public URL getUrl() {
        return url;
    }

    public Request setUrl(URL url) {
        this.url = url;
        return this;
    }

    public String getMethod() {
        return method;
    }

    public Request setMethod(String method) {
        this.method = method;
        return this;
    }

    public Response send() throws IOException {
        HttpURLConnection connection = (proxy != null)
                ? (HttpURLConnection) url.openConnection(proxy)
                : (HttpURLConnection) url.openConnection();

        connection.setRequestMethod(method);
        connection.setConnectTimeout(10000);

        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/110.0.0.0 Safari/537.36");

        int status = connection.getResponseCode();

        java.io.InputStream is = (status >= 200 && status < 300)
                ? connection.getInputStream()
                : connection.getErrorStream();

        StringBuilder response = new StringBuilder();

        if (is != null) {
            try (BufferedReader in = new BufferedReader(new InputStreamReader(is))) {
                String inputLine;

                while ((inputLine = in.readLine()) != null) {
                    response.append(inputLine);
                }
            }
        } else {
            throw new RuntimeException("is in null");
        }

        return new Response().setCode(status).setBody(response);
    }
}