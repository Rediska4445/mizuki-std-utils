package rf.ebanina.utils.loggining;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Modifier;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * <h1>Log</h1>
 * Централизованный фреймворк логирования с расширенными возможностями.
 * <p>
 * Этот класс предоставляет мощную систему ведения логов, выходящую за рамки стандартных решений.
 * Он не просто выводит сообщения, а управляет их потоком, форматированием и фильтрацией на основе
 * контекста выполнения приложения.
 * </p>
 * <p>
 * Ключевые особенности:
 * <ul>
 *   <li><b>Аннотационное управление</b>: поведение логирования (вывод в файл, активность) определяется
 *       аннотацией {@link logging} на уровне класса.</li>
 *   <li><b>Умная фильтрация</b>: поддерживает фильтрацию по классам ({@link #uniqueClasses}) и
 *       условное логирование (например, {@link #println(Supplier, Object)}).</li>
 *   <li><b>Профилирование</b>: встроенный таймер {@link #unix_time_println(Object)} для измерения
 *       производительности с выводом дельты времени.</li>
 *   <li><b>Интерактивность</b>: режим подавления ({@link #suppress(Object)}) превращает логи
 *       в точку останова с возможностью просмотра переменных.</li>
 * </ul>
 * </p>
 * <p>
 * Класс использует переопределение {@link System#out} и {@link System#err} через кастомный
 * {@link FileOutputStream}, что позволяет разделять логику форматирования и вывода.
 * Все публичные методы синхронизированы на {@link #threadLock} для потокобезопасности.
 * </p>
 *
 * <h3>Пример использования</h3>
 * <pre>{@code
 * // Инициализация файлового вывода (один раз при запуске)
 * Log.init_file_log();
 *
 * // Логирование с тегом (если класс аннотирован)
 * Log.info("Плеер запущен");
 *
 * // Профилирование
 * Log.unix_time_println("Начало загрузки трека");
 * // ... загрузка ...
 * Log.unix_time_println("Трек загружен"); // Выведет: 1_630_000_000 (Δ 150 ms) * Трек загружен
 *
 * // Условное логирование
 * Log.println(isDebugEnabled, "Отладочная информация: " + debugData);
 * }</pre>
 *
 * @author Ebanina Std
 * @since 0.0.9
 * @version 1.4.4
 * @see logging
 * @see FileOutputStream
 * @see Prefix
 */
public class Log
        implements ILogging
{
    /**
     * Корневая часть имени пакета логирования, используемая для фильтрации стека вызовов.
     * <p>
     * Определяет границу "внутреннего" кода приложения. Только классы, чьи имена начинаются
     * с этого префикса, считаются нерелевантными для логирования.
     * Это позволяет игнорировать вызовы из системных классов, библиотек и фреймворков.
     * </p>
     *
     * @see #getCallingClass()
     * @since 0.0.9
     */
    public String class_log_name = "Log";
    /**
     * Корневая часть имени пакета приложения, используемая для фильтрации стека вызовов.
     * <p>
     * Определяет границу "внутреннего" кода приложения. Только классы, чьи имена начинаются
     * с этого префикса (например, {@code rf.ebanina}), считаются релевантными для логирования.
     * Это позволяет игнорировать вызовы из системных классов, библиотек и фреймворков.
     * </p>
     *
     * @see #getCallingClass()
     * @since 0.0.9
     */
    public String package_top_name = "rf";
    /**
     * Кэш для хранения тегов логирования, извлеченных из аннотации {@link logging}.
     * <p>
     * Использует {@link ConcurrentHashMap} для обеспечения потокобезопасности.
     * Ключом является {@link Class}, значением — тег из аннотации или {@link #isActive}.
     * </p>
     * <p>
     * Кэширование критически важно для производительности, так как рефлексия (чтение аннотаций)
     * — это относительно медленная операция. Без кэша каждый вызов лога приводил бы к
     * обращению к рефлексии.
     * </p>
     *
     * @see #getLogTagForClass(Class)
     * @since 0.1.0
     */
    private final Map<Class<?>, String> tagCache = new ConcurrentHashMap<>();
    /**
     * Функция-обработчик, вызываемая перед выводом каждого сообщения.
     * <p>
     * Позволяет модифицировать сообщение в реальном времени. Может использоваться
     * для глобальной фильтрации, маскировки данных или преобразования формата.
     * </p>
     * <p>
     * Если значение {@code null}, обработка не производится.
     * </p>
     *
     * @see #print(Prefix, Object)
     * @since 0.0.9
     */
    public Function<Object, Object> onPrint = null;
    /**
     * Специальное значение, указывающее, что логирование для класса отключено.
     * <p>
     * Используется в {@link #tagCache} как маркер. Если аннотация {@link logging}
     * имеет {@code isActive = false}, в кэш записывается это значение.
     * </p>
     * <p>
     * Это позволяет быстро проверить, активно ли логирование для класса, без
     * повторного чтения аннотации.
     * </p>
     *
     * @see #getLogTagForClass(Class)
     * @since 0.1.2
     */
    private final String isActive = "-1";
    /**
     * Объект для синхронизации всех операций логирования.
     * <p>
     * Гарантирует, что сообщения от разных потоков не будут перемешиваться.
     * предпочтительнее, чем синхронизация на {@code this}, так как это
     * предотвращает внешние блокировки.
     * </p>
     *
     * @see #println(Prefix, Object)
     * @see #print(Prefix, Object)
     * @since 0.1.2
     */
    private final Object threadLock = new Object();
    /**
     * Множество классов, для которых разрешено логирование.
     * <p>
     * Если множество не пусто, логирование будет происходить только для классов,
     * присутствующих в этом множестве. Это мощный механизм фильтрации для
     * отладки конкретных компонентов.
     * </p>
     * <p>
     * По умолчанию пусто, что означает разрешение логирования для всех классов.
     * </p>
     *
     * @see #println(Prefix, Object)
     * @since 0.1.3
     */
    public Set<Class<?>> uniqueClasses = new HashSet<>();

    /**
     * Возвращает тег логирования для указанного класса с учётом аннотации {@link logging}.
     * <p>
     * Метод использует кэш {@link #tagCache} для повышения производительности, чтобы
     * избежать повторного чтения аннотации через рефлексию.
     * </p>
     * <p>
     * Помимо проверки аннотации на уровне класса, добавлена поддержка обработки методов:
     * в случае, если вызов происходит из метода с аннотацией {@link logging},
     * учитываются параметры этой аннотации (например, свойства isActive и tag).
     * </p>
     *
     * <p>Логика работы:</p>
     * <ol>
     *   <li>Проверяет кэш и возвращает существующее значение, если есть.</li>
     *   <li>Использует стэк вызовов для определения текущего метода вызова.</li>
     *   <li>Сначала ищет аннотацию {@link logging} на методе, если найдена и isActive=false — возвращает {@link #isActive}.</li>
     *   <li>Если метод помечен тегом — возвращает тег.</li>
     *   <li>Иначе анализирует аннотацию на уровне класса.</li>
     *   <li>Если класс с isActive=false — возвращает {@link #isActive}.</li>
     *   <li>Если класс с тегом — возвращает тег.</li>
     *   <li>Если ничего не найдено — возвращает null.</li>
     * </ol>
     *
     * @param cls класс вызывающего, для которого нужно получить тег
     * @return строка с тегом, {@link #isActive} если логирование отключено, или null если тег не задан
     */
    private String getLogTagForClass(Class<?> cls) {
        return tagCache.computeIfAbsent(cls, c -> {
            try {
                StackTraceElement[] stack = Thread.currentThread().getStackTrace();
                for (StackTraceElement element : stack) {
                    if (element.getClassName().equals(c.getName())) {
                        try {
                            Class<?>[] paramTypes = new Class<?>[0];
                            var method = c.getDeclaredMethod(element.getMethodName(), paramTypes);
                            logging methodAnnotation = method.getAnnotation(logging.class);

                            if (methodAnnotation != null) {
                                if (!methodAnnotation.isActive())
                                    return isActive;
                                if (!methodAnnotation.tag().isEmpty())
                                    return methodAnnotation.tag();
                            }
                        } catch (NoSuchMethodException ignored) {}

                        break;
                    }
                }

                logging annotation = c.getAnnotation(logging.class);
                if (annotation != null) {
                    if (!annotation.isActive())
                        return isActive;
                    if (!annotation.tag().isEmpty()) {
                        return annotation.tag();
                    }
                }
            } catch (Exception ignored) {
            }
            return null;
        });
    }

    private boolean isLoggingActive() {
        Class<?> callingClass = getCallingClass();

        if (callingClass.isAnnotationPresent(logging.class)) {
            logging logging = callingClass.getAnnotation(logging.class);

            return logging.isActive();
        }

        return true;
    }

    /**
     * Определяет класс, инициировавший вызов логирования, по стеку вызовов.
     * <p>
     * Обходит стек вызовов, начиная с вызывающего метода, и ищет первый класс,
     * который:
     * <ul>
     *   <li>Не является самим {@link Log}.</li>
     *   <li>Принадлежит пакету приложения (начинается с {@link #package_top_name}).</li>
     * </ul>
     * </p>
     * <p>
     * <b>Важно</b>: этот метод был модифицирован для исключения {@code FileOutputStream},
     * чтобы корректно определить вызывающий класс при переопределении {@link System#out}.
     * Это "костыль", но необходимый для работы текущей архитектуры.
     * </p>
     *
     * @return найденный класс или {@link Log}.class в случае ошибки
     * @since 0.0.9
     */
    protected Class<?> getCallingClass() {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        for (int i = 2; i < stackTrace.length; i++) {
            String className = stackTrace[i].getClassName();

            if (!className.equals(Log.class.getName()) && className.startsWith(package_top_name)) {
                try {
                    return Class.forName(className);
                } catch (ClassNotFoundException e) {
                    return Log.class;
                }
            }
        }

        return Log.class;
    }
    /**
     * Определяет вызывающий класс, исключая один указанный класс из поиска.
     * <p>
     * Перегрузка {@link #getCallingClass()}, позволяющая игнорировать один
     * конкретный класс (например, промежуточный утилитный класс) при поиске.
     * </p>
     *
     * @param rem имя класса, который нужно исключить из поиска
     * @return найденный класс или {@link Log}.class в случае ошибки
     * @since 0.0.9
     */
    protected Class<?> getCallingClass(String rem) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        for (int i = 2; i < stackTrace.length; i++) {
            String className = stackTrace[i].getClassName();

            if (!className.equals(Log.class.getName()) && className.startsWith(package_top_name) && !className.equals(rem)) {
                try {
                    return Class.forName(className);
                } catch (ClassNotFoundException e) {
                    return Log.class;
                }
            }
        }

        return Log.class;
    }
    /**
     * Определяет вызывающий класс, исключая несколько указанных классов из поиска.
     * <p>
     * Расширенная версия {@link #getCallingClass(String)}, принимающая переменное
     * число имен классов для исключения. Это позволяет игнорировать целую цепочку
     * промежуточных классов.
     * </p>
     *
     * @param rem массив полных имен классов, которые нужно исключить из поиска
     * @return найденный класс или {@link Log}.class в случае ошибки
     * @since 1.0.4.4
     */
    protected Class<?> getCallingClass(String... rem) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();

        for (int i = 2; i < stackTrace.length; i++) {
            String className = stackTrace[i].getClassName();

            for(String r : rem) {
                if (!className.equals(Log.class.getName()) && className.startsWith(package_top_name) && !r.equals(className)) {
                    try {
                        return Class.forName(className);
                    } catch (ClassNotFoundException e) {
                        return Log.class;
                    }
                }
            }
        }

        return Log.class;
    }

    /**
     * Выводит цветовой префикс сообщения.
     * <p>
     * Вспомогательный метод, инкапсулирующий логику вывода ANSI-кодов для цветового
     * выделения уровня логирования. Вызывается в начале формирования каждой записи.
     * </p>
     * <p>
     * <b>Пример</b>: для {@link Prefix#INF} выводит <code>\u001B[32m</code>.
     * </p>
     *
     * @param code строка с ANSI-кодом цвета
     * @see Prefix#getCode()
     * @since 1.0.3
     */
    private void print_prefix(String code) {
        System.out.print(code);
    }

    /**
     * Выводит тег логирования, обрамленный декоративными символами.
     * <p>
     * Вспомогательный метод, отвечающий за визуальное оформление тега. Делает тег
     * более заметным в потоке логов, используя цвет (желтый) и смайлики.
     * </p>
     * <p>
     * <b>Формат вывода</b>: <code>(значок) [тег] (значок)</code>
     * </p>
     *
     * @param line текст тега для вывода
     * @since 1.0.3
     */
    private void print_log_tag(String line) {
        System.out.print("\u001B[93m(* ^ ω ^) / \u001B[0m" + line + "\u001B[93m \\ (^ ω ^ *)\u001B[0m ");
    }

    /**
     * Выводит открывающую скобку для стека вызовов.
     * <p>
     * Вспомогательный метод, формирующий начало блока стека вызовов.
     * </p>
     * <p>
     * <b>Формат вывода</b>: <code> - [</code>
     * </p>
     *
     * @since 1.0.3
     */
    private void print_pre_stack_trace() {
        System.out.print(" - [");
    }

    /**
     * Выводит разделитель между стеком вызовов и временной меткой.
     * <p>
     * Вспомогательный метод, формирующий конец блока стека вызовов и начало
     * блока временной метки.
     * </p>
     * <p>
     * <b>Формат вывода</b>: <code>] :: </code>
     * </p>
     *
     * @since 1.0.3
     */
    private void print_after_stack_trace() {
        System.out.print("] :: ");
    }

    /**
     * Выводит первый релевантный элемент стека вызовов.
     * <p>
     * Вспомогательный метод, отвечающий за фильтрацию и вывод первого элемента
     * стека, который принадлежит пакету приложения и не является частью самого
     * логгера. Это обеспечивает краткое, но информативное указание на источник
     * сообщения.
     * </p>
     *
     * @param stackTraceElement массив элементов стека вызовов
     * @since 1.0.3
     */
    private void print_stack_trace(StackTraceElement[] stackTraceElement) {
        for (StackTraceElement el : stackTraceElement) {
            if (el.toString().toLowerCase().startsWith(package_top_name) && !el.toString().contains(class_log_name)) {
                System.out.print(el);
                break;
            }
        }
    }

    /**
     * Выводит текущую дату и время.
     * <p>
     * Вспомогательный метод, завершающий формирование записи лога. Выводит
     * временную метку, полученную из {@link LocalDateTime#now()}.
     * </p>
     *
     * @param now строковое представление текущей даты и времени
     * @since 1.0.3
     */
    private void print_local_date_now(String now) {
        System.out.print(now);
    }

    public final Map<String, String> config = new ConcurrentHashMap<>();

    /**
     * Читает .properties файл по указанному пути,
     * возвращает значение по ключу path внутри properties,
     * если ключ не найден, возвращает defaultVal.
     * Кэширует результат для ускорения последующих вызовов.
     *
     * @param path путь к .properties файлу
     * @param defaultVal значение по умолчанию, если ключ не найден
     * @return Значение параметра из файла или defaultVal
     */
    public  String getConfigItem(String path, String item, String defaultVal) {
        if (config.containsKey(path)) {
            return config.get(path);
        }

        String value = defaultVal;

        Properties properties = new Properties();
        try (FileInputStream fis = new FileInputStream(path)) {
            properties.load(fis);
            String propValue = properties.getProperty(item);
            if (propValue != null) {
                value = propValue;
            }
        } catch (IOException e) {
            return defaultVal;
        }

        config.put(path, value);

        return value;
    }

    /**
     * Инициализирует систему логирования, привязывая её к файловой системе.
     * <p>
     * Этот метод — отправная точка для долгосрочного хранения логов. Он автоматически:
     * <ol>
     *   <li><b>Генерирует уникальные имена</b>: использует текущее время для создания имени файла
     *       в формате <code>yyyy-MM-dd_HH-mm-ss.SSS</code>, гарантируя, что каждый сеанс
     *       приложения будет иметь свой собственный файл лога.</li>
     *   <li><b>Определяет пути</b>: получает директории для вывода и ошибок из централизованной
     *       карты {@link Log#config}, что позволяет легко настраивать
     *       расположение логов без изменения кода.</li>
     *   <li><b>Запускает процесс</b>: вызывает перегруженную версию {@link #init_file_log(String, String)}
     *       с сгенерированными путями.</li>
     * </ol>
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * public static void main(String[] args) {
     *     // Инициализация логирования при старте приложения
     *     Log.init_file_log();
     *
     *     Log.info("Приложение запущено");
     * }
     * }</pre>
     *
     * <h3>Пример сгенерированного пути</h3>
     * <pre>{@code
     * C:/Users/AppData/Local/Ebanina/logs/out/ - 2025-10-11_20-30-45.123.log
     * }</pre>
     *
     * @see #init_file_log(String, String)
     * @see Properties
     * @since 0.0.9
     */
    public void init_file_log() {
        init_file_log(getConfigItem("config" + File.separator + "log" + File.separator + "log.properties", "output", "logs") + " - " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss.SSS")) + ".log",
                getConfigItem("config" + File.separator + "log" + File.separator + "log.properties",  "errors", "logs") + " - " +
                        LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss.SSS")) + ".log");
    }

    /**
     * Инициализирует систему логирования с полным контролем над путями.
     * <p>
     * Это сердце архитектуры логирования. Метод создаёт "дуплексный" поток вывода,
     * который одновременно отправляет данные в два места:
     * <ul>
     *   <li><b>Консоль (stdout)</b>: для немедленного просмотра разработчиком.
     *       Данные передаются в оригинальный {@link System#out} без изменений,
     *       сохраняя цветовую разметку ANSI.</li>
     *   <li><b>Файл</b>: для долгосрочного хранения и анализа. Данные проходят через
     *       {@link FileOutputStream} с {@link FileOutputStream#FILE_LOG_PROCESSOR},
     *       который очищает их от ANSI-кодов и форматирует для удобочитаемости.</li>
     * </ul>
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * String logOutPath = "logs/app_output.log";
     * String logErrPath = "logs/app_errors.log";
     *
     * Log.init_file_log(logOutPath, logErrPath);
     * Log.info("Логирование настроено вручную");
     * }</pre>
     *
     * <h3>Архитектурная схема</h3>
     * <pre>{@code
     * [Ваш код] -> System.out.println("Hello")
     *     |
     *     V
     * [DualPrintStream] -> (1) System.out (Цветной вывод в консоль)
     *     |
     *     V
     * [FileOutputStream] -> (2) Файл (Очищенный, отформатированный вывод)
     * }</pre>
     *
     * @param out абсолютный путь к файлу для записи стандартного вывода
     * @param err абсолютный путь к файлу для записи ошибок
     * @throws IOException если возникает ошибка при создании директории или файлов
     * @see FileOutputStream
     * @since 0.0.9
     */
    public void init_file_log(String out, String err) {
        try {
            Path path = Paths.get(out);

            if (!Files.exists(path.getParent())) {
                Files.createDirectory(path.getParent());
            }

            PrintStream outLog = new PrintStream(new java.io.FileOutputStream(out));
            PrintStream errLog = new PrintStream(new java.io.FileOutputStream(err));

            PrintStream dualOut = new PrintStream(new FileOutputStream(
                    this, System.out,
                    outLog,
                    FileOutputStream.FILE_LOG_PROCESSOR
            ));

            PrintStream dualErr = new PrintStream(new FileOutputStream(
                    this, System.err,
                    errLog,
                    FileOutputStream.FILE_LOG_PROCESSOR
            ));

            System.setOut(dualOut);
            System.setErr(dualErr);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //TODO: Сделать загрузку через файл
    public final Map<Level, Boolean> levelMap = new HashMap<>(

    );

    public static class Level {
        String code;

        public String getCode() {
            return code;
        }

        public Level setCode(String code) {
            this.code = code;
            return this;
        }
    }

    /**
     * Выводит несколько сообщений для указанного уровня логирования, если этот уровень активен.
     * <p>
     * Важно: происходит приведение массива сообщений к объекту типа Object,
     * поэтому вызов реализуется через метод {@link #println(Object)}.
     * Это позволяет обрабатывать varargs удобно, но с оговорками по типизации.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * Log.println("1", "Сообщение 1", "Сообщение 2", "Сообщение 3");
     * }</pre>
     *
     * @param level уровень логирования, можно указать любой из {@link Log#levelMap}
     * @param msg   один или несколько сообщений для вывода
     * @param <T>   тип сообщения (используется обобщение)
     * @since 1.4.6-1.1.0
     */
    public <T> void println(Level level, T... msg) {
        if(levelMap.getOrDefault(level, true)) {
            println((Object) msg);
        }
    }

    /**
     * Выводит одно сообщение для заданного уровня логирования, если он активен.
     * <p>
     * Если уровень отсутствует в {@link #levelMap}, считается что он активен.
     * Внутри вызывается метод {@link #println(Object)}, который выводит сообщение.
     * </p>
     *
     * @param level уровень логирования (например, "1", "2")
     * @param msg   сообщение для вывода
     * @param <T>   тип сообщения
     * @since 1.4.6-1.1.0
     */
    public <T> void println(Level level, T msg) {
        if(levelMap.getOrDefault(level, true)) {
            println(msg);
        }
    }

    /**
     * Логирует информационное сообщение.
     * <p>
     * Создан как перегрузка для {@link Log#println(Prefix, Object)}
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * public void startServer() {
     *     // ... инициализация ...
     *     Log.info("Сервер запущен на порту " + port);// выведет [INFO] Сервер запущен на порту 8080 - [стёк] :: дата
     *     Log.info("Ожидание подключений..."); // выведет [INFO] Ожидание подключений... - [стёк] :: дата
     * }
     * }</pre>
     *
     * @param msg сообщение, описывающее информационное событие
     * @param <T> тип сообщения (позволяет передавать любые объекты)
     * @see #println(Object)
     * @since 1.0.4.4
     */
    public <T> void info(T msg) {
        println(msg);
    }

    /**
     * Логирует информационное сообщение.
     * <p>
     * Создан как перегрузка для {@link Log#println(Prefix, Object)}
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * public void startServer() {
     *     // ... инициализация ...
     *     Log.info("Сервер запущен на порту " + port);// выведет [INFO] Сервер запущен на порту 8080 - [стёк] :: дата
     *     Log.info("Ожидание подключений..."); // выведет [INFO] Ожидание подключений... - [стёк] :: дата
     * }
     * }</pre>
     *
     * @param msg сообщение, описывающее информационное событие
     * @param msg1 тип сообщения (позволяет передавать любые объекты)
     * @see #println(Object)
     * @since 1.0.4.4
     */
    public void info(String msg, Object... msg1) {
        printf(msg, msg1);
    }

    /**
     * Логирует предупреждение.
     * <p>
     * Удобная обертка для {@link #println(Prefix, Object)} с префиксом {@link Prefix#WARN}.
     * Используется для регистрации нештатных, но не критических ситуаций, которые
     * не приводят к сбою приложения, но требуют внимания.
     * </p>
     * <p>
     * <b>Примеры использования</b>: использование устаревшего API, отсутствие
     * необязательного конфигурационного параметра, медленный ответ внешнего сервиса.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * Log.warn("Конфигурационный параметр 'timeout' не задан, используется значение по умолчанию");
     * }</pre>
     *
     * @param msg сообщение для логирования
     * @param <T> тип сообщения
     * @see #println(Prefix, Object)
     * @since 1.0.4.4
     */
    public <T> void warn(T msg) {
        if(!isLoggingActive())
            return;

        println(Prefix.WARN, msg);
    }

    public void warn(String format, Object... msg) {
        if(!isLoggingActive())
            return;

        print(Prefix.WARN, String.format(format, msg));
    }

    /**
     * Логирует критическую ошибку — "красный флаг", требующий немедленного внимания.
     * <p>
     * Это сигнал о серьезном сбое, который нарушает основную функциональность приложения
     * или его компонентов. Сообщения, залогированные этим методом, должны быть
     * приоритетными для команды поддержки и разработки.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * public void saveUserData(User user) {
     *     try {
     *         database.save(user);
     *     } catch (DatabaseException e) {
     *         Log.severe("Критическая ошибка при сохранении пользователя " + user.getId() + ": " + e.getMessage());
     *         // ... обработка сбоя ...
     *     }
     * }
     * }</pre>
     *
     * @param msg сообщение, описывающее серьезный сбой
     * @param <T> тип сообщения
     * @see #println(Prefix, Object)
     * @see #err(Throwable)
     * @since 1.0.4.4
     */
    public <T> void severe(T msg) {
        if(!isLoggingActive())
            return;

        println(Prefix.ERROR, msg);
    }

    public void severe(String format, Object... msg) {
        if(!isLoggingActive())
            return;

        print(Prefix.ERROR, String.format(format, msg));
    }

    /**
     * Логирует сообщение с временной меткой Unix для целей профилирования и анализа производительности.
     * <p>
     * Этот метод — хронометр. Он интегрирует мощь {@link #unix_time_println(Object)}
     * в основную систему логирования, позволяя вести "дневник времени" прямо в потоке
     * обычных логов. Каждый вызов генерирует уникальную метку времени и рассчитывает
     * дельту с предыдущим вызовом.
     * </p>
     * <p>
     * <b>Применение</b>:
     * <ul>
     *   <li><b>Замер времени выполнения методов</b>: от начала до конца.</li>
     *   <li><b>Анализ задержек</b>: между отправкой запроса и получением ответа.</li>
     *   <li><b>Оптимизация алгоритмов</b>: сравнение времени выполнения разных подходов.</li>
     * </ul>
     * </p>
     *
     * <h3>Пример вывода</h3>
     * <pre>{@code
     * [PROFILER] 1_630_000_000 * Начало загрузки трека
     * [PROFILER] 1_630_000_150 (Δ 150 ms) * Трек загружен
     * }</pre>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * Log.profiler("Начало обработки изображения");
     *
     * BufferedImage processedImage = ImageProcessor.process(originalImage);
     *
     * Log.profiler("Обработка изображения завершена");
     * }</pre>
     *
     * @param msg описательное сообщение, связанное с точкой замера времени
     * @param <T> тип сообщения
     * @see #unix_time_println(Object)
     * @since 1.0.4.4
     */
    public <T> void profiler(T msg) {
        unix_time_println(msg);
    }

    /**
     * Логирует сообщение с полным контекстом: цветом, тегом, стеком и временем.
     * <p>
     * Это основной "рабочий" метод класса. Он объединяет все аспекты логирования
     * в один вызов:
     * <ol>
     *   <li><b>Определение контекста</b>: находит вызывающий класс с помощью {@link #getCallingClass()}.</li>
     *   <li><b>Проверка условий</b>: проверяет, активно ли логирование для этого класса
     *       (через аннотацию и {@link #uniqueClasses}).</li>
     *   <li><b>Формирование сообщения</b>: выводит цветной префикс, тег (если задан),
     *       само сообщение, первый элемент стека вызовов и текущую дату/время.</li>
     *   <li><b>Завершение</b>: добавляет символ новой строки для разделения записей.</li>
     * </ol>
     * </p>
     * <p>
     * Метод — <b>потокобезопасный</b>, синхронизированный на {@link #threadLock},
     * что предотвращает наложение сообщений от разных потоков.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * // Логирование с цветом и автоматическим тегом
     * Log.println(Prefix.WARN, "Ресурс загружен с задержкой");
     *
     * // Логирование с цветом ошибки
     * Log.println(Prefix.ERROR, "Специальное событие произошло");
     * }</pre>
     *
     * @param code цвет и стиль сообщения, определённый в {@link Prefix}
     * @param msg сообщение для логирования; будет преобразовано в строку через <code>toString()</code>
     * @return экземпляр вызывающего класса, если логирование было выполнено, иначе <code>null</code>
     * @see #println(Object)
     * @see #print(Prefix, Object)
     * @since 0.0.9
     */
    public Class<?> println(Prefix code, Object msg) {
        synchronized (threadLock) {
            Class<?> res = print(code, msg);

            if (res != null) {
                if (uniqueClasses.size() > 0) {
                    if (!uniqueClasses.contains(res)) {
                        return res;
                    }
                }

                System.out.println();
            }

            return res;
        }
    }

    /**
     * Условное логирование: "если правда, то логируй".
     * <p>
     * Элегантная альтернатива громоздким <code>if</code>-блокам. Позволяет в одну строку
     * выразить зависимость логирования от булевого условия.
     * </p>
     * <p>
     * <b>Идеальный сценарий</b>: отладочные логи, которые должны быть видны только
     * при включённом режиме отладки.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * boolean debugMode = true;
     * String playerState = "PLAYING";
     *
     * // Чисто и ясно
     * Log.println(debugMode, "Текущее состояние плеера: " + playerState);
     *
     * // Сравните с этим
     * if (debugMode) {
     *     Log.info("Текущее состояние плеера: " + playerState);
     * }
     * }</pre>
     *
     * @param predicate условие, при котором сообщение будет залогировано
     * @param msg сообщение для логирования, если <code>predicate</code> истинно
     * @since 0.0.9
     */
    public void println(Supplier<Boolean> predicate, Object msg) {
        if(predicate.get())
            println(msg);
    }

    /**
     * Логирует сообщение с полным, фильтруемым стеком вызовов.
     * <p>
     * В отличие от других методов, которые показывают лишь "вершину" стека,
     * этот метод раскрывает всю "историю" вызовов, ведущую к точке логирования.
     * </p>
     * <p>
     * <b>Предназначен для</b> глубокой отладки сложных сценариев, когда необходимо
     * понять не только "где", но и "как" программа пришла к определённому состоянию.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * Log.println(Prefix.INFO, "Глубокий вызов", element ->
     *     element.getClassName().startsWith("rf.ebanina"));
     * }</pre>
     *
     * @param code цвет ANSI для основного сообщения
     * @param msg основное сообщение, описывающее событие
     * @param filter предикат для фильтрации элементов стека; если <code>null</code>, выводятся все элементы
     * @since 0.0.9
     */
    public void println(Prefix code, Object msg, Predicate<StackTraceElement> filter) {
        LocalDateTime now = LocalDateTime.now();
        StringBuilder line = new StringBuilder();

        line.append(code.getCode()).append(msg).append(" - [\n");

        StackTraceElement[] stackTraceElements = Thread.currentThread().getStackTrace();

        boolean isEmpty = true;

        for (StackTraceElement el : stackTraceElements) {
            if (filter == null || filter.test(el)) {
                line.append("    at ").append(el).append("\n");
                isEmpty = false;
            }
        }

        line.append("] :: ").append(now).append("\n");

        if(!isEmpty) {
            System.out.print(line);
        }
    }

    /**
     * Время последнего вызова {@code unix_time_println} для расчёта дельты между вызовами.
     */
    private long lastUnixTimePrintlnMillis = -1;

    /**
     * Счетчик для вызываемых без параметров вызовов метода {@code unix_time_println}.
     */
    private long increase = 0;

    /**
     * Список классов, для которых включено логирование unix времени.
     * Если список пустой — логирование разрешено для всех.
     */
    public List<Class<?>> logging_classes = List.of();

    /**
     * Вложенный публичный класс для работы с отметками Unix времени,
     * например, для удобного форматирования времени с разделителями.
     */
    private static class unix_time {
        /**
         * Форматирует число Unix времени, вставляя разделители '_' каждые три цифры.
         * Например: 1000000000 -> "1_000_000_000".
         *
         * @param unixTime число Unix времени для форматирования
         * @return отформатированная строка с подчёркиваниями как разделителями тысяч
         */
        public static String formatUnixTimeWithUnderscores(long unixTime) {
            String s = Long.toString(unixTime);
            StringBuilder sb = new StringBuilder();

            int len = s.length();
            int firstGroupLen = len % 3;
            if (firstGroupLen == 0)
                firstGroupLen = 3;

            sb.append(s, 0, firstGroupLen);

            for (int i = firstGroupLen; i < len; i += 3) {
                sb.append('_').append(s, i, i + 3);
            }

            return sb.toString();
        }
    }

    /**
     * Логирование отметки времени Unix с автоматическим увеличением счетчика.
     */
    public void unix_time_println() {
        unix_time_println(increase++);
    }

    /**
     * Логирует отметку времени Unix с расчётом дельты.
     * <p>
     * Это мощный инструмент для профилирования производительности. Метод:
     * <ol>
     *   <li><b>Фильтрует по классу</b>: проверяет, разрешено ли профилирование для
     *       вызывающего класса через {@link #logging_classes}.
     *   <li><b>Замеряет время</b>: получает текущее время в миллисекундах.</li>
     *   <li><b>Форматирует</b>: преобразует время в строку с разделителями (например, <code>1_630_000_000</code>).</li>
     *   <li><b>Считает дельту</b>: вычисляет разницу с предыдущим вызовом и выводит её как <code>(Δ X ms)</code>.</li>
     *   <li><b>Логирует</b>: выводит отформатированное время, дельту и ваше сообщение.</li>
     * </ol>
     * </p>
     * <p>
     * <b>Идеальное применение</b>: измерение времени загрузки ресурсов, выполнения
     * алгоритмов или ответа сервера.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * Log.unix_time_println("Начало загрузки трека");
     *
     * // ... код загрузки ...
     *
     * Log.unix_time_println("Трек загружен"); // Выведет: 1_630_000_000 (Δ 150 ms) * Трек загружен
     * }</pre>
     *
     * @param msg описательное сообщение, связанное с точкой замера времени
     * @see #unix_time_println()
     * @since 0.0.9
     */
    public <T> void unix_time_println(T msg) {
        if(!isLoggingActive())
            return;

        boolean i = false;

        if(logging_classes.size() > 0) {
            for (Class<?> s : logging_classes) {
                for (StackTraceElement el : Thread.currentThread().getStackTrace()) {
                    if (el.toString().startsWith(package_top_name) && !el.toString().contains(class_log_name)) {
                        if (el.getClassName().equals(s.getName())) {
                            i = true;
                        }

                        break;
                    }
                }
            }
        } else {
            i = true;
        }

        long delta = 0;

        if(i) {
            long currentTimeMillis = System.currentTimeMillis();

            String timeStr = unix_time.formatUnixTimeWithUnderscores(currentTimeMillis);

            String deltaStr = "";
            if (lastUnixTimePrintlnMillis != -1) {
                delta = currentTimeMillis - lastUnixTimePrintlnMillis;
                deltaStr = " (Δ " + delta + " ms)";
            }

            lastUnixTimePrintlnMillis = currentTimeMillis;

            println(Prefix.PROFILER, timeStr + deltaStr + " * " + msg);
        }
    }

    public <T> void suppress(T msg) {
        suppress(true, msg);
    }

    public enum SuppressorCommand {
        EXIT("exit"),
        NEXT_STEP("step next"),
        SET_VAR("set var"),
        GET_VAR("get var"),
        GET_ALL_VARS("get all variables");

        String code;

        public String getCode() {
            return code;
        }

        SuppressorCommand(String code) {
            this.code = code;
        }
    }

    /**
     * Активирует интерактивный режим отладки "подавления".
     * <p>
     * Это уникальная функция, превращающая лог в живую точку останова.
     * Выполнение программы приостанавливается, и пользователь может ввести команду:
     * <ul>
     *   <li><code>exit</code> — немедленно завершает программу с кодом 130.</li>
     *   <li><code>step next</code> — продолжает выполнение.</li>
     *   <li><code>get all variables</code> — использует рефлексию для вывода
     *       всех полей вызывающего класса, что позволяет исследовать его состояние.</li>
     * </ul>
     * </p>
     * <p>
     * <b>Аналогия</b>: как встроенный отладчик, доступный прямо из кода логирования.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * // Условная остановка для отладки
     * Log.suppress(debugMode, "Остановка для отладки");
     *
     * // Или безусловная
     * Log.suppress("Проверка состояния приложения");
     * }</pre>
     *
     * @param predicate условие, при котором активируется режим подавления
     * @param msg сообщение, выводимое пользователю при остановке
     * @see SuppressorCommand
     * @since 0.0.9
     */
    public <T> void suppress(boolean predicate, T msg) {
        if(predicate) {
            Class<?> clazz = println(Prefix.SUPPRESS, msg);

            Scanner sc = new Scanner(System.in);

            while (sc.hasNext()) {
                String z = sc.nextLine();

                if(z.equals(SuppressorCommand.NEXT_STEP.getCode())) {
                    return;
                } if(z.equals(SuppressorCommand.GET_ALL_VARS.getCode())) {
                    java.lang.reflect.Field[] fields = clazz.getDeclaredFields();

                    for (java.lang.reflect.Field field : fields) {
                        field.setAccessible(true);

                        try {
                            String name = field.getName();
                            Object value = field.get(clazz);

                            println(Modifier.toString(field.getModifiers()) + " | " + field.getType() + " | " + name + " = " + value);
                        } catch (IllegalAccessException e) {
                            e.printStackTrace();
                        }
                    }
                } else if(z.startsWith(SuppressorCommand.GET_VAR.getCode())) {
                    try {
                        String varPath = z.split("-")[1].replace(" ", "").trim();
                        println(varPath);

                        Object currentObject = clazz;
                        Class<?> currentClass = clazz instanceof Class ? clazz : clazz.getClass();

                        String[] fields = varPath.split("\\.");

                        for (int i = 0; i < fields.length; i++) {
                            String fieldName = fields[i];

                            java.lang.reflect.Field field = null;
                            Class<?> cls = currentClass;

                            while (cls != null) {
                                try {
                                    field = cls.getDeclaredField(fieldName);
                                    break;
                                } catch (NoSuchFieldException e) {
                                    cls = cls.getSuperclass();
                                }
                            }

                            if (field == null) {
                                break;
                            }

                            field.setAccessible(true);

                            if (i == fields.length - 1) {
                                String modifiers = Modifier.toString(field.getModifiers());
                                Class<?> type = field.getType();
                                Object value = field.get(currentObject);

                                println(String.format("Field: %s\nType: %s\nModifiers: %s\nValue: %s\n",
                                        fieldName, type.getName(), modifiers, value));
                            } else {
                                currentObject = field.get(currentObject);

                                if (currentObject == null) {
                                    break;
                                }

                                currentClass = currentObject.getClass();
                            }
                        }
                    } catch(Exception e) {
                        e.printStackTrace();
                    }
                } else if(z.startsWith(SuppressorCommand.SET_VAR.getCode())) {
                    try {
                        String[] parts = z.split("-")[1].trim().split(" ", 2);

                        if(parts.length < 2) {
                            continue;
                        }

                        String varName = parts[0];
                        String newValStr = parts[1];

                        java.lang.reflect.Field field = clazz.getDeclaredField(varName);
                        field.setAccessible(true);

                        Class<?> type = field.getType();
                        Object newValue = parseValue(type, newValStr);

                        field.set(clazz, newValue);
                        println(varName + " = " + newValStr);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                } else if(z.equals(SuppressorCommand.EXIT.getCode())) {
                    System.exit(130);
                }
            }
        }
    }

    private Object parseValue(Class<?> type, String value) {
        if(type == int.class || type == Integer.class) {
            return Integer.parseInt(value);
        } else if(type == long.class || type == Long.class) {
            return Long.parseLong(value);
        } else if(type == boolean.class || type == Boolean.class) {
            return Boolean.parseBoolean(value);
        } else if(type == double.class || type == Double.class) {
            return Double.parseDouble(value);
        } else if(type == float.class || type == Float.class) {
            return Float.parseFloat(value);
        } else if(type == String.class) {
            return value;
        }

        return null;
    }

    /**
     * Безусловно завершает программу с выводом сообщения.
     * <p>
     * Обёртка вокруг {@link System#exit(int)}, которая сначала логирует сообщение
     * с префиксом {@link Prefix#SUPPRESS}, а затем вызывает завершение.
     * </p>
     * <p>
     * Предназначен для ситуаций, когда дальнейшее выполнение невозможно или нужно что-то посмотреть.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * // Завершение при критической ошибке
     * Log.exit(configFile == null, "Конфигурационный файл не найден");
     *
     * // Или безусловное завершение
     * Log.exit("Завершение по команде пользователя");
     * }</pre>
     *
     * @param predicate условие, при котором программа будет завершена
     * @param msg сообщение, выводимое перед завершением
     * @since 0.0.9
     */
    public void exit(boolean predicate, Object msg) {
        if(predicate) {
            println(Prefix.SUPPRESS, msg);
            System.exit(1);
        }
    }

    /**
     * Выход из программы с выводом сообщения.
     *
     * @param msg сообщение для вывода перед выходом
     */
    public void exit(Object msg) {
        exit(true, msg);
    }

    /**
     * Логирование исключения с трассировкой стека.
     *
     * @param e исключение для логирования
     */
    public void err(Throwable e) {
        stackTrack(Prefix.ERROR, e);
    }

    /**
     * Логирует полный стек-трейс исключения построчно.
     * <p>
     * Каждый элемент стека вызовов ({@link StackTraceElement}) обрабатывается
     * отдельным вызовом {@link #println(Prefix, Object)}, что позволяет применить
     * к нему всю мощь системы логирования: теги, цвета, фильтрацию.
     * </p>
     * <p>
     * Это обеспечивает единообразный вид стек-трейсов во всём приложении.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * try {
     *     riskyOperation();
     * } catch (Exception e) {
     *     Log.stackTrack(e);
     * }
     * }</pre>
     *
     * @param code цвет ANSI для всех строк стек-трейса
     * @param e исключение, стек-трейс которого необходимо вывести
     * @see #err(Throwable)
     * @since 0.0.9
     */
    public  void stackTrack(Prefix code, Throwable e) {
        for(int i = 0; i < e.getStackTrace().length; i++)
            println(code, e.getStackTrace()[i]);
    }

    /**
     * Выводит сообщение с полным, развернутым стеком вызовов.
     * <p>
     * Этот метод — ваш микроскоп для анализа потока выполнения программы. В отличие от
     * стандартных методов логирования, которые показывают лишь "вершину айсберга"
     * (первый релевантный вызов), <code>print_ln_st</code> раскрывает всю "подводную часть",
     * выводя <b>каждый</b> шаг, который привел к текущей точке.
     * </p>
     * <p>
     * Он формирует запись, содержащую:
     * <ol>
     *   <li><b>Цветной префикс и тег</b>: для быстрой визуальной идентификации.</li>
     *   <li><b>Основное сообщение</b>: ваше описание события.</li>
     *   <li><b>Полный стек вызовов</b>: список всех методов, участвующих в цепочке вызовов,
     *       начиная с метода, который вызвал <code>print_ln_st</code>, и заканчивая
     *       корневым методом (например, <code>main</code>).</li>
     *   <li><b>Точную временную метку</b>: для корреляции с другими событиями в логах.</li>
     * </ol>
     * </p>
     * <p>
     * <b>Когда использовать</b>:
     * <ul>
     *   <li>Диагностика сложных багов, где важно понять "путь" программы.</li>
     *   <li>Анализ неожиданного поведения, вызванного глубокими вложенными вызовами.</li>
     *   <li>Понимание работы сторонних библиотек или фреймворков.</li>
     * </ul>
     * </p>
     *
     * <h3>Пример вывода</h3>
     * <pre>{@code
     * [INFO] Глубокий стек вызовов - [
     *     at rf.ebanina.GUI.UI.MainController.loadData(MainController.java:45)
     *     at rf.ebanina.GUI.UI.MainController.lambda$initialize$0(MainController.java:30)
     *     at javafx.base/com.sun.javafx.event.CompositeEventHandler.dispatchBubblingEvent(CompositeEventHandler.java:86)
     *     at javafx.base/com.sun.javafx.event.EventHandlerManager.dispatchBubblingEvent(EventHandlerManager.java:234)
     *     at javafx.base/com.sun.javafx.event.EventHandlerManager.dispatchBubblingEvent(EventHandlerManager.java:191)
     *     at javafx.base/com.sun.javafx.event.BasicEventDispatcher.dispatchEvent(BasicEventDispatcher.java:53)
     *     ...
     * ] :: 2025-10-11T20:45:30.123
     * }</pre>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * public void loadData() {
     *     // Перед выполнением сложной операции
     *     Log.print_ln_st(Prefix.INFO, "Начало загрузки данных");
     *
     *     // ... сложная логика загрузки ...
     *
     *     Log.print_ln_st(Prefix.INFO, "Загрузка данных завершена");
     * }
     * }</pre>
     *
     * @param prefix цвет ANSI для визуального выделения уровня логирования
     * @param msg_for_world основное сообщение, описывающее анализируемое событие
     * @return экземпляр класса, вызвавшего этот метод, для возможной дальнейшей обработки
     * @see #println(Prefix, Object, Predicate)
     * @since 0.0.9
     */
    public Class<?> print_ln_st(Prefix prefix, Object msg_for_world) {
        synchronized (threadLock) {
            LocalDateTime now = LocalDateTime.now();

            Class<?> callerClass = getCallingClass();
            String tag = getLogTagForClass(callerClass);

            if (tag != null) {
                if (tag.equalsIgnoreCase(isActive))
                    return callerClass;
            }

            print_prefix(prefix.getCode());

            if (tag != null) {
                print_log_tag(tag);
            }

            System.out.print(msg_for_world);

            print_pre_stack_trace();

            System.out.println();

            for (StackTraceElement el : Thread.currentThread().getStackTrace()) {
                if (!el.toString().contains(class_log_name)) {
                    System.out.println("        " + el);
                }
            }

            print_after_stack_trace();
            print_local_date_now(now + "\n");

            return callerClass;
        }
    }

    /**
     * Удобная перегрузка {@link #print_ln_st(Prefix, Object)} для логирования уровня INFO.
     * <p>
     * Предоставляет краткий синтаксис для вызова основного метода с предустановленным
     * цветом и стилем {@link Prefix#INF}. Это позволяет сосредоточиться на сути
     * сообщения, не отвлекаясь на указание уровня.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * // Вместо громоздкого вызова
     * Log.print_ln_st(Prefix.INF, "Приложение инициализировано");
     *
     * // Используйте краткую форму
     * Log.print_ln_st("Приложение инициализировано");
     * }</pre>
     *
     * @param msg_for_world сообщение для логирования с уровнем INFO
     * @see #print_ln_st(Prefix, Object)
     * @since 0.0.9
     */
    public <T> void print_ln_st(T msg_for_world) {
        print_ln_st(Prefix.INF, msg_for_world);
    }

    /**
     * Фундаментальный метод для форматированного вывода нескольких объектов.
     * <p>
     * Это "строительный блок", на котором основаны все методы с переменным числом аргументов.
     * Он выполняет полный цикл логирования:
     * <ol>
     *   <li><b>Контекст</b>: определяет вызывающий класс и его тег с помощью {@link #getCallingClass()} и {@link #getLogTagForClass(Class)}.</li>
     *   <li><b>Фильтрация</b>: проверяет, активно ли логирование для этого класса (аннотация, {@link #uniqueClasses}).</li>
     *   <li><b>Форматирование</b>: выводит цветной префикс, тег (если есть), все объекты,
     *       разделяя их заданным <code>sepa</code>, с помощью {@link #smartToString(Object)}.</li>
     *   <li><b>Контекст выполнения</b>: добавляет первый релевантный элемент стека вызовов.</li>
     *   <li><b>Временная метка</b>: завершает запись текущей датой и временем.</li>
     * </ol>
     * </p>
     * <p>
     * <b>Важно</b>: метод не добавляет символ новой строки. Это позволяет строить
     * сложные, многострочные сообщения.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * Log.prints(Prefix.INFO, " | ", "Плеер", "загружен", "версия", "1.4.2.5");
     * // Выведет: [INFO] Плеер | загружен | версия | 1.4.2.5 - [rf.ebanina.ebanina.Main.main(Main.java:10)] :: 2025-10-11T21:00:00.000
     * }</pre>
     *
     * @param code цвет и стиль сообщения
     * @param sepa разделитель, вставляемый между объектами
     * @param msg массив объектов для логирования
     * @return экземпляр вызывающего класса, если логирование не было подавлено
     * @see #println(Prefix, Object...)
     * @see #smartToString(Object)
     * @since 0.0.9
     */
    public Class<?> prints(Prefix code, String sepa, Object... msg) {
        LocalDateTime now = LocalDateTime.now();

        Class<?> callerClass = getCallingClass();
        String tag = getLogTagForClass(callerClass);

        if(tag != null) {
            if (tag.equalsIgnoreCase(isActive))
                return callerClass;
        }

        if(uniqueClasses.size() > 0) {
            if(!uniqueClasses.contains(callerClass)) {
                return callerClass;
            }
        }

        print_prefix(code.getCode());

        if(tag != null){
            print_log_tag(tag);
        }

        System.out.print(smartToString(msg[0]) + sepa);

        for (int i = 1; i < msg.length; i++) {
            System.out.print(
                    (msg[i] != null ? smartToString(msg[i]) : null)
                            + (i != msg.length - 1 ? sepa : "")
            );
        }

        print_pre_stack_trace();
        print_stack_trace(Thread.currentThread().getStackTrace());
        print_after_stack_trace();

        print_local_date_now(now.toString());

        return callerClass;
    }

    /**
     * Безопасно преобразует объект в строку, корректно обрабатывая массивы.
     * <p>
     * Это вспомогательный метод, решающий критическую проблему: стандартный
     * <code>toString()</code> для массивов выводит бесполезное значение вроде
     * <code>[Ljava.lang.String;@1b6d3586</code>.
     * </p>
     * <p>
     * Метод:
     * <ul>
     *   <li>Если объект <code>null</code>, возвращает строку <code>"null"</code>.</li>
     *   <li>Если объект является массивом (любого типа), использует {@link Arrays#deepToString(Object[])}
     *       для рекурсивного преобразования всех элементов в строку и удаляет
     *       обрамляющие квадратные скобки <code>[]</code>.</li>
     *   <li>Во всех остальных случаях вызывает стандартный <code>toString()</code>.</li>
     * </ul>
     * </p>
     *
     * <h3>Примеры преобразования</h3>
     * <pre>{@code
     * smartToString(null) -> "null"
     * smartToString("Hello") -> "Hello"
     * smartToString(new int[]{1, 2, 3}) -> "1, 2, 3"
     * smartToString(new String[]{"a", "b"}) -> "a, b"
     * }</pre>
     *
     * @param obj объект для преобразования в строку
     * @return строковое представление объекта, пригодное для логирования
     * @since 0.0.9
     */
    public  String smartToString(Object obj) {
        if (obj == null)
            return "null";

        Class<?> cls = obj.getClass();

        if (cls.isArray()) {
            return Arrays.deepToString(new Object[]{obj}).replaceAll("^\\[|\\]$", "");
        } else {
            return obj.toString();
        }
    }

    /**
     * Логирует несколько сообщений как одну логическую запись.
     * <p>
     * Удобная обертка над {@link #prints(Prefix, String, Object...)}, которая использует
     * цвет {@link Prefix#INF} и разделитель пробел <code>" "</code> по умолчанию.
     * </p>
     * <p>
     * Идеально подходит для простых случаев, когда не нужны кастомные цвета или разделители.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * Log.println("Пользователь", "вошел", "в", "систему");
     * // Выведет: [INFO] Пользователь вошел в систему - [rf.ebanina.GUI.LoginController.login(LoginController.java:45)] :: 2025-10-11T21:05:00.000
     * }</pre>
     *
     * @param msg сообщения для логирования
     * @since 0.0.9
     */
    public  void println(Prefix code, Object... msg) {
        synchronized (threadLock) {
            Class<?> callerClass = prints(code, " ", msg);

            if(uniqueClasses.size() > 0) {
                if(!uniqueClasses.contains(callerClass)) {
                    return;
                }
            }

            System.out.println();
        }
    }

    /**
     * Логирует несколько сообщений как одну логическую запись.
     * <p>
     * Удобная обертка над {@link #prints(Prefix, String, Object...)}, которая использует
     * цвет {@link Prefix#INF} и разделитель пробел <code>" "</code> по умолчанию.
     * </p>
     * <p>
     * Идеально подходит для простых случаев, когда не нужны кастомные цвета или разделители.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * Log.println("Пользователь", "вошел", "в", "систему");
     * // Выведет: [INFO] Пользователь вошел в систему - [rf.ebanina.GUI.LoginController.login(LoginController.java:45)] :: 2025-10-11T21:05:00.000
     * }</pre>
     *
     * @param msg сообщения для логирования
     * @since 0.0.9
     */
    public  void println(Object... msg) {
        synchronized (threadLock) {
            Class<?> callerClass = Log.class;

            if (msg.length > 0)
                callerClass = prints(Prefix.INF, " ", msg);

            if(uniqueClasses.size() > 0) {
                if(!uniqueClasses.contains(callerClass)) {
                    return;
                }
            }

            System.out.println();
        }
    }

    /**
     * Логирует одно сообщение с уровнем INFO.
     * <p>
     * Самая простая и часто используемая форма логирования. Перегружает метод
     * {@link #println(Object...)} для одного аргумента, обеспечивая единообразие.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * Log.println("Сервер запущен на порту 8080");
     * }</pre>
     *
     * @param msg сообщение для логирования
     * @since 0.1.0
     */
    public <T> void println(T msg) {
        if(!isLoggingActive())
            return;

        println(Prefix.INF, msg);
    }

    /**
     * Форматирует и выводит сообщение, аналогично {@link String#format(String, Object...)}.
     * <p>
     * Предоставляет удобный способ для создания сложных строковых сообщений
     * с подстановкой значений, не требуя явного вызова <code>String.format()</code>.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * String name = "Алексей";
     * int age = 30;
     * Log.printf("Привет, %s! Тебе %d лет.", name, age);
     * }</pre>
     *
     * @param str строка формата
     * @param arr аргументы для подстановки
     * @see String#format(String, Object...)
     * @since 0.1.1
     */
    public void printf(String str, Object... arr) {
        print(String.format(str, arr));
    }
    /**
     * Печатает сообщение с цветом INFO, не добавляя новую строку.
     * <p>
     * Удобная перегрузка {@link #print(Prefix, Object)} для уровня INFO.
     * Позволяет выводить часть сообщения, а затем продолжить его в следующем вызове.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * Log.print("Загрузка: ");
     * for (int i = 0; i < 100; i += 10) {
     *     Thread.sleep(100);
     *     Log.print(i + "% ");
     * }
     * Log.println("Готово!"); // Завершаем новой строкой
     * }</pre>
     *
     * @param msg сообщение для вывода
     * @since 0.0.9
     */
    public void print(Object msg) {
        synchronized (threadLock) {
            print(Prefix.INF, msg);
        }
    }

    /**
     * Печатает сообщение с заданным цветом, не добавляя новую строку.
     * <p>
     * Это основной метод для "сырого" вывода. Он:
     * <ol>
     *   <li>Применяет глобальный обработчик {@link #onPrint}, если он задан.</li>
     *   <li>Преобразует объект в строку с помощью {@link #smartToString(Object)}.</li>
     *   <li>Определяет контекст выполнения (класс, тег).</li>
     *   <li>Проверяет условия подавления логирования.</li>
     *   <li>Выводит цветной префикс, тег (если есть), сообщение, первый элемент стека и время.</li>
     * </ol>
     * </p>
     * <p>
     * <b>Ключевое отличие</b> от <code>println</code>: не добавляет <code>\n</code>.
     * </p>
     *
     * <h3>Пример использования</h3>
     * <pre>{@code
     * Log.print(Prefix.WARN, "Внимание: ");
     * Log.print(Prefix.ERROR, "критическая ошибка!");
     * Log.println(); // Добавляем новую строку вручную
     * }</pre>
     *
     * @param code цвет ANSI для сообщения
     * @param msg сообщение для вывода
     * @return экземпляр вызывающего класса, если логирование не было подавлено
     * @see #print(Object)
     * @since 0.0.9
     */
    public Class<?> print(Prefix code, Object msg) {
        return print(code.getCode(), msg);
    }

    public Class<?> print(String code, Object msg) {
        // Callin' da runnable listener
        if(onPrint != null)
            msg = onPrint.apply(msg);

        // Current time
        LocalDateTime now = LocalDateTime.now();

        // Try print array in normal view
        if(msg instanceof Object[]) {
            msg = Arrays.deepToString((Object[]) (msg));
        } else {
            msg = String.valueOf(msg);
        }

        // Caller class
        Class<?> callerClass = getCallingClass();

        // Check on unique's classes.
        // He's ill not be print, if uniqueClasses contains something
        if(uniqueClasses.size() > 0) {
            if(!uniqueClasses.contains(callerClass)) {
                return callerClass;
            }
        }

        // Tag of classes.
        // Tag - it's just message, which set on annotation, and print before output
        String tag = getLogTagForClass(callerClass);

        if(tag != null) {
            if (tag.equalsIgnoreCase(isActive))
                return null;
        }

        print_prefix(code);

        if(tag != null) {
            print_log_tag(tag);
        }

        if(msg != null) {
            System.out.print(msg);
        } else {
            System.out.print("null");
        }

        print_pre_stack_trace();
        print_stack_trace(Thread.currentThread().getStackTrace());
        print_after_stack_trace();

        print_local_date_now(now.toString());

        return callerClass;
    }
}