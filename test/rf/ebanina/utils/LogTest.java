package rf.ebanina.utils;

import rf.ebanina.utils.loggining.Log;

public class LogTest {
    public static void main(String[] args) {
        Log g = new Log();

        g.config.put("output_method_signature", "true");

        g.println("FUCK");
    }
}
