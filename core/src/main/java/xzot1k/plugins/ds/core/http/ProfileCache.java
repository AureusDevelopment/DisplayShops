package xzot1k.plugins.ds.core.http;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import org.jetbrains.annotations.NotNull;
import xzot1k.plugins.ds.DisplayShops;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ProfileCache {
    private final DisplayShops INSTANCE;
    private final Gson gson;
    private JsonObject cache;

    public ProfileCache(@NotNull DisplayShops instance) {
        this.INSTANCE = instance;
        gson = new GsonBuilder().setPrettyPrinting().create();

        File file = new File(INSTANCE.getDataFolder(), "cache.json");
        if (!file.exists()) {
            try {
                file.createNewFile();
            } catch (IOException e) {e.printStackTrace();}
        }

        try (FileReader reader = new FileReader(file.getPath())) {
            JsonElement element = getGson().fromJson(reader, JsonObject.class);

            if (element == null) cache = new JsonObject();
            else cache = element.getAsJsonObject();
        } catch (IOException e) {e.printStackTrace();}
    }

    public JsonObject getProfile(@NotNull UUID uuid) {

        JsonElement element = getCache().get(uuid.toString().replace("-", ""));
        if (element != null) {
            JsonObject object = element.getAsJsonObject();
            if (object != null) {
                final long difference = ((System.currentTimeMillis() - object.get("timestamp").getAsLong()) / 1000);
                if (difference < 1800) return object;
            }
        }

        getCache().remove(uuid.toString());
        CompletableFuture<JsonObject> futureCache = CompletableFuture.supplyAsync(() -> {
            JsonObject simplifiedObject = new JsonObject();
            try {
                final String requestString = new HttpRequest("https://api.minetools.eu/profile/" + uuid).build();
                JsonObject responseJSON = getGson().fromJson(requestString, JsonObject.class).get("decoded").getAsJsonObject();

                final String id = responseJSON.get("profileId").getAsString(),
                        name = responseJSON.get("profileName").getAsString(),
                        url = responseJSON.get("textures").getAsJsonObject().get("SKIN").getAsJsonObject().get("url").getAsString();

                simplifiedObject.addProperty("name", name);
                simplifiedObject.addProperty("url", url);
                simplifiedObject.addProperty("timestamp", System.currentTimeMillis());

                getCache().add(id, simplifiedObject);
            } catch (Exception e) {e.printStackTrace();}
            return simplifiedObject;
        });

        futureCache.thenRunAsync(this::save);

        try {
            return futureCache.get(5, TimeUnit.SECONDS);
        } catch (Exception ignored) {}
        return null;
    }

    public void save() {
        try (FileWriter writer = new FileWriter(INSTANCE.getDataFolder() + "/cache.json")) {
            getGson().toJson(getCache(), writer);
        } catch (IOException e) {e.printStackTrace();}
    }

    public JsonObject getCache() {
        return cache;
    }

    public Gson getGson() {
        return gson;
    }
}