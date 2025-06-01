package com.github.jesusmrs05.mcforgecommander.app.client;

import android.app.AlertDialog;
import android.graphics.Typeface;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.core.content.res.ResourcesCompat;

import com.github.jesusmrs05.mcforgecommander.R;
import com.github.jesusmrs05.mcforgecommander.app.activities.MainActivity;
import com.github.jesusmrs05.mcforgecommander.app.client.threads.CommandSenderThread;
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
    private BlockingQueue<Command> commandQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);
    private GUIStatusThread guiStatusThread;
    private CommandSenderThread commandSenderThread;
    private ImageThread imageThread;
    private ProducerThread producerThread;
    private TouchCaptureSenderThread touchCaptureSenderThread;
    private volatile boolean isReceivingImage = false;
    private LoadingDialog loadingDialog;

    public static synchronized Client getInstance() {
        if (Client.client == null) {
            Client.client = new Client();
        }
        return client;
    }

    public void connect(String host, int port, String password, int fps, float imageQuality, Runnable onConnect) {
        MainActivity mainActivity = mainActivityRef.get();
        if (mainActivity == null) return;

        mainActivity.runOnUiThread(() -> {
            loadingDialog = new LoadingDialog();
            loadingDialog.show(mainActivity.getSupportFragmentManager(), "loading");
        });

        new Thread(() -> {
            try {
                socket = new Socket();
                socket.setTcpNoDelay(true);
                socket.connect(new InetSocketAddress(host, port), TIMEOUT);

                Log.d("Client", "Creating Output");
                output = new ObjectOutputStream(socket.getOutputStream());
                output.flush();

                Log.d("Client", "Sending password");
                output.writeUTF(password);
                output.flush();

                Log.d("Client", "Creating Input");
                input = new ObjectInputStream(socket.getInputStream());

                Log.d("Client", "Reading response");
                String response = input.readUTF();
                Log.d("Client", "Response received: " + response);

                if (response.equalsIgnoreCase("Welcome")) {
                    output.writeInt(fps);
                    output.flush();
                    output.writeFloat(imageQuality);
                    output.flush();
                    producerThread = new ProducerThread();
                    guiStatusThread = new GUIStatusThread();
                    imageThread = new ImageThread(mainActivityRef.get());
                    touchCaptureSenderThread = new TouchCaptureSenderThread();
                    commandSenderThread = new CommandSenderThread();
                    commandSenderThread.start();
                    touchCaptureSenderThread.start();
                    producerThread.start();
                    guiStatusThread.start();
                    imageThread.start();
                    isReceivingImage = true;

                    runOnUi(mainActivity, () -> {
                        loadingDialog.dismiss();
                        onConnect.run();
                    });
                } else {
                    runOnUi(mainActivity, () -> {
                        loadingDialog.dismiss();
                        showAlertDialog("Connection Error", "Wrong password");
                    });
                }
            } catch (UnknownHostException uhe) {
                runOnUi(mainActivity, () -> {
                    loadingDialog.dismiss();
                    showAlertDialog("Connection Error", "Unknown host");
                });
                uhe.printStackTrace();
            } catch (SocketTimeoutException stoe) {
                runOnUi(mainActivity, () -> {
                    loadingDialog.dismiss();
                    showAlertDialog("Connection Error", "Connection Timed Out");
                });
                stoe.printStackTrace();
            } catch (IOException ioe) {
                runOnUi(mainActivity, () -> {
                    loadingDialog.dismiss();
                    showAlertDialog("Connection Error", "Connection Failure: " + ioe.getMessage());
                });
                ioe.printStackTrace();
            } catch (Exception e) {
                runOnUi(mainActivity, () -> {
                    loadingDialog.dismiss();
                    showAlertDialog("Unknown Connection Error", "Unknown Connection Failure: " + e.getMessage());
                });
                e.printStackTrace();
            }
        }).start();
    }

    private void runOnUi(MainActivity mainActivity, Runnable runnable) {
        if (mainActivity != null) {
            mainActivity.runOnUiThread(runnable);
        }
    }

    public void disconnect(Runnable onDisconnect) {
        try {
            producerThread.interrupt();
            guiStatusThread.interrupt();
            imageThread.interrupt();
            touchCaptureSenderThread.interrupt();
            commandSenderThread.interrupt();
            imageQueue.clear();
            guiStatusQueue.clear();
            touchCaptureQueue.clear();
            output.close();
            input.close();
            socket.close();
            isReceivingImage = false;
            onDisconnect.run();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    public void sendCommand(Command cmd) {
        if (socket == null || socket.isClosed()) return;

        try {
            synchronized (output) {            // <-- ¡clave!
                output.writeUnshared(cmd);     // evita referencias a objs previos
                output.flush();
                // opcional cada N mensajes:  output.reset();
            }
        } catch (IOException e) {
            throw new RuntimeException(e);     // o tu manejo de error
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

    public void enqueueCommand(Command command) {
        commandQueue.offer(command);
    }

    public Command takeCommand() throws InterruptedException {
        return commandQueue.take();
    }

    public static WeakReference<MainActivity> getMainActivityRef() {
        return mainActivityRef;
    }
}
