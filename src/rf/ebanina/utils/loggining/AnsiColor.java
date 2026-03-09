package rf.ebanina.utils.loggining;

public enum AnsiColor {
    BLACK("30"),
    RED("31"),
    GREEN("32"),
    YELLOW("33"),
    BLUE("34"),
    MAGENTA("35"),
    CYAN("36"),
    WHITE("37"),
    DEFAULT("39"),
    BRIGHT_BLACK("90"),
    BRIGHT_RED("91"),
    BRIGHT_GREEN("92"),
    BRIGHT_YELLOW("93"),
    BRIGHT_BLUE("94"),
    BRIGHT_MAGENTA("95"),
    BRIGHT_CYAN("96"),
    BRIGHT_WHITE("97");

    private final String code;

    AnsiColor(String code) {
        this.code = code;
    }

    public String foreground() {
        return "\033[" + code + "m";
    }

    public String background() {
        int bgCode = Integer.parseInt(code) + (Integer.parseInt(code) < 90 ? 10 : 17);
        return "\033[" + bgCode + "m";
    }
}
