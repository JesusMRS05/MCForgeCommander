package com.github.jesusmrs05.mcforgecommander.app.activities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.jesusmrs05.mcforgecommander.R;
import com.github.jesusmrs05.mcforgecommander.common.Command;
import com.github.jesusmrs05.mcforgecommander.common.Instruction;
import com.github.jesusmrs05.mcforgecommander.common.TouchCapture;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private Thread streamThread;
    private ObjectOutputStream output;
    private Bitmap reusableBitmap = null;
    private TouchCapture lastCapture = null;
    private BlockingQueue<TouchCapture> touchCaptures = new LinkedBlockingQueue();
    private LinearLayout drawer;
    private ImageButton btnMenu;
    private final boolean[] drawerOpen = {false};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        imageView = findViewById(R.id.imageView);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
        final int drawerWidth = 240; // en dp
        final float density = getResources().getDisplayMetrics().density;
        final float drawerPx = drawerWidth * density;
        drawer = findViewById(R.id.drawerMenu);
        btnMenu = findViewById(R.id.btnMenu);
        btnMenu.setOnClickListener(v -> {
            float targetX = drawerOpen[0] ? -drawerPx : 0;
            float buttonTargetX = drawerOpen[0] ? 0 : drawerPx;

            drawer.animate().translationX(targetX).setDuration(300).start();
            btnMenu.animate().translationX(buttonTargetX).setDuration(300).start();

            drawerOpen[0] = !drawerOpen[0];
        });

        /*View touchCapture = findViewById(R.id.touchCapture);
        touchCapture.setOnTouchListener((v, event) -> {
            Log.d("MainActivity", "Touch event x: " + event.getX() + ", y: " + event.getY() + ", action: " + event.getAction());
            TouchCapture touchCaptureObj = new TouchCapture(
                    (int) event.getX(),
                    (int) event.getY(),
                    event.getAction(),
                    lastCapture == null ? null : new TouchCapture(lastCapture),
                    imageView.getWidth(),
                    imageView.getHeight()
            );
            try {
                enqueueTouchCapture(touchCaptureObj);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            lastCapture = touchCaptureObj;
            return true; // Consume el evento para que no se propague
        });
        new Thread(() -> {
            try {
                while (true) {
                    TouchCapture touchCaptureObj = takeTouchCapture();
                    Command command = new Command(Instruction.SCREEN_TOUCH, touchCaptureObj);
                    try {
                        output.writeObject(command);
                        output.flush();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
        startStream();//this whole function came from temp/tryingout because I will probably use it,*/
    }

    private void startStream() {
        streamThread = new Thread(() -> {
            try {
                Socket socket = new Socket("192.168.1.22", 6000);
                InputStream is = socket.getInputStream();
                output = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream objectInputStream = new ObjectInputStream(is);

                while (!Thread.currentThread().isInterrupted()) {
                    int length = objectInputStream.readInt();
                    if (length <= 0) break;

                    byte[] imageBytes = new byte[length];
                    objectInputStream.readFully(imageBytes);

                    Bitmap bitmap = decodeSampledBitmap(imageBytes, 426, 240);
                    if (bitmap != null) {
                        runOnUiThread(() -> setImage(bitmap));
                    }
                }

                objectInputStream.close();
                socket.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        streamThread.start();
    }

    // Método para establecer la imagen de manera eficiente
    private void setImage(Bitmap bitmap) {
        // Reciclar bitmap anterior si ya existe
        if (reusableBitmap != null && !reusableBitmap.isRecycled()) {
            reusableBitmap.recycle();
        }
        reusableBitmap = bitmap;
        imageView.setImageBitmap(reusableBitmap);
    }

    // Decodificación eficiente de la imagen con inSampleSize
    private Bitmap decodeSampledBitmap(byte[] imageBytes, int reqWidth, int reqHeight) {
        // Primero, decodificamos solo las dimensiones de la imagen (sin cargarla completamente)
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);

        // Calculamos inSampleSize para reducir el tamaño de la imagen
        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        // Ahora decodificamos la imagen con el tamaño ajustado
        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;  // Usamos RGB_565 para ahorrar memoria

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
    }

    // Método para calcular el tamaño de la muestra
    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        // Dimensiones originales de la imagen
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            // Calculamos el mayor inSampleSize que mantiene ambas dimensiones mayores que las requeridas
            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (streamThread != null) streamThread.interrupt();
        // Liberar recursos de bitmap si ya no se necesita
        if (reusableBitmap != null && !reusableBitmap.isRecycled()) {
            reusableBitmap.recycle();
        }
    }

    public void enqueueTouchCapture(TouchCapture touchCapture) throws InterruptedException {
        touchCaptures.put(touchCapture);
    }

    public TouchCapture takeTouchCapture() throws InterruptedException {
        return touchCaptures.take();
    }
}
