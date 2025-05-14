package com.github.jesusmrs05.mcforgecommander.app;

import com.github.jesusmrs05.mcforgecommander.common.Command;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;

public class Client {
    private static Client client;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;


    public static synchronized Client getInstance() {
        if (Client.client == null) {
            Client.client = new Client();
        }
        return client;
    }

    public void connect(String host, int port, String password) {
        try{
            socket = new Socket(host, port);
            input = new ObjectInputStream(socket.getInputStream());
            output = new ObjectOutputStream(socket.getOutputStream());
            output.writeUTF(password);
            output.flush();
        } catch (UnknownHostException e) {
            throw new RuntimeException(e);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public void sendCommand(Command command) {
        try {
            output.writeObject(command);
            output.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
