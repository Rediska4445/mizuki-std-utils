package rf.ebanina.utils.loggining;

public interface ILogging {
    void printf(String str, Object... arr);
    void print(Object msg);

    <T> void println(T msg);

    <T> void info(T msg);
    <T> void warn(T msg);
    <T> void severe(T msg);
    <T> void profiler(T msg);
    <T> void suppress(T msg);
    <T> void print_ln_st(T msg_for_world);
}