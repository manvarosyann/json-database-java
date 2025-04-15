package server;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Database {
    private final Map<String, JsonElement> database;
    private final String dbFilePath;
    private final ReentrantReadWriteLock lock;
    private final Lock readLock;
    private final Lock writeLock;

    public Database() {
        this.database = new HashMap<>();
        this.dbFilePath = System.getProperty("user.dir") + "/src/server/data/db.json";
        this.lock = new ReentrantReadWriteLock();
        this.readLock = this.lock.readLock();
        this.writeLock = this.lock.writeLock();

        loadDatabase();
    }

    private void loadDatabase() {
        readLock.lock();
        try {
            File dbFile = new File(dbFilePath);
            if (dbFile.exists()) {
                try (BufferedReader reader = Files.newBufferedReader(Paths.get(dbFilePath))) {
                    Gson gson = new Gson();
                    Map<String, JsonElement> loadedData = gson.fromJson(reader, Map.class);
                    if (loadedData != null) {
                        database.putAll(loadedData);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } finally {
            readLock.unlock();
        }
    }

    private void saveDatabase() {
        writeLock.lock();
        try (BufferedWriter writer = Files.newBufferedWriter(Paths.get(dbFilePath))) {
            Gson gson = new Gson();
            gson.toJson(database, writer);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            writeLock.unlock();
        }
    }

    public JsonObject set(String[] keyPath, JsonElement value) {
        JsonObject responseJson = new JsonObject();
        writeLock.lock();
        try {
            JsonElement current = getJsonElement(keyPath, true);

            if (current.isJsonObject()) {
                JsonObject jsonObject = current.getAsJsonObject();
                jsonObject.add(keyPath[keyPath.length - 1], value);
            } else if (current.isJsonArray()) {
                JsonArray jsonArray = current.getAsJsonArray();
                jsonArray.add(value);
            }

            saveDatabase();
            responseJson.addProperty("response", "OK");
        } finally {
            writeLock.unlock();
        }
        return responseJson;
    }

    public JsonObject get(String[] keyPath) {
        JsonObject responseJson = new JsonObject();
        readLock.lock();
        try {
            JsonElement value = getJsonElement(keyPath, false);
            if (value != null) {
                responseJson.addProperty("response", "OK");
                responseJson.add("value", value);
            } else {
                responseJson.addProperty("response", "ERROR");
                responseJson.addProperty("reason", "No such key");
            }
        } finally {
            readLock.unlock();
        }
        return responseJson;
    }

    public JsonObject delete(String[] keyPath) {
        JsonObject responseJson = new JsonObject();
        writeLock.lock();
        try {
            JsonElement current = getJsonElement(keyPath, false);
            if (current != null && current.isJsonObject()) {
                JsonObject jsonObject = current.getAsJsonObject();
                jsonObject.remove(keyPath[keyPath.length - 1]);
                saveDatabase();
                responseJson.addProperty("response", "OK");
            } else {
                responseJson.addProperty("response", "ERROR");
                responseJson.addProperty("reason", "No such key");
            }
        } finally {
            writeLock.unlock();
        }
        return responseJson;
    }

    private JsonElement getJsonElement(String[] keyPath, boolean createIfNotExist) {
        JsonElement current = database.get(keyPath[0]);

        for (int i = 0; i < keyPath.length; i++) {
            if (current == null) {
                if (createIfNotExist) {
                    current = new JsonObject();
                } else {
                    return null;
                }
            }

            if (current.isJsonObject()) {
                current = current.getAsJsonObject().get(keyPath[i]);
            } else if (current.isJsonArray()) {
                current = current.getAsJsonArray().get(Integer.parseInt(keyPath[i])
                );
            }
        }
        return current;
    }

    public JsonObject exit() {
        JsonObject responseJson = new JsonObject();
        responseJson.addProperty("response", "OK");
        System.exit(0);
        return responseJson;
    }
}

