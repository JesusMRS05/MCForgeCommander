package com.github.jesusmrs05.mcforgecommander.app.activities;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.util.Log;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.github.jesusmrs05.mcforgecommander.R;

import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class MainActivity extends AppCompatActivity {

    private ImageView imageView;
    private Thread streamThread;
    private ObjectOutputStream output;
    private Bitmap reusableBitmap = null;

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

        startStream();
    }

    private void startStream() {
        streamThread = new Thread(() -> {
            try {
                Socket socket = new Socket("10.0.2.2", 6000);  // o usa IP real si no es en emulador
                InputStream is = socket.getInputStream();
                output = new ObjectOutputStream(socket.getOutputStream());
                ObjectInputStream objectInputStream = new ObjectInputStream(is);

                while (!Thread.currentThread().isInterrupted()) {
                    Log.d("ImageStream", "Waiting for length...");
                    int length = objectInputStream.readInt();
                    Log.d("ImageStream", "Received length: " + length);
                    if (length <= 0) break;

                    byte[] imageBytes = new byte[length];
                    Log.d("ImageStream", "About to read fully...");
                    objectInputStream.readFully(imageBytes);
                    Log.d("ImageStream", "Read fully");

                    // Decodificación eficiente
                    Bitmap bitmap = decodeSampledBitmap(imageBytes, 426, 240); // 144p o 240p según tu resolución objetivo
                    if (bitmap != null) {
                        Log.d("ImageStream", "Image decoded successfully");
                        runOnUiThread(() -> setImage(bitmap));
                    } else {
                        Log.d("ImageStream", "Failed to decode image");
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
}
