package rf.ebanina.utils.network;

import java.util.Objects;

/**
 * <h3>HTTP Response контейнер для Deezer API</h3>
 * Содержит статус-код и тело ответа (JSON/HTML).
 * <p>
 * <b>Назначение:</b> передача данных от {@link Request#send()} → парсинг JSON.
 * </p>
 *
 * <h3>Основные поля:</h3>
 * <table>
 *   <tr><th>Поле</th><th>Тип</th><th>Описание</th></tr>
 *   <tr><td>{@link #code}</td><td>int</td><td>HTTP статус (200, 404, 500)</td></tr>
 *   <tr><td>{@link #body}</td><td>{@link StringBuilder}</td><td>Raw JSON/HTML тело</td></tr>
 * </table>
 *
 * <h3>Fluent Builder API:</h3>
 * <pre>{@code
 * // В Request.send()
 * return new Response()
 *     .setCode(status)     // 200 OK
 *     .setBody(body);      // {"data":[...]}
 * }</pre>
 *
 * <h3>HashCode/Equals семантика:</h3>
 * <ul>
 *   <li><b>equals:</b> по {@code body.toString()} (контент)</li>
 *   <li><b>hashCode:</b> {@code Objects.hash(body)} (консистентно)</li>
 *   <li><b>HashSet/Map:</b> дедупликация одинаковых JSON ответов</li>
 * </ul>
 *
 * <h3>Жизненный цикл:</h3>
 * <pre>{@code
 * Request.send()
 *   ↓ HttpURLConnection → status + BufferedReader
 *   ↓ new Response().setCode().setBody()
 *   ↓ ApiConsumer: response.getCode(), response.getBody().toString()
 *   ↓ JsonProcess парсит → TrackData/List
 * }</pre>
 *
 * <h3>Thread-safety:</h3>
 * <p><b>Не thread-safe:</b> mutable {@link StringBuilder}.
 * Single-use per request, не для concurrent кэша.
 * </p>
 *
 * <h3>Deezer API пример:</h3>
 * <pre>{@code
 * Response response = request.send();
 * // code = 200
 * // body = {"data":[{"id":"123","title":"Song"}],"total":1}
 *
 * if(response.getCode() == 200) {
 *     String json = response.getBody().toString();
 *     List<TrackData> tracks = JsonProcess.parseTracks(json);
 * }
 * }</pre>
 *
 * <h3>Кэширование:</h3>
 * <pre>{@code
 * Set<Response> cache = new HashSet<>();
 * Response fresh = request.send();
 * if(!cache.contains(fresh)) {  // equals() по body
 *     cache.add(fresh);
 * }
 * }</pre>
 */
public class Response {
    /**
     * <h3>HTTP статус код</h3>
     * Результат {@link java.net.HttpURLConnection#getResponseCode()}.
     * <p>
     * <b>Типичные значения:</b>
     * <ul>
     *   <li>{@code 200} — OK (Deezer API успех)</li>
     *   <li>{@code 404} — трек/артист не найден</li>
     *   <li>{@code 429} — rate limit</li>
     *   <li>{@code 500} — серверная ошибка</li>
     * </ul>
     * </p>
     *
     * @return статус код (100-599)
     */
    private int code;

    /**
     * <h3>Тело HTTP ответа (StringBuilder)</h3>
     * Содержит полный raw ответ от Deezer API сервера.
     * <p>
     * <b>Формирование:</b> BufferedReader → line-by-line append в {@link Request#send()}}.
     * </p>
     * <p>
     * <b>Содержимое:</b>
     * <ul>
     *   <li><b>Успех 200:</b> {@code {"data":[...],"total":1,"next":"..."}} JSON</li>
     *   <li><b>404 Not Found:</b> {@code {"error":{"type":"track_not_found"}}} JSON</li>
     *   <li><b>500 Server Error:</b> JSON/HTML error page</li>
     *   <li><b>Пустой:</b> некоторые errorStream случаи</li>
     * </ul>
     * </p>
     *
     * <h3>Доступ:</h3>
     * <table>
     *   <tr><th>Метод</th><th>Назначение</th></tr>
     *   <tr><td>{@link #getBody()}</td><td>Возврат StringBuilder (shared reference)</td></tr>
     *   <tr><td>{@link #setBody(StringBuilder)}</td><td>Fluent setter (Request.send)</td></tr>
     *   <tr><td>{@code toString()}</td><td>{@code body.toString()} (JSON как текст)</td></tr>
     * </table>
     *
     * <h3>Пример Deezer JSON:</h3>
     * <pre>{@code
     * StringBuilder body = response.getBody();
     * // Содержит:
     * // {
     * //   "data": [{"id":"123","title":"Bohemian Rhapsody","artist":{...}}],
     * //   "total": 1500,
     * //   "next": "https://api.deezer.com/search/track?q=..."
     * // }
     *
     * String json = body.toString();
     * String data = JsonProcess.getJsonItem(json, "data");
     * }</pre>
     *
     * <h3>Mutable shared reference:</h3>
     * <p><b>Внимание:</b> {@link #getBody()} возвращает **тот же** StringBuilder.
     * Изменения видны всем держателям ссылки.
     * </p>
     * <pre>{@code
     * StringBuilder body1 = response.getBody();
     * body1.append(" modified");  // Изменяет оригинал!
     * StringBuilder body2 = response.getBody();  // Видит изменения
     * }</pre>
     *
     * <h3>Thread-safety:</h3>
     * <p><b>Не thread-safe:</b> StringBuilder mutable, нет синхронизации.
     * Response создается один раз per request.
     * </p>
     *
     * <h3>Почему StringBuilder (не String):</h3>
     * <ul>
     *   <li>Эффективное чтение: append без копирования</li>
     *   <li>toString() для JSON парсинга</li>
     *   <li>Fluent setBody() в Request.send()</li>
     * </ul>
     */
    private StringBuilder body;

    public Response() {}

    /**
     * <h3>Тело ответа</h3>
     * StringBuilder с полным HTTP телом (JSON для Deezer).
     * <p>
     * <b>Содержимое:</b> {@code {"data": [...], "total": 1, "next": "..."}}
     * </p>
     *
     * @return StringBuilder (не null)
     */
    public int getCode() {
        return code;
    }
    /**
     * <h3>Статус код setter (Fluent Builder)</h3>
     * Устанавливает HTTP статус из {@link java.net.HttpURLConnection#getResponseCode()}.
     * <p>
     * <b>Контекст:</b> вызывается в {@link Request#send()})}.
     * </p>
     * <p>
     * <b>Цепочка:</b>
     * <pre>{@code
     * return new Response()
     *     .setCode(status)     // 200, 404, 500
     *     .setBody(response);  // JSON body
     * }</pre>
     * </p>
     * <p>
     * <b>Thread-safety:</b> простой setter, не синхронизирован.
     * </p>
     *
     * @param code HTTP статус (100-599)
     * @return this для fluent API
     */
    public Response setCode(int code) {
        this.code = code;
        return this;
    }
    /**
     * <h3>Тело ответа (StringBuilder)</h3>
     * Полное HTTP тело: JSON от Deezer API или error HTML.
     * <p>
     * <b>Содержимое:</b>
     * <ul>
     *   <li>200: {@code {"data": [...], "total": 1}}</li>
     *   <li>404: {@code {"error": {"type": "track_not_found"}}}</li>
     *   <li>500: серверная ошибка JSON/HTML</li>
     * </ul>
     * </p>
     * <p>
     * <b>Использование:</b>
     * <pre>{@code
     * StringBuilder body = response.getBody();
     * String json = body.toString();           // "{\"data\":[...]}"
     * String data = JsonProcess.getJsonItem(json, "data");
     * }</pre>
     * </p>
     * <p>
     * <b>Mutable:</b> возвращает **тот же** StringBuilder (shared reference).
     * </p>
     * <p>
     * <b>Не null:</b> всегда содержит данные (пустой при ошибках).
     * </p>
     *
     * @return StringBuilder с HTTP телом
     */
    public StringBuilder getBody() {
        return body;
    }
    /**
     * <h3>Тело setter (Fluent)</h3>
     * Устанавливает тело из BufferedReader в {@link Request#send()}}.
     *
     * @param body ответ StringBuilder
     * @return this
     */
    public Response setBody(StringBuilder body) {
        this.body = body;
        return this;
    }
    /**
     * <h3>Сравнение Response по содержимому body</h3>
     * Два Response равны, если содержат идентичный JSON/HTML текст.
     * <p>
     * <b>Стандартная реализация Object.equals():</b> reflexive, symmetric, transitive.
     * </p>
     * <p>
     * <b>Логика сравнения:</b>
     * <ul>
     *   <li><code>this == o</code> → reference equality (быстрый возврат)</li>
     *   <li><code>o == null || getClass() != o.getClass()</code> → null/type safety</li>
     *   <li><code>body.toString().equals(response.body.toString())</code> → **контент equality**</li>
     * </li>
     * </ul>
     * </p>
     *
     * <h3>HashCode контракт соблюдён:</h3>
     * <pre>{@code
     * if (r1.equals(r2)) → r1.hashCode() == r2.hashCode()
     * Objects.hash(body).equals(Objects.hash(body))
     * }</pre>
     *
     * <h3>Примеры равенства:</h3>
     * <table>
     *   <tr><th>Сценарий</th><th>equals()</th><th>Причина</th></tr>
     *   <tr><td>Одинаковый JSON (code=200)</td><td><b>true</b></td><td>body.toString() совпадает</td></tr>
     *   <tr><td>Разный code, JSON тот же</td><td><b>true</b></td><td>Игнорирует status code</td></tr>
     *   <tr><td>Разный JSON (пустой vs данные)</td><td><b>false</b></td><td>body.toString() разные</td></tr>
     *   <tr><td>null body vs пустой</td><td><b>false</b></td><td><code>"" != null</code></td></tr>
     * </table>
     *
     * <h3>Кэширование/HashSet использование:</h3>
     * <pre>{@code
     * Set<Response> cache = new HashSet<>();
     * Response fresh = request.send();           // {"data":[...]}
     * Response cached = cache.get(url);          // {"data":[...]}
     *
     * assert fresh.equals(cached);               // true - дубликат
     * assert cache.add(fresh) && !cache.add(cached);  // Один в Set
     * }</pre>
     *
     * <h3>Thread-safety предупреждение:</h3>
     * <p><b>Не thread-safe:</b> {@code body.toString()} на mutable StringBuilder.
     * Используйте synchronized или immutable копии в multi-thread.
     * </p>
     *
     * <h3>Почему НЕ сравнивает code:</h3>
     * <p>HTTP семантика: **одинаковый контент = одинаковый Response**.
     * Status code - metadata, не влияет на JSON данные для парсинга.
     * </p>
     *
     * @param o другой Response для сравнения
     * @return true если body.toString() идентичны
     */
    @Override
    public boolean equals(Object o) {
        if (this == o)
            return true;

        if (o == null || getClass() != o.getClass())
            return false;

        Response response = (Response) o;
        return body.toString().equals(response.body.toString());
    }

    /**
     * <h3>HashCode на основе тела ответа</h3>
     * Генерирует хэш от содержимого {@link #body} (StringBuilder).
     * <p>
     * <b>Логика:</b> {@link Objects#hash(Object...)} делегирует {@code body.hashCode()}.
     * </p>
     * <p>
     * <b>Назначение:</b> поддержка HashSet/HashMap для Response объектов.
     * </p>
     * <p>
     * <b>Семантика:</b> два Response равны, если содержат одинаковое тело ответа.
     * </p>
     * <p>
     * <b>Игнорирует:</b> {@link #code} статус (только body).
     * </p>
     *
     * <h3>Примеры:</h3>
     * <pre>{@code
     * Response r1 = request.send();  // code=200, body="{\"data\":[...]}"
     * Response r2 = cache.get(url);  // code=200, body="{\"data\":[...]}"
     *
     * assert r1.hashCode() == r2.hashCode();  // Одинаковые body → один hash
     * assert new HashSet<>().add(r1) && !new HashSet<>().add(r2);  // Deduplication
     * }</pre>
     *
     * <h3>Производительность:</h3>
     * <table>
     *   <tr><th>Случай</th><th>Сложность</th></tr>
     *   <tr><td>Маленький JSON</td><td>O(1) быстро</td></tr>
     *   <tr><td>Большой JSON</td><td>O(n) пропорционально размеру</td></tr>
     *   <tr><td>Пустой body</td><td>O(1) мгновенно</td></tr>
     * </table>
     *
     * <h3>Thread-safety:</h3>
     * <p><b>Не thread-safe:</b> {@link StringBuilder#hashCode()} читает mutable состояние.
     * Используйте в single-thread контексте или synchronized.
     * </p>
     *
     * <h3>Совместимость с equals:</h3>
     * <p><b>Предполагает:</b> {@link #equals(Object)} тоже по body.
     * Стандартная Java контракция hashCode/equals.
     * </p>
     *
     * @return hashCode от body.toString()
     */
    @Override
    public int hashCode() {
        return Objects.hash(body);
    }

    /**
     * <h3>Автоматическое toString()</h3>
     * Удобный вывод тела ответа (JSON как текст).
     * <p>
     * <b>Логирование:</b>
     * <pre>{@code
     * logger.info("Response: {}", response);
     * System.out.println(response);  // {"data": [...]}
     * }</pre>
     * </p>
     *
     * @return body.toString() (JSON/HTML)
     */
    @Override
    public String toString() {
        return body.toString();
    }
}