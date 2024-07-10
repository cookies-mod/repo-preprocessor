package dev.morazzer;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import io.github.moulberry.repo.data.NEUItem;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;

public class IdMapper {

    private static JsonObject id;
    private static JsonObject registry;

    public static void init() {
        final URL resource = IdMapper.class.getClassLoader().getResource("id_map.json");
        if (resource == null) {
            throw new RuntimeException("Failed to find id_map.json");
        }
        try (InputStream connection = resource.openStream()) {
            JsonObject object =
                new Gson().fromJson(new String(connection.readAllBytes(), StandardCharsets.UTF_8), JsonObject.class);
            id = object.getAsJsonObject("id");
            registry = object.getAsJsonObject("register");
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static String getModernIdFromItem(NEUItem neuItem) {
        if (neuItem.getDamage() != 0) {
            return IdMapper.getModernRegistryName(String.format("%s:%s",
                neuItem.getMinecraftItemId(),
                neuItem.getDamage()));
        } else {
            return IdMapper.getModernRegistryName(neuItem.getMinecraftItemId());
        }

    }

    public static String getModernRegistryName(String oldName) {
        if (!oldName.startsWith("minecraft:")) {
            oldName = "minecraft:" + oldName;
        }
        final JsonElement jsonElement = registry.get(oldName);
        if (jsonElement == null) {
            System.err.println("No mapping found for %s".formatted(oldName));
            return oldName;
        }
        return jsonElement.getAsString();
    }
}
