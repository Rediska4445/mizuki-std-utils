package rf.ebanina.utils.loggining;

/**
 * <h3>Универсальный логгер (printf-style + generics)</h3>
 * Современная альтернатива {@link java.util.logging.Logger} с fluent API.
 * <p>
 * <b>Назначение:</b> структурированное логирование в утилитах (Request/Response, JSON парсинг).
 * </p>
 *
 * <h3>Методы по уровню:</h3>
 * <table>
 *   <tr><th>Уровень</th><th>Метод</th><th>Использование</th></tr>
 *   <tr><td>INFO</td><td>{@link #info(Object)}</td><td>Успешные операции</td></tr>
 *   <tr><td>WARN</td><td>{@link #warn(Object)}</td><td>Предупреждения</td></tr>
 *   <tr><td>SEVERE</td><td>{@link #severe(Object)}</td><td>Критические ошибки</td></tr>
 *   <tr><td>PROFILER</td><td>{@link #profiler(Object)}</td><td>Метрики производительности</td></tr>
 *   <tr><td>SUPPRESS</td><td>{@link #suppress(Object)}</td><td>Подавленные ошибки</td></tr>
 * </table>
 *
 * <h3>Printf-style:</h3>
 * <pre>{@code
 * logger.printf("Request %s → code=%d, size=%d", url, code, body.length());
 * logger.printf("Parse error: %s", e.getMessage());
 * }</pre>
 *
 * <h3>Generics (любые типы):</h3>
 * <pre>{@code
 * logger.info("Track: " + track.getTitle());
 * logger.info(track);                    // toString()
 * logger.warn("Rate limit: " + response);
 * logger.profiler("Parse time: " + duration);
 * logger.severe(e);                      // Exception
 * }</pre>
 *
 * <h3>Request/Response пример:</h3>
 * <pre>{@code
 * public class Request {
 *     private final ILogging logger = Logging.getLogger(Request.class);
 *
 *     public Response send() {
 *         logger.info("→ " + url);
 *         try {
 *             Response res = ...;
 *             logger.printf("← %s %d bytes", res.getCode(), res.getBody().length());
 *             return res;
 *         } catch (IOException e) {
 *             logger.severe(e);
 *             throw e;
 *         }
 *     }
 * }
 * }</pre>
 *
 * <h3>Thread-safety:</h3>
 * <p><b>Зависит от реализации:</b> ожидается synchronized/Concurrent.
 * </p>
 *
 * <h3>print_ln_st(msg_for_world):</h3>
 * <p><b>Специальный метод:</b> "print line stack trace?" — логирует с полным stack trace.
 * </p>
 * <pre>{@code
 * logger.print_ln_st("Critical API failure");  // + full stack
 * }</pre>
 *
 * @implNote Реализации: {@link Log}
 */
public interface ILogging {
    /**
     * <h3>Printf-style форматированное логирование</h3>
     * Аналог {@link String#formatted(Object...)} + вывод в логгер.
     * <p>
     * <b>Поддержка:</b> {@code %s}, {@code %d}, {@code %f}, {@code %%} (как {@link java.util.Formatter}).
     * </p>
     *
     * <h3>Примеры:</h3>
     * <pre>{@code
     * logger.printf("Request %s → code=%d", url, response.getCode());
     * logger.printf("Parse error: %s (%.2f sec)", e.getMessage(), duration);
     * logger.printf("Rate limit: %% left=%d", quota);
     * }</pre>
     *
     * <h3>Внутренняя логика:</h3>
     * <pre>{@code
     * // Псевдокод реализации
     * public void printf(String str, Object... arr) {
     *     String formatted = str.formatted(arr);
     *     log(INFO, formatted);
     * }
     * }</pre>
     *
     * @param str шаблон с плейсхолдерами (%s, %d, %f)
     * @param arr аргументы для подстановки
     * @see String#formatted(Object...)
     */
    void printf(String str, Object... arr);
    /**
     * <h3>Простой вывод без переноса строки</h3>
     * Базовый print для коротких сообщений (progress, status).
     * <p>
     * <b>Автоматический toString():</b> работает с любыми объектами.
     * </p>
     *
     * <h3>Примеры:</h3>
     * <pre>{@code
     * logger.print("Parsing ");           // "Parsing "
     * logger.print(response);             // "Response{code=200}"
     * logger.print(tracks.size());        // "5"
     * logger.print_ln_st("done");         // "done\n"
     * }</pre>
     *
     * @param msg любой объект (toString())
     */
    void print(Object msg);
    /**
     * <h3>Println с generics (типобезопасный)</h3>
     * Стандартный вывод **со строкой переноса**.
     * <p>
     * <b>Generics преимущество:</b> компилятор проверяет типы.
     * </p>
     *
     * <h3>Примеры:</h3>
     * <pre>{@code
     * logger.println("Request sent");              // String
     * logger.println(track.getTitle());            // String
     * logger.println(response.getCode());          // int → "200"
     * logger.println(Arrays.toString(tracks));     // массив
     * logger.println(e);                           // Exception
     * }</pre>
     *
     * <h3>Внутренняя логика:</h3>
     * <pre>{@code
     * public <T> void println(T msg) {
     *     log(INFO, msg != null ? msg.toString() : "null");
     * }
     * }</pre>
     *
     * @param msg <T> любой тип (toString() + \n)
     * @param <T> тип сообщения
     */
    <T> void println(T msg);
    /**
     * <h3>INFO уровень логирования (400)</h3>
     * Стандартный уровень для **нормальной работы** приложения.
     * <p>
     * <b>Семантика:</b> информация о прогрессе, успешных операциях, состоянии [web:120].
     * </p>
     *
     * <h3>Когда использовать:</h3>
     * <table>
     *   <tr><th>Использовать</th><th>Не использовать</th></tr>
     *   <tr><td>Запросы API отправлены</td><td>Каждый клик мыши</td></tr>
     *   <tr><td>Количество обработанных записей</td><td>Debug параметры</td></tr>
     *   <tr><td>Успешное завершение batch</td><td>Runtime переменные</td></tr>
     * </table>
     *
     * <h3>Примеры в Request/Response:</h3>
     * <pre>{@code
     * public Response send() {
     *     logger.info("→ GET " + url);                    // Запрос начат
     *
     *     Response res = doSend();
     *     logger.info("← " + res.getCode() + " OK");      // Успех
     *     logger.info("Parsed " + tracks.size() + " items"); // Результат
     *
     *     return res;
     * }
     * }</pre>
     *
     * <h3>Generics преимущества:</h3>
     * <ul>
     *   <li><b>Типобезопасность:</b> компилятор проверяет типы</li>
     *   <li><b>Авто toString():</b> работает с объектами, примитивными типами</li>
     *   <li><b>Null-safe:</b> "null" для null объектов</li>
     * </ul>
     *
     * <h3>Внутренняя реализация:</h3>
     * <pre>{@code
     * // Псевдокод ConsoleLogging
     * public <T> void info(T msg) {
     *     if (level <= Level.INFO.intValue()) {
     *         out.println("[INFO ] " + format(msg));
     *     }
     * }
     *
     * private String format(T msg) {
     *     return msg == null ? "null" : msg.toString();
     * }
     * }</pre>
     *
     * <h3>Вывод в консоль:</h3>
     * <pre>
     * [INFO ] Request → GET https://api.example.com/search
     * [INFO ] Request ← 200 OK
     * [INFO ] Request Parsed 5 tracks
     * </pre>
     *
     * @param <T> любой тип: String, Integer, Response, Exception, CustomObject
     * @param msg сообщение для логирования (toString() + timestamp)
     */
    <T> void info(T msg);
    /**
     * <h3>WARNING уровень логирования (900)</h3>
     * Логирует **потенциальные проблемы**, которые **не останавливают** работу приложения.
     *
     * <p><b>Семантика:</b> recoverable ошибки, подозрительные состояния, деградация сервиса.</p>
     *
     * <h3>Когда использовать WARN:</h3>
     * <table>
     *   <tr><th>WARN (правильно)</th><th>INFO/SEVERE (неправильно)</th></tr>
     *   <tr><td>HTTP 4xx (429 Rate Limit, 404 Not Found)</td><td>HTTP 200 OK</td></tr>
     *   <tr><td>Cache miss / fallback to network</td><td>Успешный кэш hit</td></tr>
     *   <tr><td>Deprecated API usage</td><td>Нормальная работа</td></tr>
     *   <tr><td>Timeout с retry</td><td>Фатальный timeout без восстановления</td></tr>
     *   <tr><td>Validation warning</td><td>Validation error (throw)</td></tr>
     * </table>
     *
     * <h3>Request/Response примеры:</h3>
     * <pre>{@code
     * public Response send() {
     *     try {
     *         Response res = doSend();
     *
     *         switch(res.getCode()) {
     *             case 200 -> logger.info("OK");
     *             case 404 -> logger.<b>warn</b>("Track not found: " + url);
     *             case 429 -> logger.<b>warn</b>("Rate limit exceeded");
     *             default  -> logger.warn("Unexpected status: " + res.getCode());
     *         }
     *         return res;
     *     } catch (SocketTimeoutException e) {
     *         logger.<b>warn</b>("Request timeout, will retry: " + e.getMessage());
     *         throw e;  // Retry logic
     *     }
     * }
     * }</pre>
     *
     * <h3>Generics + авто toString():</h3>
     * <pre>{@code
     * logger.warn(url);                           // URL
     * logger.warn(response);                      // Response{code=404}
     * logger.warn(e.getMessage());                // "Read timed out"
     * logger.warn("Cache miss: " + key);          // String
     * logger.warn(quota.remaining());             // 0
     * }</pre>
     *
     * <h3>Внутренняя реализация:</h3>
     * <pre>{@code
     * public <T> void warn(T msg) {
     *     if (isLoggable(Level.WARNING)) {
     *         String formatted = formatMessage("WARN", msg);
     *         appenders.forEach(a -> a.append(formatted));
     *     }
     * }
     * }</pre>
     *
     * <h3>Вывод в логах:</h3>
     * <pre>
     * [WARN ] Request Track not found: <a href="https://api.deezer.com/track/999999">...</a>
     * [WARN ] Request Rate limit exceeded
     * [WARN ] Request Unexpected status: 503
     * [WARN ] Request Request timeout, will retry: Read timed out
     * </pre>
     *
     * @param <T> любой тип: String, Response, URL, Exception, Integer, CustomObject
     * @param msg предупреждение о проблеме (не фатальной)
     */
    <T> void warn(T msg);
    /**
     * <h3>SEVERE уровень логирования (1000) — критические сбои</h3>
     * <b>Высочайший уровень</b> для <b>необратимых ошибок</b>, блокирующих нормальную работу.
     *
     * <p><b>Семантика JUL:</b> события, **предотвращающие нормальное выполнение программы**.</p>
     *
     * <h3>Когда SEVERE (строго):</h3>
     * <table>
     *   <tr><th>SEVERE (правильно)</th><th>WARN/INFO (неправильно)</th></tr>
     *   <tr><td>Network IOException без retry</td><td>HTTP 404/429 (recoverable)</td></tr>
     *   <tr><td>JSON parse failure → empty result</td><td>Cache miss</td></tr>
     *   <tr><td>Database connection lost</td><td>Deprecated API warning</td></tr>
     *   <tr><td>OutOfMemoryError</td><td>Validation warning</td></tr>
     *   <tr><td>Critical config missing</td><td>Slow response</td></tr>
     * </table>
     *
     * <h3>Request/Response примеры:</h3>
     * <pre>{@code
     * public Response send() {
     *     logger.info("→ " + url);
     *     try {
     *         return doSend();
     *     } catch (SocketTimeoutException e) {
     *         logger.<b>warn</b>("Timeout, retrying");  // recoverable
     *         throw e;
     *     } catch (IOException e) {
     *         logger.<b>severe</b>("Network failure, cannot recover: " + e);
     *         throw new RuntimeException("API unavailable", e);
     *     }
     * }
     *
     * public List<TrackData> parse(Response res) {
     *     try {
     *         return JsonProcess.parse(res.getBody());
     *     } catch (JsonException e) {
     *         logger.<b>severe</b>("JSON parse failed for " + res.getCode() +
     *             ": " + res.getBody());
     *         return Collections.emptyList();  // Graceful degradation
     *     }
     * }
     * }</pre>
     *
     * <h3>Generics + Throwable поддержка:</h3>
     * <pre>{@code
     * logger.severe(e);                           // Exception (stack trace)
     * logger.severe("Critical: " + e.getMessage()); // Message only
     * logger.severe(response);                     // Response.toString()
     * logger.severe("Config missing: " + key);     // String
     * }</pre>
     *
     * <h3>Внутренняя реализация (псевдокод):</h3>
     * <pre>{@code
     * public <T> void severe(T msg) {
     *     if (isLoggable(Level.SEVERE)) {
     *         String formatted = formatSevere(msg);  // Добавляет stack trace
     *         appenders.forEach(a -> a.append(formatted));
     *         // Возможен alert в продакшене
     *     }
     * }
     *
     * private String formatSevere(T msg) {
     *     if (msg instanceof Throwable t) {
     *         return "[SEVERE] " + t.getClass().getSimpleName() +
     *                ": " + t.getMessage() + "\n" + exceptionToString(t);
     *     }
     *     return "[SEVERE] " + msg.toString();
     * }
     * }</pre>
     *
     * <h3>Вывод в логах:</h3>
     * <pre>
     * [SEVERE] Request Network failure, cannot recover: ConnectException
     *         at rf.ebanina.Request.doSend(Request.java:45)
     *         at rf.ebanina.Request.send(Request.java:32)
     *
     * [SEVERE] JsonProcess JSON parse failed for 200: {"malformed":"json"
     * </pre>
     *
     * @param <T> любой тип: Exception, String, Response, URL
     * @param msg **критическая** ошибка, блокирующая функциональность
     */
    <T> void severe(T msg);
    /**
     * <h3>PROFILER уровень — метрики производительности</h3>
     * Специализированный уровень для **измерения времени выполнения**, throughput, latency.
     *
     * <p><b>Назначение:</b> мониторинг производительности HTTP запросов, парсинга JSON, кэширования.</p>
     *
     * <h3>Сценарии использования:</h3>
     * <table>
     *   <tr><th>Метрика</th><th>Пример</th></tr>
     *   <tr><td>HTTP latency</td><td>{@code logger.profiler("GET " + url + ": " + duration + "ms")}</td></tr>
     *   <tr><td>JSON parse time</td><td>{@code logger.profiler("Parse: " + tracks.size() + " items, " + time + "ms")}</td></tr>
     *   <tr><td>Cache hit/miss</td><td>{@code logger.profiler("Cache hit: " + key)}</td></tr>
     *   <tr><td>Batch processing</td><td>{@code logger.profiler("Processed " + count + " tracks/sec")}</td></tr>
     * </table>
     *
     * <h3>Request/Response пример:</h3>
     * <pre>{@code
     * public Response send() {
     *     long start = System.nanoTime();
     *     logger.info("→ " + url);
     *
     *     try {
     *         Response res = doSend();
     *         long durationMs = (System.nanoTime() - start) / 1_000_000;
     *
     *         logger.<b>profiler</b>(String.format("GET %s: %d ms, %d bytes",
     *             url.getPath(), durationMs, res.getBody().length()));
     *
     *         return res;
     *     } catch (Exception e) {
     *         long durationMs = (System.nanoTime() - start) / 1_000_000;
     *         logger.profiler("GET FAILED " + url + ": " + durationMs + "ms");
     *         throw e;
     *     }
     * }
     * }</pre>
     *
     * <h3>Generics примеры:</h3>
     * <pre>{@code
     * logger.profiler(durationMs + " ms");              // Long
     * logger.profiler("Parse: " + count + "/s");        // String
     * logger.profiler(res);                             // Response
     * logger.profiler(new PerfMetrics(url, time, size)); // Custom object
     * }</pre>
     *
     * <h3>Внутренняя реализация (псевдокод):</h3>
     * <pre>{@code
     * public <T> void profiler(T msg) {
     *     if (isLoggable(Level.PROFILER)) {  // Custom level ~600
     *         String formatted = "[PROFILER] " + format(msg) +
     *             " [" + Thread.currentThread().getName() + "]";
     *         appenders.forEach(a -> a.append(formatted));
     *     }
     * }
     * }</pre>
     *
     * <h3>Вывод в логах:</h3>
     * <pre>
     * [PROFILER] GET /search/track?q=queen: 245 ms, 1250 bytes [main]
     * [PROFILER] Parse: 5 items, 12 ms [main]
     * [PROFILER] Cache hit: track_123 [ForkJoinPool]
     * [PROFILER] GET FAILED /track/999: 1500 ms [main]
     * </pre>
     *
     * <h3>Thread-safety:</h3>
     * <p><b>Ожидается synchronized:</b> метрики из разных потоков.</p>
     *
     * @param <T> любой тип: Long (ms), String, Response, PerfMetrics
     * @param msg метрика производительности (время, размер, throughput)
     */
    <T> void profiler(T msg);
    /**
     * <h3>SUPPRESS уровень — подавленные ошибки</h3>
     * Логирует **ошибки, которые обработаны** и **не нарушают** основную функциональность.
     *
     * <p><b>Назначение:</b> "silent logging" для graceful degradation, fallback'ов, non-critical failures.</p>
     *
     * <h3>Когда использовать SUPPRESS:</h3>
     * <table>
     *   <tr><th>SUPPRESS (правильно)</th><th>SEVERE/WARN (неправильно)</th></tr>
     *   <tr><td>Cache load failed → use stale data</td><td>Network timeout</td></tr>
     *   <tr><td>Optional metadata parse failed</td><td>Primary JSON parse failed</td></tr>
     *   <tr><td>Image thumbnail load failed</td><td>Critical resource missing</td></tr>
     *   <tr><td>Fallback to default config</td><td>Config completely missing</td></tr>
     * </table>
     *
     * <h3>Request/Response примеры:</h3>
     * <pre>{@code
     * public List<TrackData> parse(Response res) {
     *     try {
     *         return JsonProcess.parseTracks(res.getBody());
     *     } catch (JsonException e) {
     *         logger.<b>severe</b>("Primary parse failed");  // Critical!
     *         return Collections.emptyList();
     *     }
     * }
     *
     * public TrackMetadata parseMetadata(Response res) {
     *     try {
     *         return JsonProcess.parseMetadata(res.getBody());
     *     } catch (JsonException e) {
     *         logger.<b>suppress</b>("Metadata parse failed, using defaults: " + e.getMessage());
     *         return TrackMetadata.DEFAULT;  // Graceful fallback
     *     }
     * }
     * }</pre>
     *
     * <h3>Типичные сценарии:</h3>
     * <pre>{@code
     * // 1. Cache fallback
     * if (!cache.load(key)) {
     *     logger.suppress("Cache miss, using network: " + key);
     * }
     *
     * // 2. Optional features
     * try {
     *     analytics.send(event);
     * } catch (Exception e) {
     *     logger.suppress("Analytics failed: " + e.getMessage());
     * }
     *
     * // 3. Graceful degradation
     * if (!loadPreviewImage()) {
     *     logger.suppress("Preview image failed");
     * }
     * }</pre>
     *
     * <h3>Внутренняя реализация:</h3>
     * <pre>{@code
     * public <T> void suppress(T msg) {
     *     if (isLoggable(Level.FINER)) {  // Низкий уровень, редко включается
     *         String formatted = "[SUPPRESS] " + format(msg);
     *         appenders.forEach(a -> a.appendSilent(formatted));  // Без алертов
     *     }
     * }
     * }</pre>
     *
     * <h3>Вывод в логах:</h3>
     * <pre>
     * [SUPPRESS] Metadata parse failed, using defaults: Unexpected token
     * [SUPPRESS] Cache miss, using network: track_123
     * [SUPPRESS] Analytics failed: Service unavailable
     * [SUPPRESS] Preview image failed
     * </pre>
     *
     * <h3>Thread-safety:</h3>
     * <p><b>Ожидается synchronized:</b> тихое логирование из любых потоков.</p>
     *
     * @param <T> любой тип: String, Exception, Response, URL
     * @param msg **подавленная** ошибка с успешным fallback'ом
     */
    <T> void suppress(T msg);
    /**
     * <h3>DEBUG с полным stack trace — "print line stack trace"</h3>
     * Выводит сообщение **+ полный stack trace** текущего потока для детальной отладки.
     *
     * <p><b>Назначение:</b> экстренная диагностика в development/staging. "Для мира" = verbose debugging.</p>
     *
     * <h3>Когда использовать:</h3>
     * <table>
     *   <tr><th>print_ln_st (правильно)</th><th>Обычные методы</th></tr>
     *   *   <tr><td>Подозрение на deadlock/concurrency</td><td>Простые логи</td></tr>
     *   <tr><td>Неожиданное состояние программы</td><td>HTTP статусы</td></tr>
     *   <tr><td>Debug сложных алгоритмов</td><td>Performance метрики</td></tr>
     *   <tr><td>Thread dump для анализа</td><td>Routine операции</td></tr>
     * </table>
     *
     * <h3>Request/Response примеры:</h3>
     * <pre>{@code
     * public Response send() {
     *     logger.info("→ " + url);
     *     try {
     *         return doSend();
     *     } catch (Exception e) {
     *         logger.print_ln_st("CRITICAL: unexpected state");  // ← Полный stack!
     *         logger.severe(e);
     *         throw e;
     *     }
     * }
     *
     * public void debugState() {
     *     logger.print_ln_st("=== THREAD DUMP ===");  // Все потоки + состояние
     * }
     * }</pre>
     *
     * <h3>Внутренняя реализация:</h3>
     * <pre>{@code
     * public <T> void print_ln_st(T msg_for_world) {
     *     StringBuilder sb = new StringBuilder();
     *     sb.append("[DEBUG-ST] ").append(msg_for_world).append("\n");
     *
     *     // 1. Сообщение
     *     sb.append("Thread: ").append(Thread.currentThread()).append("\n");
     *
     *     // 2. Полный stack trace
     *     StackTraceElement[] stack = Thread.currentThread().getStackTrace();
     *     for (StackTraceElement ste : stack) {
     *         sb.append("  at ").append(ste).append("\n");
     *     }
     *
     *     log(LEVEL_FINEST, sb.toString());
     * }
     * }</pre>
     *
     * <h3>Вывод в логах:</h3>
     * <pre>
     * [DEBUG-ST] CRITICAL: unexpected state
     * Thread: Thread[main,5,main]
     *   at rf.ebanina.Request.send(Request.java:45)
     *   at rf.ebanina.ApiClient.fetch(ApiClient.java:23)
     *   at rf.ebanina.Main.main(Main.java:12)
     *   at jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method)
     *   ...
     * </pre>
     *
     * <h3>Сравнение с обычным println():</h3>
     * <table>
     *   <tr><th>Метод</th><th>Вывод</th></tr>
     *   <tr><td>{@link #println}</td><td>`CRITICAL: unexpected state`</td></tr>
     *   <tr><td>{@link #print_ln_st}</td><td>`[DEBUG-ST] + 50 строк stack trace`</td></tr>
     * </table>
     *
     * <h3>Использование в production:</h3>
     * <p><b>НЕ использовать!</b> Только development/debug. В продакшене — фильтровать по уровню.</p>
     *
     * @param <T> любой тип (toString() + stack trace)
     * @param msg_for_world сообщение перед stack trace'ем ("для мира" = verbose)
     */
    <T> void print_ln_st(T msg_for_world);
}