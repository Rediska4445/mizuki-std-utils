package rf.ebanina.utils.concurrency;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * <h1>PriorityThreadFactory</h1>
 * Фабрика потоков с настраиваемыми приоритетами, именами и daemon статусом.
 * <p>
 * <b>Реализует:</b> {@link ThreadFactory} интерфейс для {@link java.util.concurrent.Executors}.
 * Предотвращает "thread-0", "pool-1-thread-1" хаос в логах/профайлерах.
 * </p>
 *
 * <h2>Настраиваемые параметры</h2>
 * <table>
 *   <tr><th>Параметр</th><th>Назначение</th><th>Типичные значения</th></tr>
 *   <tr><td>{@code namePrefix}</td><td>Префикс имен потоков</td><td>{@code "api-client"}, {@code "music-downloader"}</td></tr>
 *   <tr><td>{@code priority}</td><td>Приоритет потока (1-10)</td><td>{@code Thread.NORM_PRIORITY}, {@code Thread.MAX_PRIORITY}</td></tr>
 *   <tr><td>{@code isDaemon}</td><td>Daemon статус</td><td>{@code true} (background), {@code false} (user tasks)</td></tr>
 * </table>
 *
 * <h2>Thread lifecycle</h2>
 * <ul>
 *   <li><b>Имена:</b> {@code namePrefix-thread-1, -2, -3...} (уникальные номера)</li>
 *   <li><b>Приоритет:</b> фиксированный для всех потоков фабрики</li>
 *   <li><b>Daemon:</b> все потоки наследуют статус</li>
 *   <li><b>Atomic:</b> {@link AtomicInteger} для thread-safe нумерации</li>
 * </ul>
 *
 * <h2>Использование с Executors</h2>
 * <pre>{@code
 * // API запросы (средний приоритет, daemon)
 * ThreadFactory apiFactory = new PriorityThreadFactory("api-client",
 *     Thread.NORM_PRIORITY, true);
 * ExecutorService apiExecutor = Executors.newFixedThreadPool(4, apiFactory);
 *
 * // UI задачи (высокий приоритет, user)
 * ThreadFactory uiFactory = new PriorityThreadFactory("ui-updater",
 *     Thread.NORM_PRIORITY + 1, false);
 * ScheduledExecutorService uiScheduler = Executors.newScheduledThreadPool(2, uiFactory);
 *
 * // Deezer downloader (низкий приоритет, daemon)
 * ThreadFactory downloadFactory = new PriorityThreadFactory("music-dl",
 *     Thread.MIN_PRIORITY, true);
 * ExecutorService downloader = Executors.newCachedThreadPool(downloadFactory);
 * }</pre>
 *
 * <h2>ThreadDump/Профайлинг</h2>
 * <pre>
 * "api-client-thread-5" #25 daemon prio=5 os_prio=0 tid=0x...
 * "ui-updater-thread-1" #27 prio=6 os_prio=0 tid=0x...
 * "music-dl-thread-12" #32 daemon prio=1 os_prio=0 tid=0x...
 * </pre>
 *
 * <h2>Конструктор</h2>
 * <p>Автоматически добавляет {@code "-thread-"} суффикс к префиксу.
 * {@code new PriorityThreadFactory("api", ...)} → {@code "api-thread-1"}.</p>
 *
 * <h2>Thread-safety</h2>
 * <ul>
 *   <li><b>AtomicInteger:</b> безопасная нумерация</li>
 *   <li><b>Immutable state:</b> namePrefix/priority/daemon final</li>
 *   <li><b>Stateless newThread():</b> только чтение полей</li>
 * </ul>
 *
 * @see java.util.concurrent.Executors#newFixedThreadPool(int, ThreadFactory)
 * @see java.util.concurrent.Executors#newScheduledThreadPool(int, ThreadFactory)
 * @see Thread#setPriority(int)
 * @see Thread#setDaemon(boolean)
 */
public class PriorityThreadFactory
        implements ThreadFactory
{
    /**
     * <h3>Префикс имен потоков</h3>
     * Базовая часть имени: "api-client-thread-1", "music-dl-thread-5".
     * <p>
     * <b>Формирование:</b> конструктор добавляет {@code "-thread-"} + номер.
     * </p>
     * <p>
     * <b>Примеры:</b>
     * <ul>
     *   <li>{@code "api-client"} → "api-client-thread-1"</li>
     *   <li>{@code "ui"} → "ui-thread-1"</li>
     *   <li>{@code "music-dl"} → "music-dl-thread-1"</li>
     * </ul>
     * </p>
     * <p>
     * <b>ThreadDump:</b> читаемые имена вместо "pool-1-thread-X".
     * </p>
     */
    private final String namePrefix;

    /**
     * <h3>Приоритет потоков</h3>
     * Фиксированный приоритет (1-10) для всех создаваемых потоков.
     * <p>
     * <b>Диапазон:</b> {@link Thread#MIN_PRIORITY} (1) до {@link Thread#MAX_PRIORITY} (10).
     * </p>
     * <p>
     * <b>Типичные значения:</b>
     * <ul>
     *   <li>{@link Thread#NORM_PRIORITY} (5) — API запросы</li>
     *   <li>{@link Thread#MAX_PRIORITY} (10) — UI задачи</li>
     *   <li>{@link Thread#MIN_PRIORITY} (1) — фоновые загрузки</li>
     * </ul>
     * </p>
     * <p>
     * <b>Применение:</b> {@code thread.setPriority(priority)} в {@link #newThread(Runnable)}.
     * </p>
     */
    private final int priority;

    /**
     * <h3>Daemon статус</h3>
     * Все потоки наследуют этот статус (background или user).
     * <p>
     * <b>true:</b> daemon потоки (background задачи, не блокируют JVM shutdown).
     * <p>
     * <b>false:</b> user потоки (UI, critical задачи).
     * </p>
     * <p>
     * <b>Применение:</b> {@code thread.setDaemon(isDaemon)}.
     * </p>
     * <p>
     * <b>Примеры:</b>
     * <ul>
     *   <li>{@code true} — API клиенты, downloaders</li>
     *   <li>{@code false} — UI обновления</li>
     * </ul>
     * </p>
     */
    private final boolean isDaemon;

    /**
     * <h3>Счетчик потоков</h3>
     * Thread-safe нумерация: 1, 2, 3... для уникальных имен.
     * <p>
     * <b>AtomicInteger:</b> {@code getAndIncrement()} — безопасно в multi-thread.
     * </p>
     * <p>
     * <b>Именование:</b> {@code namePrefix + threadNumber} → "api-thread-5".
     * </p>
     * <p>
     * <b>Начальное значение:</b> 1 (никаких "thread-0").
     * </p>
     */
    private final AtomicInteger threadNumber = new AtomicInteger(1);
    /**
     * <h3>Создание фабрики потоков</h3>
     * Инициализирует immutable состояние и счетчик.
     * <p>
     * <b>Автоматическое именование:</b> добавляет {@code "-thread-"} суффикс.
     * </p>
     * <p>
     * <b>Примеры:</b>
     * <pre>{@code
     * new PriorityThreadFactory("api", 5, true)
     *     → namePrefix = "api-thread-" (потоки: "api-thread-1", "api-thread-2")
     *
     * new PriorityThreadFactory("ui", 6, false)
     *     → namePrefix = "ui-thread-" (user потоки)
     * }</pre>
     * </p>
     * <p>
     * <b>Валидация:</b> НЕ выполняется (приоритет 1-10 проверяется JVM).
     * </p>
     * <p>
     * <b>Thread-safety:</b> конструктор single-use, поля final.
     * </p>
     *
     * @param namePrefix префикс имен ("api-client", "music-dl")
     * @param priority приоритет 1-10 (Thread.NORM_PRIORITY = 5)
     * @param isDaemon daemon статус (true = background)
     */
    public PriorityThreadFactory(String namePrefix, int priority, boolean isDaemon) {
        this.namePrefix = namePrefix + "-thread-";
        this.priority = priority;
        this.isDaemon = isDaemon;
    }
    /**
     * <h3>Создание нового потока (ThreadFactory контракт)</h3>
     * Основной метод для {@link java.util.concurrent.Executors}.
     * <p>
     * <b>Последовательность:</b>
     * <ol>
     *   <li><b>Имя:</b> {@code namePrefix + threadNumber.getAndIncrement()}</li>
     *   <li><b>Thread:</b> {@code new Thread(r, name)} — с уникальным именем</li>
     *   <li><b>Приоритет:</b> {@code thread.setPriority(priority)}</li>
     *   <li><b>Daemon:</b> {@code thread.setDaemon(isDaemon)}</li>
     *   <li><b>Возврат:</b> готовый Thread для пула</li>
     * </ol>
     * </p>
     * <p>
     * <b>Thread-safety:</b> только {@link AtomicInteger#getAndIncrement()}.
     * </p>
     * <p>
     * <b>Использование:</b>
     * <pre>{@code
     * ExecutorService exec = Executors.newFixedThreadPool(4, apiFactory);
     * exec.submit(() -> api.getTrack("Bohemian Rhapsody"));
     * // Создает: "api-thread-1" prio=5 daemon=true
     * }</pre>
     * </p>
     * <p>
     * <b>Null-safety:</b> {@code r == null} → NPE от {@link Thread#Thread(Runnable, String)}.
     * </p>
     *
     * @param r задача для выполнения
     * @return настроенный Thread с именем/приоритетом/daemon статусом
     */
    @Override
    public Thread newThread(Runnable r) {
        Thread thread = new Thread(r, namePrefix + threadNumber.getAndIncrement());
        thread.setPriority(priority);
        thread.setDaemon(isDaemon);
        return thread;
    }
}