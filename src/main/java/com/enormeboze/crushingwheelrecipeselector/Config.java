package com.enormeboze.crushingwheelrecipeselector;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import net.neoforged.fml.loading.FMLPaths;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;

public class Config {

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
    private static final File CONFIG_FILE = new File(FMLPaths.CONFIGDIR.get().toFile(), "crushing-recipe-selector.json");

    // Map of input item ID -> desired output item ID
    private static Map<String, String> recipeOverrides = new HashMap<>();

    public static void init() {
        load();
    }

    public static void load() {
        if (!CONFIG_FILE.exists()) {
            // Create default config with example
            recipeOverrides.put("minecraft:example_input", "minecraft:example_output");
            save();
            CrushingWheelRecipeSelector.LOGGER.info("Created default config file at: {}", CONFIG_FILE.getAbsolutePath());
        } else {
            try (FileReader reader = new FileReader(CONFIG_FILE)) {
                Type type = new TypeToken<Map<String, String>>(){}.getType();
                recipeOverrides = GSON.fromJson(reader, type);
                if (recipeOverrides == null) {
                    recipeOverrides = new HashMap<>();
                }
                CrushingWheelRecipeSelector.LOGGER.info("Loaded {} recipe overrides from config", recipeOverrides.size());
            } catch (IOException e) {
                CrushingWheelRecipeSelector.LOGGER.error("Failed to load config file", e);
                recipeOverrides = new HashMap<>();
            }
        }
    }

    public static void save() {
        try (FileWriter writer = new FileWriter(CONFIG_FILE)) {
            GSON.toJson(recipeOverrides, writer);
            CrushingWheelRecipeSelector.LOGGER.info("Saved config file");
        } catch (IOException e) {
            CrushingWheelRecipeSelector.LOGGER.error("Failed to save config file", e);
        }
    }

    public static String getPreferredOutput(String inputItemId) {
        return recipeOverrides.get(inputItemId);
    }

    public static void setPreferredOutput(String inputItemId, String outputItemId) {
        recipeOverrides.put(inputItemId, outputItemId);
        save();
    }

    public static Map<String, String> getAllOverrides() {
        return new HashMap<>(recipeOverrides);
    }
}