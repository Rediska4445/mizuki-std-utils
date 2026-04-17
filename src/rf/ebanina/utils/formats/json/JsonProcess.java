package rf.ebanina.utils.formats.json;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Iterator;

/**
 * <h1>JsonProcess</h1>
 * Утилиты парсинга JSON для Deezer API без heavyweight библиотек.
 * <p>
 * <b>Зависимость:</b> json-simple (lightweight, regex-free).
 * </p>
 *
 * <h2>Основные методы</h2>
 * <table>
 *   <tr><th>Метод</th><th>Назначение</th><th>Deezer пример</th></tr>
 *   <tr><td>{@link #getRawArray}</td><td>Raw JSONArray</td><td>{@code response.get("data")}</td></tr>
 *   <tr><td>{@link #getJsonArray}</td><td>String[] массив</td><td>{@code tracks[0], artists[0]}</td></tr>
 *   <tr><td>{@link #getJsonItem}</td><td>Поле объекта</td><td>{@code track.get("artist"), album.get("cover")}</td></tr>
 * </table>
 *
 * <h2>Использование в Deezer API</h2>
 * <pre>{@code
 * // Поиск треков
 * String response = api.send().getBody();
 * String data = JsonProcess.getJsonItem(response, "data");     // {"data": [...]}
 * String[] tracks = JsonProcess.getJsonArray(data);           // ["track1", "track2"]
 * String trackJson = tracks[0];                               // первый трек
 *
 * // Обогащение
 * String artistJson = JsonProcess.getJsonItem(trackJson, "artist");
 * String albumJson = JsonProcess.getJsonItem(trackJson, "album");
 * }</pre>
 *
 * <h2>Thread-safety</h2>
 * <p><b>Static методы:</b> stateless, thread-safe.
 * json-simple парсер не мутирует состояние.
 * </p>
 *
 * @see org.json.simple легковесный JSON
 */
public class JsonProcess {
    /**
     * <h3>Парсинг в сырой JSONArray</h3>
     * Прямой возврат json-simple JSONArray без преобразований.
     * <p>
     * <b>Вход:</b> полная JSON строка массива {@code ["item1", "item2"]}.
     * </p>
     * <p>
     * <b>Использование:</b> низкоуровневый доступ к массиву объектов.
     * </p>
     * <p>
     * <b>Пример:</b>
     * <pre>{@code
     * JSONArray rawArray = JsonProcess.getRawArray(jsonData);
     * for(Object item : rawArray) {
     *     JSONObject track = (JSONObject) item;
     *     // Низкоуровневая обработка
     * }
     * }</pre>
     * </p>
     *
     * @param rawJson JSON массив строка
     * @return сырой JSONArray
     * @throws ParseException синтаксическая ошибка JSON
     */
    public static JSONArray getRawArray(String rawJson) throws ParseException {
        return ((JSONArray) (new JSONParser()).parse(rawJson));
    }
    /**
     * <h3>JSONArray → String[] (основной метод)</h3>
     * Преобразует JSON массив в примитивный String массив.
     * <p>
     * <b>Алгоритм:</b>
     * <ol>
     *   <li>{@link #getRawArray} → JSONArray</li>
     *   <li>Создание {@code String[] res = new String[size()]}</li>
     *   <li>Iterator копирование: {@code it.next().toString()}</li>
     * </ol>
     * </p>
     * <p>
     * <b>Ключевой метод Deezer:</b> {@code tracks[0]} для первого результата.
     * </p>
     * <p>
     * <b>Пример:</b>
     * <pre>{@code
     * String data = JsonProcess.getJsonItem(response, "data");
     * String[] tracks = JsonProcess.getJsonArray(data);  // ["track1_json", "track2_json"]
     * String firstTrack = tracks[0];                     // Используется в getTrack()
     * }</pre>
     * </p>
     * <p>
     * <b>Iterator вместо for-each:</b> точный контроль индекса {@code res[i]}.
     * </p>
     *
     * @param rawJson JSON массив строка
     * @return String[] элементов массива
     * @throws ParseException парсинг или пустой массив
     */
    public static String[] getJsonArray(String rawJson) throws ParseException {
        JSONArray itemSearched = getRawArray(rawJson);
        String[] res = new String[itemSearched.size()];

        Iterator it = itemSearched.iterator();

        for(int i = 0; it.hasNext(); i++) {
            res[i] = it.next().toString();
        }

        return res;
    }
    /**
     * <h3>Извлечение поля из JSONObject</h3>
     * Безопасный доступ к ключу с null возвратом.
     * <p>
     * <b>Логика:</b>
     * <ol>
     *   <li>Парсинг → JSONObject</li>
     *   <li>{@code get(item) != null ? toString() : null}</li>
     * </ol>
     * </p>
     * <p>
     * <b>Deezer цепочка:</b>
     * <pre>{@code
     * String artistJson = JsonProcess.getJsonItem(trackJson, "artist");
     * String albumJson = JsonProcess.getJsonItem(trackJson, "album");
     * String preview = JsonProcess.getJsonItem(trackJson, "preview");
     * }</pre>
     * </p>
     * <p>
     * <b>Null-safe:</b> отсутствующие поля → {@code null}.
     * </p>
     *
     * @param rawJson JSON объект строка
     * @param item ключ поля
     * @return значение поля как String или null
     * @throws ParseException не JSONObject
     */
    public static String getJsonItem(String rawJson, String item) throws ParseException {
        Object itemParser = (new JSONParser()).parse(rawJson);
        JSONObject itemObject = (JSONObject) itemParser;

        if(itemObject.get(item) != null) {
            return itemObject.get(item).toString();
        }

        return null;
    }
}
