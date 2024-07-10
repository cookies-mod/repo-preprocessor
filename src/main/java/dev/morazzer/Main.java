package dev.morazzer;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import io.github.moulberry.repo.NEURepository;
import io.github.moulberry.repo.NEURepositoryException;
import io.github.moulberry.repo.constants.EssenceCosts;
import io.github.moulberry.repo.data.NEUCraftingRecipe;
import io.github.moulberry.repo.data.NEUForgeRecipe;
import io.github.moulberry.repo.data.NEUIngredient;
import io.github.moulberry.repo.data.NEUItem;
import io.github.moulberry.repo.data.NEURecipe;
import io.github.moulberry.repo.util.NEUId;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;

public class Main {
    final NEURepository neuRepository = NEURepository.of(Path.of("NotEnoughUpdates-REPO"));
    final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public Main() throws NEURepositoryException, IOException {
        SackValues.init(neuRepository.file("constants/sacks.json").json(JsonObject.class));
        MuseumValues.init(neuRepository.file("constants/museum.json").json(JsonObject.class));
        IdMapper.init();
        JsonArray items = new JsonArray();
        JsonArray recipes = new JsonArray();
        neuRepository.reload();
        neuRepository.getItems().getItems().forEach(processItem(this::processItem, items, recipes));

        final Path out = Path.of("out");
        System.out.println(out.toAbsolutePath());
        if (Files.notExists(out)) {
            Files.createDirectories(out);
        }
        write(out.resolve("items.json5"), items);
        write(out.resolve("recipes.json5"), recipes);
    }

    private <T, U, V> BiConsumer<T, V> processItem(ItemConsumer<T, V, U> function, U value, U recipes) {
        return (t, v) -> function.apply(t, v, value, recipes);
    }

    private void processItem(String id, NEUItem neuItem, JsonArray items, JsonArray recipes) {
        if (id.endsWith("NPC")) {
            return;
        }

        JsonObject item = new JsonObject();
        item.add("name", parseStringToText(neuItem.getDisplayName()));
        item.addProperty("internal_id", neuItem.getSkyblockItemId());
        item.addProperty("minecraft_id", IdMapper.getModernIdFromItem(neuItem));

        final List<String> lore = neuItem.getLore();
        String completeLore = String.join("\n", lore).replaceAll("§[A-Fa-f0-9klmnor]", "");
        item.add("lore",
            lore.stream().map(this::parseStringToText).collect(JsonArray::new, JsonArray::add, JsonArray::addAll));

        final String last = lore.getLast().replaceAll("§[A-Fa-f0-9klmnor]", "");
        final String[] lastLineSplit = last.split(" ");

        item.addProperty("tier", lastLineSplit[0]);
        if (lastLineSplit.length > 1) {
            item.addProperty("category", last.substring(lastLineSplit[0].length()).trim());
        }
        addEssenceIfAvailable(id, item);
        item.addProperty("sackable", SackValues.canBeSacked(id));
        item.addProperty("museumable", MuseumValues.isMuseumable(id));

        if (completeLore.contains("* Co-op Soulbound *")) {
            item.addProperty("soulboundtype", "COOP");
        } else if (completeLore.contains("* Soulbound *")) {
            item.addProperty("soulboundtype", "SOULBOUND");
        }

        item.addProperty("rift_transfareable", completeLore.contains("X Rift-Transferable X"));

        neuItem.getRecipes().forEach(processRecipe(this::processRecipe, neuItem, recipes));
        items.add(item);
    }

    private void write(Path path, JsonElement jsonElement) throws IOException {
        Files.writeString(path,
            getLicense() + "\n" + gson.toJson(jsonElement),
            StandardCharsets.UTF_8,
            StandardOpenOption.CREATE,
            StandardOpenOption.TRUNCATE_EXISTING);
    }

    private JsonElement parseStringToText(String value) {
        return GsonComponentSerializer.gson()
            .serializeToTree(LegacyComponentSerializer.legacySection().deserialize(value));
    }

    private void addEssenceIfAvailable(String id, JsonObject items) {
        final Map<@NEUId String, EssenceCosts.EssenceCost> costs =
            neuRepository.getConstants().getEssenceCost().getCosts();
        if (!costs.containsKey(id)) {
            return;
        }

        record EssenceCostValue(int essenceCost, List<String> items) {
            public JsonElement toObject() {
                if (items.isEmpty()) {
                    return new JsonPrimitive(essenceCost);
                }
                final JsonArray jsonElements = new JsonArray();
                jsonElements.add("essence:" + EssenceCostValue.this.essenceCost);
                items.forEach(jsonElements::add);
                return jsonElements;
            }
        }

        final EssenceCosts.EssenceCost essenceCost = costs.get(id);
        JsonObject essence = new JsonObject();
        JsonObject values = new JsonObject();
        essence.addProperty("type", essenceCost.getType());
        essenceCost.getEssenceCosts().forEach((integer, integer2) -> values.add(String.valueOf(integer),
            new EssenceCostValue(integer2,
                essenceCost.getItemCosts().getOrDefault(integer, Collections.emptyList())).toObject()));
        essence.add("values", values);
        items.add("essence", essence);
    }

    private <T, U, X> Consumer<T> processRecipe(RecipeConsumer<T, U, X> function, U value, X recipes) {
        return t -> function.apply(t, value, recipes);
    }

    private void processRecipe(NEURecipe neuRecipe, NEUItem neuItem, JsonArray jsonElements) {
        JsonObject recipe = new JsonObject();
        switch (neuRecipe) {
            case NEUForgeRecipe forgeRecipe -> {
                recipe.addProperty("type", "forge");
                recipe.addProperty("duration", forgeRecipe.getDuration());
                final JsonArray ingredients = new JsonArray();
                for (NEUIngredient input : forgeRecipe.getInputs()) {
                    if (input == NEUIngredient.SENTINEL_EMPTY) {
                        continue;
                    }
                    ingredients.add(formatIngredient(input));
                }
                recipe.add("ingredients", ingredients);
                recipe.addProperty("out", formatIngredient(forgeRecipe.getOutputStack()));
            }
            case NEUCraftingRecipe craftingRecipe -> {
                recipe.addProperty("type", "craft");
                final JsonArray ingredients = new JsonArray();
                for (NEUIngredient input : craftingRecipe.getInputs()) {
                    if (input == NEUIngredient.SENTINEL_EMPTY) {
                        continue;
                    }
                    ingredients.add(formatIngredient(input));
                }
                recipe.add("ingredients", ingredients);
                recipe.addProperty("out", formatIngredient(craftingRecipe.getOutput()));
            }
            default -> recipe = null;
        }
        if (recipe != null) {
            jsonElements.add(recipe);
        }
    }

    private String formatIngredient(NEUIngredient ingredient) {
        if (ingredient.getAmount() > 1) {
            return "%s:%s".formatted(ingredient.getItemId(), (int) ingredient.getAmount());
        }
        return ingredient.getItemId();
    }

    public static void main(String[] args) throws NEURepositoryException, IOException {
        new Main();
    }

    private String getLicense() {
        try {
            final byte[] licenses = this.neuRepository.file("LICENSE").stream().readAllBytes();
            final String licenseString = new String(licenses, StandardCharsets.UTF_8);
            return Arrays.stream(licenseString.split("\n")).map("// %s"::formatted).collect(Collectors.joining("\n"));
        } catch (IOException | NEURepositoryException e) {
            throw new RuntimeException(e);
        }
    }

    interface ItemConsumer<T, U, V> {
        void apply(T t, U u, V v, V v2);
    }

    interface RecipeConsumer<T, U, X> {
        void apply(T t, U u, X x);
    }
}