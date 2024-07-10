package dev.morazzer;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class MuseumValues {

    private static final List<String> museumValues = new CopyOnWriteArrayList<>();

    public static void init(JsonObject jsonObject) {
        for (Map.Entry<String, JsonElement> museum : jsonObject.entrySet()) {
            if (!museum.getValue().isJsonArray()) {
                continue;
            }
            museum.getValue().getAsJsonArray().forEach(museumValue -> museumValues.add(museumValue.getAsString()));
        }
    }

    public static boolean isMuseumable(String item) {
        return museumValues.contains(item);
    }

}
