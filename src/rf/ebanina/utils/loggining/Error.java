package rf.ebanina.utils.loggining;

import java.time.LocalDateTime;

public class Error {
    private static LocalDateTime now;
    private static String prefix_str = "[ERROR] ";

    private static int date_margin = 5;

    private static final String START = "<=";
    private static final String END = "=>";

    private static int pre(Thread thread, Throwable throwable) {
        now = LocalDateTime.now();

        String name = (" [\u001B[91m" + throwable.getMessage() + "\u001B[0m] ");

        int z_out = getMaxSizeString(thread, throwable);
        int pointer;
        int res;

        System.out.print(START);

        for(pointer = 0; pointer < (((z_out - START.length()) / 2) - throwable.getMessage().length() / 2) - 2; pointer++) {
            System.out.print("-");
        }

        System.out.print(name);

        name = name.replace("\u001B[91m", "").replace("\u001B[0m", "");

        pointer += name.length();

        for(res = pointer; res < z_out - START.length() - 2; res++) {
            System.out.print("-");
        }

        System.out.println(END);

        return res;
    }

    private static void out(int res) {
        System.out.print(START);

        for(int i = 0; i < res; i++) {
            System.out.print("-");
        }

        System.out.print(END + "\n");
    }

    private static int getMaxSizeStringIndex(Thread thread, Throwable throwable) {
        int max_Size_String_Index = 0;

        String str = prefix_str + thread.getName() + " [" + thread.getPriority() + " / " + thread.getState() + "] ";

        int index = 0;

        for(StackTraceElement el1 : throwable.getStackTrace()) {
            if((str + el1.toString()).length() > throwable.getStackTrace()[max_Size_String_Index].toString().length())
                max_Size_String_Index = index;
            index++;
        }

        return max_Size_String_Index;
    }

    private static int getMaxSizeString(Thread thread, Throwable throwable) {
        return (prefix_str + thread.getName() + " [" + thread.getPriority() + " / " + thread.getState() + "] " +
                throwable.getStackTrace()[getMaxSizeStringIndex(thread, throwable)].toString())
                .length();
    }

    public static Thread.UncaughtExceptionHandler __log_init() {
        return (thread, throwable) -> {
            int max_Size_String = getMaxSizeString(thread, throwable);

            int res = pre(thread, throwable);

            for(StackTraceElement el : throwable.getStackTrace()) {
                String prepared_Str = Prefix.ERROR.getCode() + thread.getName() + " [" + thread.getPriority() + " / ";

                if(thread.getState() == Thread.State.RUNNABLE) {
                    prepared_Str += "\u001B[0;32m" + thread.getState() + "\u001B[0m" + "] -> ";
                } else {
                    prepared_Str += "\u001B[0;71m" + thread.getState() + "\u001B[0m" + "] -> ";
                }

                prepared_Str += el;

                System.out.print(prepared_Str);

                for(int i = prepared_Str.length() - 24; i < max_Size_String + date_margin; i++) {
                    System.out.print(" ");
                }

                System.out.print(" :: " + now + "\n");
            }

            out(res);
        };
    }
}
