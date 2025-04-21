package server;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Type;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Database {
    private final Map<String, JsonElement> database;
    private final String dbFilePath;
    private final ReentrantReadWriteLock lock;
    private final Lock readLock;
    private final Lock writeLock;
    private final Gson gson;

    public Database() {
        this.database = new HashMap<>();
        this.dbFilePath = System.getProperty("user.dir") + "/JSON Database with Java/task/src/server/data/db.json";
        this.lock = new ReentrantReadWriteLock();
        this.readLock = this.lock.readLock();
        this.writeLock = this.lock.writeLock();
        this.gson = new Gson();
        loadDatabase();
    }

    private void loadDatabase() {
        readLock.lock();
        try {
            File dbFile = new File(dbFilePath);
            if (dbFile.exists()) {
                try (BufferedReader reader = Files.newBufferedReader(Paths.get(dbFilePath))) {
                    Type type = new TypeToken<Map<String, JsonElement>>() {
                    }.getType();
                    Map<String, JsonElement> loadedData = gson.fromJson(reader, type);

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
            String rootKey = keyPath[0];
            JsonElement rootElement = database.getOrDefault(rootKey, null);

            if (rootElement == null) {
                if (keyPath.length == 1) {
                    database.put(rootKey, value);
                } else {
                    JsonObject newObj = new JsonObject();
                    database.put(rootKey, newObj);
                    setNestedValue(newObj, Arrays.copyOfRange(keyPath, 1, keyPath.length), value);
                }
            } else {
                if (keyPath.length == 1) {
                    database.put(rootKey, value);
                } else if (rootElement.isJsonObject()) {
                    JsonObject rootObj = rootElement.getAsJsonObject();
                    setNestedValue(rootObj, Arrays.copyOfRange(keyPath, 1, keyPath.length), value);
                } else {
                    responseJson.addProperty("response", "ERROR");
                    responseJson.addProperty("reason", "Cannot set nested property on non-object");
                    return responseJson;
                }
            }

            saveDatabase();
            responseJson.addProperty("response", "OK");
        } finally {
            writeLock.unlock();
        }
        return responseJson;
    }

    private void setNestedValue(JsonObject parent, String[] remainingPath, JsonElement value) {
        if (remainingPath.length == 1) {
            parent.add(remainingPath[0], value);
            return;
        }

        String currentKey = remainingPath[0];
        JsonElement element = parent.get(currentKey);

        if (element == null || !element.isJsonObject()) {
            JsonObject newObj = new JsonObject();
            parent.add(currentKey, newObj);
            element = newObj;
        }

        setNestedValue(element.getAsJsonObject(),
                Arrays.copyOfRange(remainingPath, 1, remainingPath.length),
                value);
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
            if (keyPath.length == 1) {
                if (database.remove(keyPath[0]) != null) {
                    saveDatabase();
                    responseJson.addProperty("response", "OK");
                } else {
                    responseJson.addProperty("response", "ERROR");
                    responseJson.addProperty("reason", "No such key");
                }
                return responseJson;
            }

            JsonElement current = getJsonElement(Arrays.copyOf(keyPath, keyPath.length - 1), false);
            if (current != null && current.isJsonObject()) {
                JsonObject jsonObject = current.getAsJsonObject();
                if (jsonObject.has(keyPath[keyPath.length - 1])) {
                    jsonObject.remove(keyPath[keyPath.length - 1]);
                    saveDatabase();
                    responseJson.addProperty("response", "OK");
                } else {
                    responseJson.addProperty("response", "ERROR");
                    responseJson.addProperty("reason", "No such key");
                }
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

        for (int i = 1; i < keyPath.length; i++) {
            if (current == null) {
                if (createIfNotExist) {
                    current = new JsonObject();
                    return current;
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

