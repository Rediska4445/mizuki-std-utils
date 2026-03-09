package rf.ebanina.utils.weakly;

import java.io.Serial;
import java.io.Serializable;

/**
 * Класс {@code WeakConst} реализует слабую константу с возможностью одноразового присвоения значения.
 * <p>
 * Значение можно назначить единожды с помощью метода {@link #set(Object)},
 * при повторном вызове будет выброшено исключение {@link IllegalStateException}.
 * Метод {@link #get()} возвращает установленное значение и может вызываться многократно.
 * </p>
 * <p>
 * Использование дженериков {@code <T>} обеспечивает типобезопасность хранимого значения.
 * </p>
 * <p>
 * Пример:
 * <pre>
 * {@code
 * WeakConst<String> wc = new WeakConst<>();
 * wc.set("Hello");
 * String value = wc.get(); // вернёт "Hello"
 * wc.set("World"); // вызовет IllegalStateException
 * }
 * </pre>
 * </p>
 *
 * @param <T> тип значения слабой константы
 * @author Ebanina Std.
 * @version 1.0.0
 * @since 1.4.7
 */
public class WeakConst<T>
    implements Serializable
{
    @Serial
    private static final long serialVersionUID = 2_0_0_1L;

    /**
     * Хранит значение "константы"
     * */
    private T value;

    /**
     *  Флаг, свидетельствующий о том, что значение уже установлено
     *  */
    private boolean isSet = false;

    /**
     * Устанавливает значение, если оно ещё не было установлено.
     *
     * @param value значение для установки
     * @throws IllegalStateException если значение уже было установлено ранее
     */
    public T set(T value) {
        if (!isSet) {
            this.value = value;
            isSet = true;

            return value;
        } else {
            throw new IllegalStateException("Value already set");
        }
    }

    /**
     * Устанавливает значение ТОЛЬКО если оно ещё не было установлено.
     * При повторном вызове просто возвращает уже установленное значение.
     *
     * @param value значение для установки
     * @return установленное значение (новое или существующее)
     */
    public T setIfUnset(T value) {
        if (!isSet) {
            this.value = value;
            isSet = true;
        }
        return this.value;
    }

    /**
     * Возвращает установленное значение.
     *
     * @return значение константы
     * @throws IllegalStateException если значение ещё не было установлено
     */
    public T get() {
        if (!isSet) {
            throw new IllegalStateException("Value is not set yet");
        }

        return value;
    }
}
