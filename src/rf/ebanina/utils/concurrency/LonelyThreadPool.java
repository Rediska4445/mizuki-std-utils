package rf.ebanina.utils.concurrency;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

/**
 * <h1>LonelyThreadPool</h1>
 * <p>
 * Класс реализует пул потоков с одной особенностью: в пуле может быть активен только один поток — самый молодой.
 * При добавлении нового задания в пул все предыдущие, если они ещё выполняются,
 * отменяются (останавливаются), и в работе остаётся только новая задача.
 * </p>
 *
 * <h2>Особенности</h2>
 * <ul>
 *   <li>Использует {@link Executors#newSingleThreadExecutor()}, обеспечивающий последовательное выполнение задач в одном потоке.</li>
 *   <li>Каждый вызов {@link #runNewTask(Runnable)} отменяет предыдущую невыполненную задачу через {@link Future#cancel(boolean)}.</li>
 *   <li>Методы синхронизированы для корректного управления текущей задачей {@link #currentTask}.</li>
 *   <li>Метод {@link #shutdown()} останавливает пул и прерывает активные задачи.</li>
 * </ul>
 *
 * <h2>Использование</h2>
 * <pre>{@code
 * AloneThreadPool pool = new AloneThreadPool();
 *
 * // Добавление первой задачи
 * pool.runNewTask(() -> {
 *     // Долгая операция
 * });
 *
 * // Добавление новой задачи — первая отменяется, запускается только новая
 * pool.runNewTask(() -> {
 *     // Новая операция
 * });
 *
 * // Корректное завершение пула при остановке приложения
 * pool.shutdown();
 * }</pre>
 *
 * <h2>Обратите внимание</h2>
 * <ul>
 *   <li>Отмена задачи происходит через {@link Future#cancel(boolean)} — задача должна корректно реагировать на прерывание потока.</li>
 *   <li>Пул не позволяет выполнять несколько задач параллельно — задачи идут строго последовательно.</li>
 *   <li>Класс не реализует интерфейсы ExecutorService напрямую, а предоставляет доступ к внутреннему {@link ExecutorService} через {@link #getExecutor()}.</li>
 * </ul>
 *
 * @author Ebanina Std
 * @since 0.1.4.6-1.1.1
 */
public class LonelyThreadPool
    implements AutoCloseable
{
    /**
     * Внутренний исполнитель задач с одним потоком.
     */
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    /**
     * Текущая выполняющаяся или ожидающая задача в пуле.
     */
    private Future<?> currentTask;

    public synchronized void submit(Runnable task) {
        this.runNewTask(task);
    }

    /**
     * Запускает новую задачу в пуле, отменяя предыдущую, если она ещё не завершена.
     * <p>
     * Этот метод синхронизирован для обеспечения потокобезопасного доступа к {@link #currentTask}.
     * </p>
     *
     * @param task новая задача для выполнения
     */
    public synchronized void runNewTask(Runnable task) {
        if (currentTask != null && !currentTask.isDone()) {
            currentTask.cancel(true);
        }

        currentTask = executor.submit(task);
    }

    /**
     * Запускает новую задачу в пуле, отменяя предыдущую, если она ещё не завершена.
     * <p>
     * Этот метод синхронизирован для обеспечения потокобезопасного доступа к {@link #currentTask}.
     * </p>
     *
     * @param task новая задача для выполнения
     * @param onCancel задача, выполняемая при отмене предыдущей
     */
    public synchronized void runNewTask(Runnable task, Runnable onCancel) {
        if (currentTask != null && !currentTask.isDone()) {
            onCancel.run();

            currentTask.cancel(true);
        }

        currentTask = executor.submit(task);
    }
    /**
     * Немедленно прекращает работу пула и прерывает все выполняющиеся задачи.
     */
    public void shutdown() {
        executor.shutdownNow();
    }
    /**
     * Возвращает внутренний {@link ExecutorService}.
     * <p>
     * Можно использовать для контроля состояния или для интеграции с другими API.
     * </p>
     *
     * @return внутренний исполнитель задач
     */
    public ExecutorService getExecutor() {
        return executor;
    }
    /**
     * Возвращает текущий объект {@link Future}, связанный с выполняемой задачей.
     *
     * @return объект {@link Future} текущей задачи или {@code null}, если задач нет
     */
    public Future<?> getCurrentTask() {
        return currentTask;
    }
    /**
     * Устанавливает текущий объект {@link Future} — используется для тестирования и специальных сценариев.
     *
     * @param currentTask новый объект {@link Future} текущей задачи
     */
    public void setCurrentTask(Future<?> currentTask) {
        this.currentTask = currentTask;
    }
    /**
     * Закрывает пул и прерывает текущую задачу, освобождая ресурсы.
     * <p>
     * Этот метод должен корректно завершать работу пула:
     * </p>
     * <ul>
     *   <li>{@code currentTask.cancel(true)} прерывает выполняющуюся задачу, если она есть и не завершена.</li>
     *   <li>{@code executor.shutdownNow()} инициирует немедленное завершение потока, прерывая задачи и блокируя новые.</li>
     * </ul>
     *
     * @throws Exception при ошибках закрытия
     */
    @Override
    public void close() throws Exception {
        if (currentTask != null) {
            currentTask.cancel(true);
        }

        executor.shutdownNow();

        try {
            if (!executor.awaitTermination(60, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}