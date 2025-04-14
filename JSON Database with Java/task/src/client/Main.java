package client;

import com.beust.jcommander.JCommander;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Client client = new Client();

        JCommander.newBuilder()
                .addObject(client)
                .build()
                .parse(args);

        if (client.getInputFile() != null) {
            client.readRequestFromFile(client.getInputFile());
        } else {
            client.connect();
        }
    }
}