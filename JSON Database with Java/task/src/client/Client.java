package client;

import com.beust.jcommander.Parameter;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.*;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.stream.Stream;

public class Client {
    private final String serverAddress;
    private final int serverPort;

    @Parameter(names = {"-t"}, description = "The type of the operation (set, get, delete)")
    private String command;
    @Parameter(names = {"-i"}, description = "The index of the cell")
    private int index;
    @Parameter(names = {"-m"}, description = "The message for 'set' operation")
    private String message;

    @Parameter(names = {"-in"}, description = "The input file name for requests")
    private String inputFile;

    public Client() {
        serverAddress = "127.0.0.1";
        serverPort = 23456;
    }

    public String getInputFile() {
        return inputFile;
    }

    public void connect() {
        try (Socket socket = new Socket(serverAddress, serverPort)) {
            System.out.println("Client started!");
            PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
            BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));

            JsonObject request = new JsonObject();
            request.addProperty("type", command);
            request.addProperty("key", index);

            if (command.equals("set") && message != null) {
                request.addProperty("value", message);
            }

            out.println(request.toString());
            System.out.println("Sent: " + request.toString());

            String response = in.readLine();
            System.out.println("Received: " + response);
        } catch (IOException e) {
            System.out.println("Error communicating with the server: " + e.getMessage());
        }
    }

    public void readRequestFromFile(String fileName) {
        String workingDirectory = System.getProperty("user.dir");
        String filePath = workingDirectory + "/src/client/data/" + fileName;

        try (Stream<String> lines = Files.lines(Paths.get(filePath))) {
            lines.forEach(line -> {
                JsonObject request = JsonParser.parseString(line).getAsJsonObject();
                System.out.println("Request: " + request.toString());
            });
        } catch (IOException e) {
            System.out.println("Error reading file: " + e.getMessage());
        }
    }
}

