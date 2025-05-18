package com.github.jesusmrs05.mcforgecommander.app.client;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.github.jesusmrs05.mcforgecommander.R;
import com.github.jesusmrs05.mcforgecommander.app.activities.MainActivity;
import com.github.jesusmrs05.mcforgecommander.app.client.threads.GUIStatusThread;
import com.github.jesusmrs05.mcforgecommander.app.client.threads.ImageThread;
import com.github.jesusmrs05.mcforgecommander.app.client.threads.ProducerThread;
import com.github.jesusmrs05.mcforgecommander.app.client.threads.TouchCaptureSenderThread;
import com.github.jesusmrs05.mcforgecommander.common.Command;
import com.github.jesusmrs05.mcforgecommander.common.ServerPacket;
import com.github.jesusmrs05.mcforgecommander.common.TouchCapture;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.ref.WeakReference;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.UnknownHostException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class Client {
    private static final int TIMEOUT = 10000; // in ms
    private static final int MAX_QUEUE_SIZE = 300;
    private static final int MAX_TOUCH_CAPTURE_QUEUE_SIZE = 1000;
    private static WeakReference<MainActivity> mainActivityRef;
    private static Client client;
    private Socket socket;
    private ObjectOutputStream output;
    private ObjectInputStream input;
    private BlockingQueue<byte[]> imageQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private BlockingQueue<ServerPacket.GUIStatus> guiStatusQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    public BlockingQueue<TouchCapture> touchCaptureQueue = new LinkedBlockingQueue<>(MAX_TOUCH_CAPTURE_QUEUE_SIZE);
    private GUIStatusThread guiStatusThread;
    private ImageThread imageThread;
    private ProducerThread producerThread;
    private TouchCaptureSenderThread touchCaptureSenderThread;
    private volatile boolean isReceivingImage = false;


    public static synchronized Client getInstance() {
        if (Client.client == null) {
            Client.client = new Client();
        }
        return client;
    }

    public void connect(String host, int port, String password, Runnable onConnect) {
        new Thread(() -> {
            try {
                socket = new Socket();
                socket.connect(new InetSocketAddress(host, port), TIMEOUT);
                input = new ObjectInputStream(socket.getInputStream());
                output = new ObjectOutputStream(socket.getOutputStream());
                output.writeUTF(password);
                output.flush();
                if (input.readUTF().equalsIgnoreCase("Welcome")) {
                    producerThread = new ProducerThread();
                    guiStatusThread = new GUIStatusThread();
                    imageThread = new ImageThread(mainActivityRef.get());
                    touchCaptureSenderThread = new TouchCaptureSenderThread();
                    touchCaptureSenderThread.start();
                    producerThread.start();
                    guiStatusThread.start();
                    imageThread.start();
                    isReceivingImage = true;
                    onConnect.run();
                } else {
                    showAlertDialog("Connection Error", "Wrong password");
                }
            } catch (UnknownHostException uhe) {
                showAlertDialog("Connection Error", "Unknown host");
            } catch (SocketTimeoutException stoe) {
                showAlertDialog("Connection Error", "Connection Timed Out");
            } catch (IOException ioe) {
                showAlertDialog("Connection Error", "Connection Failure: " + ioe.getMessage());
            }

        }).start();
    }

    public void disconnect(Runnable onDisconnect) {
        try {
            producerThread.interrupt();
            guiStatusThread.interrupt();
            imageThread.interrupt();
            touchCaptureSenderThread.interrupt();
            imageQueue.clear();
            guiStatusQueue.clear();
            touchCaptureQueue.clear();
            output.close();
            input.close();
            socket.close();
            isReceivingImage = false;
            onDisconnect.run();
        } catch (IOException ioe){
            ioe.printStackTrace();
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

    public static void setMainActivity(MainActivity mainActivity) {
        mainActivityRef = new WeakReference<>(mainActivity);
    }

    public ServerPacket getServerPacket() throws IOException {
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

    public void enqueueTouchCapture(TouchCapture touchCapture) throws InterruptedException {
        touchCaptureQueue.put(touchCapture);
    }

    public TouchCapture takeTouchCapture() throws InterruptedException {
        return touchCaptureQueue.take();
    }

    private void showAlertDialog(String title, String message) {
        MainActivity mainActivity = mainActivityRef.get();
        if (mainActivity == null) return;

        mainActivity.runOnUiThread(() -> {
            AlertDialog alertDialog = new AlertDialog.Builder(mainActivity)
                    .setTitle(title)
                    .setMessage(message)
                    .setPositiveButton("Accept", (dialog, which) -> dialog.dismiss())
                    .create();

            alertDialog.show();

            Typeface minecraftTypeface = ResourcesCompat.getFont(mainActivity, R.font.minecraftia_regular);

            int textViewId = mainActivity.getResources().getIdentifier("alertTitle", "id", "android");
            TextView dialogTitle = alertDialog.findViewById(textViewId);
            if (dialogTitle != null && minecraftTypeface != null) {
                dialogTitle.setTypeface(minecraftTypeface);
            }

            TextView messageView = alertDialog.findViewById(android.R.id.message);
            if (messageView != null && minecraftTypeface != null) {
                messageView.setTypeface(minecraftTypeface);
            }

            Button positiveButton = alertDialog.getButton(AlertDialog.BUTTON_POSITIVE);
            if (positiveButton != null && minecraftTypeface != null) {
                positiveButton.setTypeface(minecraftTypeface);
            }
        });
    }

    public boolean isConnected() {
        return socket != null && socket.isConnected();
    }

    public boolean isReceivingImage() {
        return isReceivingImage;
    }
}
