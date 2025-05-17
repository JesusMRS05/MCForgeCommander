package com.github.jesusmrs05.mcforgecommander.app.client;

import android.app.AlertDialog;

import com.github.jesusmrs05.mcforgecommander.app.activities.MainActivity;
import com.github.jesusmrs05.mcforgecommander.app.client.threads.GUIStatusThread;
import com.github.jesusmrs05.mcforgecommander.app.client.threads.ImageThread;
import com.github.jesusmrs05.mcforgecommander.app.client.threads.ProducerThread;
import com.github.jesusmrs05.mcforgecommander.common.Command;
import com.github.jesusmrs05.mcforgecommander.common.ServerPacket;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.lang.ref.WeakReference;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {
    private static final int MAX_QUEUE_SIZE = 10;
    private static WeakReference<MainActivity> mainActivityRef;
    private static Client client;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private BlockingQueue<byte[]> imageQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private BlockingQueue<ServerPacket.GUIStatus> guiStatusQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private GUIStatusThread guiStatusThread;
    private ImageThread imageThread;
    private ProducerThread producerThread;


    public static synchronized Client getInstance() {
        if (Client.client == null) {
            Client.client = new Client();
        }
        return client;
    }

    public void connect(String host, int port, String password) {
        new Thread(() -> {
            try {
                socket = new Socket(host, port);
                input = new ObjectInputStream(socket.getInputStream());
                output = new ObjectOutputStream(socket.getOutputStream());
                output.writeUTF(password);
                output.flush();
                if(input.readUTF().equalsIgnoreCase("Welcome")){
                    producerThread = new ProducerThread();
                    guiStatusThread = new GUIStatusThread();
                    imageThread = new ImageThread(mainActivityRef.get());
                    producerThread.start();
                    guiStatusThread.start();
                    imageThread.start();
                }
            } catch (UnknownHostException e) {
                try {
                    MainActivity mainActivity = mainActivityRef.get();
                    mainActivity.runOnUiThread(() -> {
                        AlertDialog alertDialog = new AlertDialog.Builder(mainActivityRef.get()).create();
                        alertDialog.setTitle("Error");
                        alertDialog.setMessage("Unknown host");
                        alertDialog.show();
                    });
                } catch (NullPointerException npe) {
                }
            } catch (IOException e) {
                try {
                    MainActivity mainActivity = mainActivityRef.get();
                    mainActivity.runOnUiThread(() -> {
                        AlertDialog alertDialog = new AlertDialog.Builder(mainActivityRef.get()).create();
                        alertDialog.setTitle("Error");
                        alertDialog.setMessage("Connection Failure: " + e.getMessage());
                        alertDialog.show();
                    });
                } catch (NullPointerException npe) {
                }
            }
        }).start();
    }

    public void sendCommand(Command command) {
        try {
            output.writeObject(command);
            output.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    public static void setMainActivity(MainActivity mainActivity) {
        mainActivityRef = new WeakReference<>(mainActivity);
    }

    public ServerPacket getServerPacket() throws IOException{
        ServerPacket serverPacket;
        try {
            serverPacket = (ServerPacket) input.readObject();
        } catch (ClassNotFoundException cnfe) {
            serverPacket = null;
        }
        return serverPacket;
    }

    public void enqueueImage(byte[] image) throws InterruptedException {
        imageQueue.put(image);
    }

    public byte[] takeImage() throws InterruptedException {
        return imageQueue.take();
    }

    public void enqueueGUIStatus(ServerPacket.GUIStatus guiStatus) throws InterruptedException {
        guiStatusQueue.put(guiStatus);
    }

    public ServerPacket.GUIStatus takeGUIStatus() throws InterruptedException {
        return guiStatusQueue.take();
    }
}
