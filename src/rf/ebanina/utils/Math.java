package rf.ebanina.utils;

/**
 * Утилитный класс для математических операций.
 * <p>
 * Данный класс предоставляет статические методы для типичных математических преобразований:
 * <ul>
 *   <li>{@link #scale(double, double, double, double, double)} - масштабирование значения из одного диапазона в другой;</li>
 *   <li>{@link #clamp(double, double, double)} - ограничение значения в заданных границах.</li>
 * </ul>
 * </p>
 *
 * <p>Класс объявлен как финальный и не подлежит наследованию. Все методы статические и вызываются напрямую через класс.</p>
 */
public final class Math {
    /**
     * Масштабирует заданное число из старого диапазона [oldMin, oldMax] в новый диапазон [newMin, newMax].
     * <p>
     * Формула вычисления:
     * <pre>
     * (value - oldMin) / (oldMax - oldMin) * (newMax - newMin) + newMin
     * </pre>
     * </p>
     *
     * @param value  число для масштабирования
     * @param oldMin нижняя граница старого диапазона
     * @param oldMax верхняя граница старого диапазона
     * @param newMin нижняя граница нового диапазона
     * @param newMax верхняя граница нового диапазона
     * @return число, масштабированное в новый диапазон
     * @throws ArithmeticException если oldMax равен oldMin (деление на ноль)
     */
    public static double scale(double value, double oldMin, double oldMax, double newMin, double newMax) {
        if (oldMin == oldMax)
            throw new ArithmeticException("Zero range");

        return ((value - oldMin) / (oldMax - oldMin)) * (newMax - newMin) + newMin;
    }
    /**
     * Ограничивает значение заданными минимальной и максимальной границами.
     * <p>
     * Если значение меньше минимума, возвращается минимум.
     * Если значение больше максимума, возвращается максимум.
     * Иначе возвращается само значение.
     * </p>
     *
     * @param val значение для ограничения
     * @param min нижняя граница
     * @param max верхняя граница
     * @return значение, ограниченное диапазоном [min, max]
     */
    public static double clamp(double val, double min, double max) {
        if (Double.isNaN(val))
            return min;

        return java.lang.Math.max(min, java.lang.Math.min(max, val));
    }
}
