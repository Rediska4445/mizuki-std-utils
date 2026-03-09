package rf.ebanina.utils.loggining;

public enum Prefix {
    COLOR_INFO(AnsiColor.GREEN.foreground()),
    COLOR_ERROR(AnsiColor.RED.foreground()),
    COLOR_WARN(AnsiColor.BRIGHT_YELLOW.foreground()),
    COLOR_PROFILER(AnsiColor.RED.foreground()),
    COLOR_SUPPRESS(AnsiColor.RED.foreground()),

    INF_TITLE("INFO"),
    ERROR_TITLE("ERROR"),
    WARN_TITLE("WARN"),
    PROFILER_TITLE("PROFILER"),
    SUPPRESS_TITLE("SUPPRESS"),

    INF(
            COLOR_INFO.getCode() + "[" + AnsiMode.RESET.sequence(false)
                    + INF_TITLE.getCode() + COLOR_INFO.getCode() + "] " + AnsiMode.RESET.sequence(false)
    ),
    ERROR(
            COLOR_ERROR.getCode() + "[" + ERROR_TITLE.getCode()
                    + "] " + AnsiMode.RESET.sequence(false)
    ),
    WARN(
            COLOR_WARN.getCode() + "[" + WARN_TITLE.getCode()
                    + "] " + AnsiMode.RESET.sequence(false) + AnsiMode.ITALIC.sequence(false)
    ),
    PROFILER(
            COLOR_PROFILER.getCode() + "[" + PROFILER_TITLE.getCode()
                    + "] " + AnsiMode.RESET.sequence(false) + AnsiMode.ITALIC.sequence(false)
    ),
    SUPPRESS(
            COLOR_SUPPRESS.getCode() + "[" + AnsiMode.RESET.sequence(false)
                    + SUPPRESS_TITLE.getCode() + COLOR_SUPPRESS.getCode() + "] " + AnsiMode.RESET.sequence(false)
    );

    private final String code;

    Prefix(String code) {
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}