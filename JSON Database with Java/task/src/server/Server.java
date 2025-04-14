package server;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonPrimitive;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Server {
    private Database database;
    private final String address;
    private final int port;
    private final ExecutorService executor;

    public Server() {
        address = "127.0.0.1";
        port = 23456;
        database = new Database();
        executor = Executors.newFixedThreadPool(10);
    }

    public void startServer() {
        try (ServerSocket server = new ServerSocket(port, 50, InetAddress.getByName(address))) {
            System.out.println("Server started!");

            while (true) {
                try (Socket socket = server.accept()) {
                    executor.submit(() -> handleClient(socket));
                } catch (IOException e) {
                    System.out.println("Error: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private void handleClient(Socket socket) {
        try (BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);

            String command = in.readLine();
            if (command != null) {
                JsonObject request = JsonParser.parseString(command).getAsJsonObject();
                String action = request.get("type").getAsString();
                String key = request.get("key").getAsString();
                JsonObject response = new JsonObject();

                switch (action) {
                    case "set":
                        String value = request.get("value").getAsString();
                        JsonElement valueJson = new JsonObject();
                        valueJson = new JsonPrimitive(value);
                        response = database.set(new String[]{key}, valueJson);
                        break;
                    case "get":
                        response = database.get(new String[]{key});
                        break;
                    case "delete":
                        response = database.delete(new String[]{key});
                        break;

                    case "exit":
                        response = database.exit();
                        break;
                }
                out.println(response.toString());
            }
        } catch (IOException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }
}


