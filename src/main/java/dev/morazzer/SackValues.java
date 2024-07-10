package dev.morazzer;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

public class SackValues {

    private static final List<String> sackableItems = new CopyOnWriteArrayList<>();

    public static void init(JsonObject jsonObject) {
        for (Map.Entry<String, JsonElement> sacks : jsonObject.getAsJsonObject("sacks").entrySet()) {
            final JsonArray contents = sacks.getValue().getAsJsonObject().getAsJsonArray("contents");
            for (JsonElement content : contents) {
                sackableItems.add(content.getAsString());
            }
        }
    }

    public static boolean canBeSacked(String item) {
        return sackableItems.contains(item);
    }

}
