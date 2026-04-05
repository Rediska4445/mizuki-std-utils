package rf.ebanina.utils.formats.json;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

import java.util.Iterator;

public class JsonProcess {
    public static JSONArray getRawArray(String rawJson) throws ParseException {
        return ((JSONArray) (new JSONParser()).parse(rawJson));
    }

    public static String[] getJsonArray(String rawJson) throws ParseException {
        JSONArray itemSearched = getRawArray(rawJson);
        String[] res = new String[itemSearched.size()];

        Iterator it = itemSearched.iterator();

        for(int i = 0; it.hasNext(); i++) {
            res[i] = it.next().toString();
        }

        return res;
    }

    public static String getJsonItem(String rawJson, String item) throws ParseException {
        Object itemParser = (new JSONParser()).parse(rawJson);
        JSONObject itemObject = (JSONObject) itemParser;

        if(itemObject.get(item) != null) {
            return itemObject.get(item).toString();
        }

        return null;
    }
}
