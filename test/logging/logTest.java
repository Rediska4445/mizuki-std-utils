package logging;

import rf.ebanina.utils.loggining.Log;

public class logTest {
    private static final Log mocky = new Log();

    public static void main(String[] args) {
        testLevelMap();
    }

    private static void testLevelMap() {
        Log.Level low = new Log.Level().setCode("low");
        Log.Level high = new Log.Level().setCode("high");

        // ------------------------
        mocky.levelMap.put(high, true);
        mocky.levelMap.put(low, false);

        mocky.println(low, "Hi! It's Low level log");
        mocky.println(high, "Hi! It's High level log");

        // ------------------------
        mocky.levelMap.put(high, false);
        mocky.levelMap.put(low, true);

        mocky.println(low, "Hi! It's Low level log");
        mocky.println(high, "Hi! It's High level log");

        // ------------------------
        mocky.levelMap.put(high, false);
        mocky.levelMap.put(low, false);

        mocky.println(low, "Hi! It's Low level log");
        mocky.println(high, "Hi! It's High level log");

        // ------------------------
        mocky.levelMap.put(high, true);
        mocky.levelMap.put(low, true);

        mocky.println(low, "Hi! It's Low level log");
        mocky.println(high, "Hi! It's High level log");
    }
}
