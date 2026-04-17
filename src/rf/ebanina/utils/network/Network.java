package rf.ebanina.utils.network;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * <h1>Network</h1>
 * Утилита для HTTP запросов с автоматическим следование редиректам.
 * <p>
 * <b>Назначение:</b> обработка 301/302 редиректов Deezer API/CDN без ручного кода.
 * </p>
 *
 * <h2>Основная функциональность</h2>
 * <ul>
 *   <li>Автоматическое следование Location заголовкам (301, 302)</li>
 *   <li>Защита от redirect loops (maxRedirects)</li>
 *   <li>Стандартные таймауты и User-Agent</li>
 *   <li>Поддержка произвольного targetStatus (200, 404...)</li>
 * </ul>
 *
 * <h2>Перегрузки</h2>
 * <table>
 *   <tr><th>Сигнатура</th><th>Параметры по умолчанию</th></tr>
 *   <tr><td>{@code followRedirects(url)}</td><td>max=5, status=200</td></tr>
 *   <tr><td>{@code followRedirects(url, maxRedirects)}</td><td>status=200</td></tr>
 *   <tr><td>{@code followRedirects(url, max, status)}</td><td>-</td></tr>
 * </table>
 */
public class Network {
    /**
     * <h3>Следование редиректам (convenience)</h3>
     * Стандартные параметры: max=5, status=200 (OK).
     * <p>
     * <b>Использование:</b>
     * <pre>{@code
     * InputStream response = Network.followRedirects(trackPreviewUrl);
     * StringBuilder body = readStream(response);
     * }</pre>
     * </p>
     *
     * @param originalUrl начальный URL (может редиректить)
     * @return InputStream финального ответа
     * @throws IOException HTTP ошибки, редиректы, таймауты
     */
    public static InputStream followRedirects(URL originalUrl) throws IOException {
        return followRedirects(originalUrl, 5, HttpURLConnection.HTTP_OK);
    }
    /**
     * <h3>Следование редиректам (custom max)</h3>
     * Настраиваемый лимит редиректов, status=200.
     *
     * @param originalUrl начальный URL
     * @param maxRedirects максимум итераций (5-10 типично)
     * @return InputStream финального ответа
     * @throws IOException redirect loop, HTTP ошибки
     */
    public static InputStream followRedirects(URL originalUrl, int maxRedirects) throws IOException {
        return followRedirects(originalUrl, maxRedirects, HttpURLConnection.HTTP_OK);
    }
    /**
     * <h3>Полная версия (main алгоритм)</h3>
     * Универсальное следование редиректам с настраиваемым статусом.
     * <p>
     * <b>Алгоритм:</b>
     * <ol>
     *   <li>{@code conn.setInstanceFollowRedirects(false)} — manual control</li>
     *   <li>User-Agent (антибот защита)</li>
     *   <li>Таймауты 10s connect/read</li>
     *   <li>Проверка {@code code == targetStatus → return InputStream}</li>
     *   <li>Редирект 3xx → {@code new URL(url, location)}</li>
     *   <li>Защита: {@code redirects < maxRedirects}</li>
     * </ol>
     * </p>
     * <p>
     *
     * <h3>Логика while цикла</h3>
     * <pre>
     * url = originalUrl
     * while(redirects < maxRedirects):
     *   conn = url.openConnection()
     *   conn.setInstanceFollowRedirects(false)  // Manual!
     *   conn.setRequestProperty("User-Agent", "Mozilla...")
     *   conn.setConnectTimeout(10000)
     *   conn.setReadTimeout(10000)
     *
     *   code = conn.getResponseCode()
     *
     *   if(code == targetStatus):      → return conn.getInputStream()
     *   if(code < 300 || code >= 400): → throw IOException(code)
     *
     *   location = conn.getHeaderField("Location")
     *   if(location == null):          → throw "No Location"
     *
     *   url = new URL(url, location)   // Relative redirect support
     *
     * throw "Too many redirects"
     * </pre>
     *
     * <b>Deezer CDN редиректы:</b> api.deezer.com → e-cdn-files.dz.deezer.com
     * </p>
     *
     * @param originalUrl начальный URL
     * @param maxRedirects лимит (рекомендуется 5-10)
     * @param targetStatus желаемый код (200, 404...)
     * @return InputStream финального URL
     * @throws IOException HTTP ошибки, редиректы, таймауты
     */
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
