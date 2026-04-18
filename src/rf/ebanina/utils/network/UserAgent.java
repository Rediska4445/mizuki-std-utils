package rf.ebanina.utils.network;

public enum UserAgent {
    /**
     * Chrome на Windows 10/11 (самый популярный)
     */
    WINDOWS_CHROME("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Safari/537.36"),

    /**
     * Firefox на Windows
     */
    WINDOWS_FIREFOX("Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:123.0) Gecko/20100101 Firefox/123.0"),

    /**
     * Safari на macOS
     */
    MACOS_SAFARI("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3.1 Safari/605.1.15"),

    /**
     * Chrome на Android
     */
    ANDROID_CHROME("Mozilla/5.0 (Linux; Android 10; K) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/122.0.0.0 Mobile Safari/537.36"),

    /**
     * iPhone Safari
     */
    IOS_SAFARI("Mozilla/5.0 (iPhone; CPU iPhone OS 17_3_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/17.3.1 Mobile/15E148 Safari/604.1");

    private final String code;

    UserAgent(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}