package rf.ebanina.utils.network.okhttp;

public record OkHttpClient(okhttp3.OkHttpClient okHttpClient) {
    public static OkHttpClient DEFAULT = buildDefault();

    public static OkHttpClient buildDefault() {
        return new OkHttpClient(new okhttp3.OkHttpClient.Builder()
                .connectTimeout(10, java.util.concurrent.TimeUnit.SECONDS)
                .readTimeout(30, java.util.concurrent.TimeUnit.SECONDS)
                .build());
    }
}
