package rf.ebanina.utils.loggining;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * <h1>FileOutputStream</h1>
 * Этот класс расширяет {@link OutputStream} и предназначен для записи потоков байт с возможностью
 * перенаправления вывода одновременно в консоль и в файл с поддержкой предобработки сообщений.
 *
 * <p>Основные возможности:</p>
 * <ul>
 *   <li>Буферизация данных с помощью {@link ByteArrayOutputStream} для групповой обработки перед записью.</li>
 *   <li>Поддержка интерфейса {@link MessageProcessor} для предварительной обработки лог-сообщений перед записью в файл.</li>
 *   <li>Одновременный вывод данных в два независимых потока — консоль и файл.</li>
 *   <li>Умная фильтрация сообщений с учётом аннотаций на вызывающем классе.</li>
 * </ul>
 *
 * <p>Класс реализует переопределённые методы записи {@code write()}, обеспечивая вызов
 * {@code processAndWrite()} при достижении конца строки или превышения размера буфера.</p>
 *
 * <p>Публичные методы {@code flush()} и {@code close()} гарантируют корректную очистку буфера и
 * освобождение системных ресурсов.</p>
 *
 * <p>Класс предназначен для интеграции с кастомным логгером {@code Log} для расширенного
 * контроля вывода лога в приложении.</p>
 *
 * <h3>Пример использования</h3>
 * <pre>{@code
 * FileOutputStream.MessageProcessor processor = (message, clazz) -> {
 *     // Пример обработки строки лога
 *     return message.toUpperCase();
 * };
 *
 * OutputStream console = System.out;
 * OutputStream file = new FileOutputStream("app.log");
 * FileOutputStream fos = new FileOutputStream(console, file, processor);
 *
 * fos.write("Test message\n".getBytes());
 * fos.flush();
 * fos.close();
 * }</pre>
 *
 * @author Ebanina Std
 * @since 0.0.9
 * @version 1.4.4
 * @see OutputStream
 * @see Log
 * @see MessageProcessor
 */
public class FileOutputStream extends OutputStream {
    /**
     * Интерфейс для обработки сообщений лога перед их записью в файл.
     * <p>
     * Это позволяет централизованно изменять формат, фильтровать или модифицировать сообщения,
     * учитывая контекст вызова (например, класс, который логирует).
     * </p>
     *
     * <p>Метод process вызывается при формировании каждой строки лога с передачей:</p>
     * <ul>
     *   <li>{@code message} — исходное сообщение для записи.</li>
     *   <li>{@code callingClass} — класс, из которого вызван лог (для принятия решений и фильтрации).</li>
     * </ul>
     *
     * <p>Возвращаемое значение — преобразованная строка, которая будет записана в файл.
     * Пустая строка означает, что сообщение записано не будет.</p>
     *
     * @author Ebanina Std
     * @since 0.1.4.2
     * @version 1.4.6-1.1.0
     */
    public interface MessageProcessor {
        /**
         * Обрабатывает сообщение лога перед записью в файл.
         *
         * @param message      Исходное сообщение лога.
         * @param callingClass Класс, откуда был вызван лог.
         * @return Обработанная строка для записи в файл (пустая строка означает пропуск записи).
         */
        String process(String message, Class<?> callingClass);
    }

    private final Log logImpl;

    /**
     * Поток вывода с возможностью разделять сообщение между консолью и файлом,
     * а также производить преобразование через {@link MessageProcessor}.
     * <p>
     * Используется для реализации расширенного логирования, где важно,
     * чтобы в консоль шёл оригинальный текст, а в файл — отформатированный.
     * </p>
     *
     * <p>Внутренний буфер накапливает данные до конца строки или достижения лимита по размеру
     * и затем обрабатывает сообщение двумя потоками.</p>
     *
     * @see OutputStream
     * @see MessageProcessor
     */
    private final OutputStream consoleStream;
    /**
     * Поток вывода с возможностью разделять сообщение между консолью и файлом,
     * а также производить преобразование через {@link MessageProcessor}.
     * <p>
     * Используется для реализации расширенного логирования, где важно,
     * чтобы в консоль шёл оригинальный текст, а в файл — отформатированный.
     * </p>
     *
     * <p>Внутренний буфер накапливает данные до конца строки или достижения лимита по размеру
     * и затем обрабатывает сообщение двумя потоками.</p>
     *
     * @see OutputStream
     * @see MessageProcessor
     */
    private final OutputStream fileStream;
    /**
     * Интерфейс обработки сообщений для записи в файл.
     * Позволяет реализовывать различные стратегии фильтрации и форматирования логов,
     * учитывая класс, из которого сделан вызов логирования.
     */
    private final MessageProcessor fileProcessor;
    /**
     * Временный буфер для накопления байтов перед обработкой.
     * Позволяет собрать данные до тех пор, пока не достигнется конец строки или заданный размер,
     * после чего данные передаются на обработку и запись.
     */
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();

    public FileOutputStream(Log logImpl, OutputStream consoleStream, OutputStream fileStream, MessageProcessor fileProcessor) {
        this.logImpl = logImpl;
        this.consoleStream = consoleStream;
        this.fileStream = fileStream;
        this.fileProcessor = fileProcessor;
    }

    /**
     * Записывает один байт в поток вывода.
     * <p>
     * Данные накапливаются во внутреннем буфере {@link ByteArrayOutputStream}.
     * Если байт равен символу новой строки ('\n') или размер буфера превысил 8192 байт,
     * вызывается метод обработки и записи {@link #processAndWrite()}.
     * </p>
     *
     * @param b байт для записи
     * @throws IOException если произошла ошибка ввода/вывода
     */
    @Override
    public void write(int b) throws IOException {
        buffer.write(b);

        if (b == '\n' || buffer.size() > 8192) {
            processAndWrite();
        }
    }

    /**
     * Записывает массив байт в поток вывода с заданными смещением и длиной.
     * <p>
     * Записываемые байты сначала добавляются во внутренний буфер.
     * При обнаружении символа новой строки ('\n') вызывается {@link #processAndWrite()}.
     * </p>
     *
     * @param b   массив байт для записи
     * @param off смещение в массиве
     * @param len количество байт для записи
     * @throws IOException если произошла ошибка ввода/вывода
     */
    @Override
    public void write(byte[] b, int off, int len) throws IOException {
        buffer.write(b, off, len);

        for (int i = off; i < off + len; i++) {
            if (b[i] == '\n') {
                processAndWrite();
                return;
            }
        }
    }

    /**
     * Обрабатывает и записывает накопленные в буфере данные.
     * <p>
     * Преобразует байты во внутреннем буфере в строку с использованием UTF-8.
     * Получает класс, вызвавший логирование.
     * Записывает оригинальное сообщение в консоль и, если настроен {@link MessageProcessor},
     * обрабатывает сообщение и записывает в файл.
     * </p>
     *
     * @throws IOException если произошла ошибка записи
     */
    private void processAndWrite() throws IOException {
        if (buffer.size() == 0) return;

        String message = buffer.toString(StandardCharsets.UTF_8);
        buffer.reset();

        Class<?> callingClass = logImpl.getCallingClass(getClass().getName());

        byte[] consoleBytes = message.getBytes(StandardCharsets.UTF_8);
        consoleStream.write(consoleBytes);

        if (fileProcessor != null) {
            String processedMessage = fileProcessor.process(message, callingClass);

            if (!processedMessage.isEmpty()) {
                byte[] fileBytes = processedMessage.getBytes(StandardCharsets.UTF_8);
                fileStream.write(fileBytes);

                if (message.endsWith("\n")) {
                    fileStream.write('\n');
                }
            }
        }
    }

    /**
     * Очистка внутренних буферов и сброс потоков.
     * <p>
     * Если во внутреннем буфере есть данные, инициирует их обработку и запись.
     * После этого сбрасывает буферы вывода консоли и файла.
     * </p>
     *
     * @throws IOException если произошла ошибка ввода/вывода
     */
    @Override
    public void flush() throws IOException {
        if (buffer.size() > 0) {
            processAndWrite();
        }
        consoleStream.flush();
        fileStream.flush();
    }

    /**
     * Закрывает потоки вывода, предварительно выполнив {@link #flush()}.
     *
     * @throws IOException если произошла ошибка закрытия потоков
     */
    @Override
    public void close() throws IOException {
        flush();
        consoleStream.close();
        fileStream.close();
    }

    public static final MessageProcessor FILE_LOG_PROCESSOR = new MessageProcessor() {
        @Override
        public String process(String message, Class<?> callingClass) {
            logging annotation = callingClass.getAnnotation(logging.class);
            boolean shouldWriteToFile = annotation == null || annotation.fileOut();

            if (!shouldWriteToFile) {
                return "";
            }

            try {
                return processLogLine(cleanAnsiAndFormat(message));
            } catch (Exception e) {
                return message.replaceAll("\\x1B\\[[;\\d]*m", "").trim();
            }
        }

        private String cleanAnsiAndFormat(String msg) {
            msg = msg
                    .replace("[3m", "")
                    .replace("\u001B[0m", "")
                    .replace("\u001B[93m", "")
                    .replace("\u001B[31m", "")
                    .replace("\u001B[32m", "")
                    .trim()
                    .strip();

            return msg;
        }

        public String processLogLine(String logLine) {
            int dateIndex = logLine.lastIndexOf(" :: ");
            if (dateIndex == -1) {
                return logLine;
            }
            String datePart = logLine.substring(dateIndex + 4).trim();

            String withoutDate = logLine.substring(0, dateIndex).trim();

            int lastOpenBracket = withoutDate.lastIndexOf('[');
            int lastCloseBracket = withoutDate.lastIndexOf(']');
            if (lastOpenBracket != -1 && lastCloseBracket != -1 && lastCloseBracket > lastOpenBracket) {
                withoutDate = withoutDate.substring(0, lastOpenBracket).trim();

                if (withoutDate.endsWith("-")) {
                    withoutDate = withoutDate.substring(0, withoutDate.length() - 1).trim();
                }
            }

            int firstCloseBracket = withoutDate.indexOf(']');
            if (withoutDate.startsWith("[") && firstCloseBracket != -1) {
                String level = withoutDate.substring(1, firstCloseBracket).trim();
                String rest = withoutDate.substring(firstCloseBracket + 1).trim();

                return String.format("[%s %s] %s", level, datePart, rest);
            } else {
                return logLine;
            }
        }

    };
}