package rf.ebanina.utils.network.okhttp;

import okhttp3.ResponseBody;
import rf.ebanina.utils.network.Request;
import rf.ebanina.utils.network.Response;
import rf.ebanina.utils.network.UserAgent;

import java.io.IOException;
import java.net.URL;

public class OkHttpRequest
        extends Request
{
    private final OkHttpClient okHttpClient;
    private volatile okhttp3.Call activeCall;

    public OkHttpRequest(URL url) {
        super(url);

        this.okHttpClient = OkHttpClient.DEFAULT;
    }

    public OkHttpRequest(URL url, OkHttpClient client) {
        super(url);
        this.okHttpClient = client;
    }

    @Override
    public Response send()
            throws IOException
    {
        okhttp3.Request request = new okhttp3.Request.Builder()
                .url(super.getUrl())
                .header("User-Agent", UserAgent.WINDOWS_CHROME.getCode())
                .header("Accept", "application/json")
                .build();

        this.activeCall = okHttpClient.okHttpClient().newCall(request);

        try (okhttp3.Response response = this.activeCall.execute()) {
            ResponseBody body = response.body();
            String bodyString = (body != null) ? body.string() : "";

            return new OkHttpResponse()
                    .setCode(response.code())
                    .setBody(new StringBuilder(bodyString));
        } finally {
            okHttpClient.okHttpClient().dispatcher().executorService().shutdown();
            okHttpClient.okHttpClient().connectionPool().evictAll();
        }
    }

    public void abort() {
        if (activeCall != null && !activeCall.isCanceled()) {
            activeCall.cancel();
        }
    }
}
