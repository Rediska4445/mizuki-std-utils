package rf.ebanina.utils.network;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.URL;

/**
 * <h3>HTTP клиент</h3>
 * Создает HttpURLConnection и отправляет GET запросы к Deezer endpoints.
 * <p>
 * <b>Назначение:</b> абстракция над raw HttpURLConnection → {@link Response}.
 * </p>
 *
 * <h3>Основной lifecycle:</h3>
 * <pre>{@code
 * Request request = new Request(url);     // https://api.deezer.com/search/track?q=song
 * Response response = request.send();     // GET → status + body
 * String json = response.getBody().toString();  // {"data":[...]}
 * List<TrackData> tracks = JsonProcess.parse(json);
 * }</pre>
 *
 * <h3>Ключевые методы:</h3>
 * <table>
 *   <tr><th>Метод</th><th>Описание</th></tr>
 *   <tr><td>{@link #send()}</td><td>Выполняет HTTP GET → {@link Response}</td></tr>
 *   <tr><td>constructor</td><td>Инициализация URL</td></tr>
 * </table>
 *
 * <h3>Внутренняя логика send():</h3>
 * <ol>
 *   <li><code>HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection()</code></li>
 *   <li><code>conn.setRequestMethod("GET")</code></li>
 *   <li><code>int status = conn.getResponseCode()</code></li>
 *   <li><code>StringBuilder body = readStream(conn.getInputStream() / errorStream)</code></li>
 *   <li><code>return new Response().setCode(status).setBody(body)</code></li>
 * </ol>
 *
 * <h3>Deezer API примеры:</h3>
 * <pre>{@code
 * // Поиск треков
 * Request search = new Request("https://api.deezer.com/search/track?q=bohemian");
 * Response r1 = search.send();
 *
 * // Трек по ID
 * Request track = new Request("https://api.deezer.com/track/123456");
 * Response r2 = track.send();
 *
 * // Плейлист
 * Request playlist = new Request("https://api.deezer.com/playlist/789");
 * Response r3 = playlist.send();
 * }</pre>
 *
 * <h3>Потокобезопасность:</h3>
 * <p><b>Thread-safe:</b> stateless, каждый send() создает новое HttpURLConnection.
 * </p>
 *
 * <h3>Ограничения:</h3>
 * <ul>
 *   <li>Только GET (search/track/playlist)</li>
 *   <li>Без timeout/user-agent (по умолчанию JVM)</li>
 *   <li>Raw StringBuilder (без Jackson/Gson)</li>
 * </ul>
 *
 * <h3>Интеграция с JsonProcess:</h3>
 * <pre>{@code
 * public List<TrackData> searchTracks(String query) {
 *     Request req = new Request("https://api.deezer.com/search/track?q=" + query);
 *     Response res = req.send();
 *     if(res.getCode() == 200) {
 *         return JsonProcess.parseTracks(res.getBody().toString());
 *     }
 *     return Collections.emptyList();
 * }
 * }</pre>
 */
public class Request {
    /**
     * <h3>Целевой URL</h3>
     * Endpoint запроса (может меняться fluent setUrl).
     * <p>
     * <b>Примеры:</b>
     * <ul>
     *   <li><code><a href="https://api.spotify.com/v1/search?q=track:bohemian">...</a></code></li>
     *   <li><code><a href="https://jsonplaceholder.typicode.com/posts/1"</code></li>
     *   <li><code><a href="https://httpbin.org/get"</code></li>
     * </ul>
     * </p>
     * <p>
     * <b>Fluent API:</b>
     * <pre>{@code
     * new Request()
     *     .setUrl(new URL("https://api.example.com/search"))
     *     .send();
     * }</pre>
     * </p>
     * <p>
     * <b>Immutable после send():</b> используется только в конструкторе/{@link #send()}.
     * </p>
     */
    private URL url;

    /**
     * <h3>HTTP метод</h3>
     * По умолчанию "GET" (универсальный HTTP клиент).
     * <p>
     * <b>Поддержка:</b>
     * <ul>
     *   <li><code>"GET"</code> - REST ресурсы, поиск</li>
     *   <li><code>"POST"</code> - создание данных</li>
     *   <li><code>"PUT"</code> - обновление</li>
     *   <li><code>"DELETE"</code> - удаление</li>
     * </ul>
     * </p>
     * <p>
     * <b>Установка:</b>
     * <pre>{@code
     * conn.setRequestMethod(method);  // В send()
     * }</pre>
     * </p>
     * <p>
     * <b>Thread-safety:</b> простые String, не mutable.
     * </p>
     *
     * @see HttpURLConnection#setRequestMethod(String)
     */
    private String method;

    /**
     * <h3>Proxy конфигурация</h3>
     * Опциональный прокси (HTTP/SOCKS), null = direct connection.
     */
    private Proxy proxy;
    /**
     * <h3>Создание GET запроса</h3>
     * Базовый URL + method="GET" по умолчанию.
     *
     * @param url целевой endpoint
     */
    public Request(URL url) {
        this.url = url;
        this.method = "GET";
    }
    /**
     * <h3>Proxy по IP:port (String convenience)</h3>
     * Создает HTTP proxy, возвращает this.
     * <p>
     * <b>Пример:</b> {@code .setProxy("proxy.corp", 8080)}
     * </p>
     *
     * @param ip прокси адрес
     * @param port прокси порт
     * @return this
     */
    public Request setProxy(String ip, int port) {
        this.proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress(ip, port));
        return this;
    }
    /**
     * <h3>Текущий proxy</h3>
     * Readonly доступ к конфигурации.
     *
     * @return proxy или null
     */
    public Proxy getProxy() {
        return proxy;
    }
    /**
     * <h3>Proxy setter (Proxy объект)</h3>
     * Принимает готовый Proxy (HTTP/SOCKS).
     *
     * @param proxy прокси объект
     * @return this
     */
    public Request setProxy(Proxy proxy) {
        this.proxy = proxy;
        return this;
    }
    /**
     * <h3>URL геттер</h3>
     *
     * @return текущий URL
     */
    public URL getUrl() {
        return url;
    }
    /**
     * <h3>URL setter</h3>
     *
     * @param url новый endpoint
     * @return this
     */
    public Request setUrl(URL url) {
        this.url = url;
        return this;
    }
    /**
     * <h3>Метод геттер</h3>
     *
     * @return "GET" или custom
     */
    public String getMethod() {
        return method;
    }
    /**
     * <h3>Метод setter</h3>
     *
     * @param method HTTP метод
     * @return this
     */
    public Request setMethod(String method) {
        this.method = method;
        return this;
    }
    /**
     * <h3>Выполняет HTTP запрос → Response</h3>
     * Открывает соединение, читает статус + тело, возвращает Response.
     * <p>
     * <b>Бросает:</b> {@link IOException} (network, DNS, TLS ошибки).
     * </p>
     *
     * <h3>Полный алгоритм:</h3>
     * <ol>
     *   <li><code>HttpURLConnection conn = (HttpURLConnection) url.openConnection()</code></li>
     *   <li><code>conn.setRequestMethod(method)</code> (GET по умолчанию)</li>
     *   <li><code>int status = conn.getResponseCode()</code></li>
     *   <li><code>StringBuilder body = readResponse(conn)</code></li>
     *   <li><code>conn.disconnect()</code></li>
     *   <li><code>return new Response().setCode(status).setBody(body)</code></li>
     * </ol>
     *
     * <h3>Чтение тела (readResponse):</h3>
     * <pre>{@code
     * InputStream stream = (status >= 400) ? errorStream : inputStream;
     * BufferedReader reader = new BufferedReader(new InputStreamReader(stream));
     * StringBuilder body = new StringBuilder();
     * String line;
     * while((line = reader.readLine()) != null) {
     *     body.append(line).append("\n");
     * }
     * }</pre>
     *
     * <h3>Thread-safety:</h3>
     * <p><b>Thread-safe:</b> каждый вызов создает новое HttpURLConnection.
     * </p>
     *
     * <h3>Пример использования:</h3>
     * <pre>{@code
     * try {
     *     Response response = request.send();
     *     if(response.getCode() == 200) {
     *         String json = response.getBody().toString();
     *         // JsonProcess.parse(json)
     *     }
     * } catch (IOException e) {
     *     log.error("Network error: " + e.getMessage());
     * }
     * }</pre>
     *
     * <h3>Возвращаемые статусы:</h3>
     * <table>
     *   <tr><th>Код</th><th>Сценарий</th><th>Body</th></tr>
     *   <tr><td>200</td><td>OK</td><td>JSON данные</td></tr>
     *   <tr><td>404</td><td>Not Found</td><td>Error JSON</td></tr>
     *   <tr><td>429</td><td>Rate Limit</td><td>Error JSON</td></tr>
     *   <tr><td>500</td><td>Server Error</td><td>Error HTML/JSON</td></tr>
     * </table>
     *
     * @return {@link Response} с code + body
     * @throws IOException сеть, DNS, SSL, timeout
     * @see HttpURLConnection#getResponseCode()
     * @see HttpURLConnection#getInputStream()
     * @see HttpURLConnection#getErrorStream()
     */
    public Response send()
            throws IOException
    {
        HttpURLConnection connection = (proxy != null)
                ? (HttpURLConnection) url.openConnection(proxy)
                : (HttpURLConnection) url.openConnection();

        connection.setRequestMethod(method);
        connection.setConnectTimeout(10000);
        connection.setRequestProperty("Accept", "application/json");
        connection.setRequestProperty("User-Agent", UserAgent.WINDOWS_CHROME.getCode());

        return send(connection);
    }

    /**
     * <h3>Чтение Response (low-level)</h3>
     * Универсальный метод чтения готового HttpURLConnection.
     * <p>
     * <b>Stream логика:</b>
     * <ul>
     *   <li>2xx → {@code getInputStream()}</li>
     *   <li>4xx/5xx → {@code getErrorStream()}</li>
     *   <li>{@code is == null → RuntimeException}</li>
     * </ul>
     * </p>
     * <p>
     * <b>Чтение:</b> BufferedReader → StringBuilder (line-by-line).
     * </p>
     * <p>
     * <b>AutoClose:</b> try-with-resources для InputStreamReader.
     * </p>
     * <p>
     * <b>Fluent Response:</b> {@code new Response().setCode().setBody()}.
     * </p>
     * <p>
     * <b>Расширяемость:</b> можно переиспользовать с custom connection.
     * </p>
     *
     * @param connection готовое соединение
     * @return Response(code, body StringBuilder)
     * @throws IOException read/parse ошибки
     */
    public Response send(HttpURLConnection connection)
            throws IOException
    {
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

        return new Response()
                .setCode(status)
                .setBody(response);
    }
}