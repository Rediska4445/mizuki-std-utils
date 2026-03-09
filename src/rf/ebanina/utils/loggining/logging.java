package rf.ebanina.utils.loggining;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Аннотация для управления логированием на уровне классов и методов.
 * <p>
 * Позволяет задавать ключевые параметры для логирования:
 * </p>
 * <ul>
 *   <li><b>tag</b> — тег для идентификации сообщений, исходящих из класса или метода.</li>
 *   <li><b>fileOut</b> — определяет необходимость вывода сообщений в файл для данного класса или метода.</li>
 *   <li><b>isActive</b> — включает или отключает логирование для помеченного элемента.</li>
 * </ul>
 *
 * <p>Аннотация доступна для применения к классам (TYPE) и методам (METHOD) и сохраняется во время выполнения (RUNTIME).</p>
 *
 * <pre>{@code
 * @logging(tag = "PLAYER", fileOut = true, isActive = true)
 * public class Player {
 *     ...
 * }
 *
 * @logging(isActive = false)
 * public void someMethod() {
 *     // Логирование отключено для этого метода
 * }
 * }</pre>
 *
 * @author Ebanina Std
 * @since 0.0.9
 * @version 1.4.4
 */
@Retention(RUNTIME)
@Target({ElementType.METHOD, ElementType.TYPE})
public @interface logging {

    /**
     * Тег, который будет добавлен к сообщениям логов из данного класса или метода.
     * Позволяет удобнее фильтровать и идентифицировать логи.
     * @return тег в виде строки
     */
    String tag() default "";

    /**
     * Флаг, определяющий нужно ли записывать логи из данного класса/метода в файл.
     * По умолчанию false — логи не записываются в файл.
     * @return true, если нужно писать в файл, иначе false
     */
    boolean fileOut() default false;

    /**
     * Включает или отключает логирование для данного класса или метода.
     * По умолчанию true — логирование активно.
     * @return true, если логирование активно, иначе false
     */
    boolean isActive() default true;
}
