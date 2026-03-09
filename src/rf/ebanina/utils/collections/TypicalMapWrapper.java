package rf.ebanina.utils.collections;

import java.io.Serial;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * <h1>TypicalMapWrapper</h1>
 * <p>
 * Класс-обёртка над {@code Map}, предоставляющий типобезопасное хранение значений любых типов
 * с автоматическим <i>и</i> ручным управлением типами при добавлении и извлечении данных.
 * Более удобная и безопасная альтернатива {@code Map<K, Object>}.
 * </p>
 *
 * <h2>Особенности</h2>
 * <ul>
 *   <li><b>Тип ключа</b> задаётся дженериком {@code <K>} — рекомендуется единый тип для эффективности.</li>
 *   <li><b>Автоматические методы</b> {@link #putAuto(Object, Object)} / {@link #getAuto(Object)}
 *       — определяют тип через {@code value.getClass()} автоматически.</li>
 *   *   <li><b>Методы не синхронизированы</b> — потокобезопасность на стороне клиента.</li>
 *   <li>Переопределены {@link #equals(Object)}, {@link #hashCode()}, {@link #toString()} для корректной работы.</li>
 * </ul>
 *
 * <h2>Использование</h2>
 * <pre>{@code
 * TypicalMapWrapper<String> map = new TypicalMapWrapper<>();
 *
 * // === Автоматическое управление типами ===
 * map.putAuto("name", "KUTE");
 * String name = map.getAuto("name");           // String.class автоматически
 *
 * map.putAuto("age", 30);
 * Integer age = map.getAuto("age");            // Integer.class автоматически
 *
 * // === Ручное управление типами ===
 * map.put("score", 100, Integer.class);
 * Integer score = map.get("score", Integer.class);
 *
 * // === Полиморфизм ===
 * List<String> list = new ArrayList<>();
 * map.putAuto("list", list);
 * List<?> anyList = map.getAuto("list", List.class);  // работает!
 * }</pre>
 *
 * <h2>Методы</h2>
 * <ul>
 *   <li><b>Автоматические:</b>
 *     <ul>
 *       <li>{@link #putAuto(Object, Object)} — сохраняет с автоматическим определением типа</li>
 *       <li>{@link #getAuto(Object)} — возвращает по сохранённому типу из {@code putAuto}</li>
 *       <li>{@link #getAuto(Object, Class)} — приводит к указанному типу ({@code instanceof} проверка)</li>
 *     </ul>
 *   </li>
 *   <li><b>Ручные:</b>
 *     <ul>
 *       <li>{@code <T> void put(K key, T value, Class<T> type)} — добавляет с явным типом</li>
 *       <li>{@code <T> T get(K key, Class<T> type)} — возвращает с проверкой указанного типа</li>
 *     </ul>
 *   </li>
 * </ul>
 *
 * @param <K> тип ключа карты (рекомендуется единый тип: {@code String}, {@code Integer}, enum)
 */
public class TypicalMapWrapper<K>
        implements Serializable
{
    @Serial
    private static final long serialVersionUID = 2_0_0L;

    /**
     * Внутренняя карта для хранения значений вместе с их типами.
     */
    private final Map<K, TypedValue<?>> map = new HashMap<>();

    /**
     * Внутренний класс, хранящий значение и его тип.
     *
     * @param <T> тип значения
     */
    private record TypedValue<T>(T value, Class<T> type) implements Serializable {
        @Serial
        private static final long serialVersionUID = 2_0_1L;

        @Override
        public String toString() {
            return "TypedValue{" +
                    "value=" + value +
                    ", type=" + type +
                    '}';
        }
    }
    /**
     * Возвращает внутреннюю карту ключ-значение.
     * Используйте с осторожностью для специальных случаев.
     *
     * @return внутренняя карта {@code Map<K, TypedValue<?>>}
     */
    public Map<K, TypedValue<?>> getMap() {
        return map;
    }
    /**
     * Добавляет значение с указанным типом по ключу.
     * Явное указание типа значения гарантирует безопасность при извлечении.
     *
     * @param key   ключ, по которому сохраняется значение
     * @param value значение для сохранения
     * @param type  класс типа значения
     * @param <T>   тип значения
     */
    public <T> void put(K key, T value, Class<T> type) {
        map.put(key, new TypedValue<>(value, type));
    }
    /**
     * Возвращает значение по ключу с проверкой типа.
     * Возвращает null, если ключ отсутствует.
     * Бросает {@link ClassCastException}, если тип значения не совпадает.
     *
     * @param key  ключ для поиска
     * @param type ожидаемый тип значения
     * @param <T>  тип значения
     * @return значение указанного типа или null, если ключ не найден
     * @throws ClassCastException при несовпадении типов
     */
    @SuppressWarnings("unchecked")
    public <T> T get(K key, Class<T> type) {
        TypedValue<?> typedValue = map.get(key);

        if (typedValue == null) {
            return null;
        }

        if (!typedValue.type().equals(type)) {
            throw new ClassCastException("Type mismatch for key " + key);
        }

        return (T) typedValue.value();
    }

    /**
     * Добавляет значение по ключу с автоматическим определением типа через instanceof.
     * Тип сохраняется автоматически на основе реального типа {@code value}.
     *
     * @param key   ключ для сохранения
     * @param value значение любого типа
     * @param <T>   автоматически определённый тип значения
     */
    public <T> void putAuto(K key, T value) {
        Class<T> type = (Class<T>) value.getClass();
        map.put(key, new TypedValue<>(value, type));
    }

    /**
     * Возвращает значение по ключу с автоматическим приведением к целевому типу.
     *
     * <p><b>Логика работы:</b></p>
     * <ul>
     *   <li>Если ключ отсутствует — возвращает {@code null}</li>
     *   <li>Если тип значения совместим с {@code target} ({@code target.isInstance(value)})
     *       — возвращает приведённое значение</li>
     *   <li>Если типы несовместимы — выбрасывает {@link ClassCastException} с подробным описанием</li>
     * </ul>
     *
     * <p><b>Пример:</b></p>
     * <pre>{@code
     * map.putAuto("name", "KUTE");
     * String name = map.getAuto("name", String.class); // "KUTE"
     *
     * List<String> list = new ArrayList<>();
     * map.putAuto("list", list);
     * List<?> anyList = map.getAuto("list", List.class); // работает
     * }</pre>
     *
     * @param key    ключ для поиска значения
     * @param target целевой тип для приведения значения ({@code String.class}, {@code Integer.class}, etc.)
     * @param <T>    тип возвращаемого значения
     * @return значение указанного типа или {@code null}, если ключ не найден
     * @throws ClassCastException если тип хранимого значения несовместим с {@code target}
     */
    @SuppressWarnings("unchecked")
    public <T> T getAuto(K key, Class<T> target) {
        TypedValue<?> typedValue = map.get(key);

        if (typedValue == null) {
            return null;
        }

        if (target.isInstance(typedValue.value())) {
            return (T) typedValue.value();
        }

        throw new ClassCastException(
                "Cannot cast %s to %s for key '%s'".formatted(
                        typedValue.value().getClass().getSimpleName(),
                        target.getSimpleName(),
                        key
                )
        );
    }

    /**
     * Возвращает значение по ключу, используя тип, сохранённый при добавлении через {@link #putAuto(K, Object)}.
     *
     * <p><b>Логика работы:</b></p>
     * <ul>
     *   <li>Если ключ отсутствует — возвращает {@code null}</li>
     *   <li>Возвращает значение, приведённое к типу, который был сохранён при {@code putAuto}</li>
     *   <li>Если тип изменился (например, через {@link #put(K, Object, Class)}) — выбрасывает {@link ClassCastException}</li>
     * </ul>
     *
     * <p><b>Пример:</b></p>
     * <pre>{@code
     * map.putAuto("name", "KUTE");
     * String name = map.getAuto("name"); // "KUTE" - тип String.class из putAuto
     *
     * map.putAuto("age", 30);
     * Integer age = map.getAuto("age"); // 30 - тип Integer.class из putAuto
     * }</pre>
     *
     * @param key ключ для поиска значения
     * @param <T> тип значения, определённый при {@link #putAuto(K, Object)}
     * @return значение указанного типа или {@code null}, если ключ не найден
     * @throws ClassCastException если тип значения изменился после putAuto
     */
    @SuppressWarnings("unchecked")
    public <T> T getAuto(K key) {
        TypedValue<?> typedValue = map.get(key);

        if (typedValue == null) {
            return null;
        }

        Class<T> savedType = (Class<T>) typedValue.type();

        if (savedType.isInstance(typedValue.value())) {
            return (T) typedValue.value();
        }

        throw new ClassCastException(
                "Type mismatch for key '%s': expected %s but got %s".formatted(
                        key,
                        savedType.getSimpleName(),
                        typedValue.value().getClass().getSimpleName()
                )
        );
    }

    /**
     * Проверяет равенство объектов по содержимому внутренней карты.
     *
     * @param o объект для сравнения
     * @return true, если обе карты равны по содержимому
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TypicalMapWrapper<?> that = (TypicalMapWrapper<?>) o;
        return map.equals(that.map);
    }
    /**
     * Возвращает хэш-код на основе содержимого внутренней карты.
     *
     * @return хэш-код объекта
     */
    @Override
    public int hashCode() {
        return Objects.hash(map);
    }
    /**
     * Возвращает строковое представление карты с типами значений для удобства отладки.
     *
     * @return строковое представление объекта
     */
    @Override
    public String toString() {
        return "TypicalMapWrapper{" +
                "map=" + map +
                '}';
    }
}