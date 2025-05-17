package com.github.jesusmrs05.mcforgecommander.app.client.threads;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.github.jesusmrs05.mcforgecommander.app.activities.MainActivity;
import com.github.jesusmrs05.mcforgecommander.app.client.Client;

public class ImageThread extends Thread{
    private MainActivity mainActivity;

    public ImageThread(MainActivity mainActivity) {
        this.mainActivity = mainActivity;
    }

    @Override
    public void run() {
        try{
            Client client = Client.getInstance();
            while (!isInterrupted()){
                byte[] imageBytes = client.takeImage();
                Bitmap bitmap = decodeSampledBitmap(imageBytes, 426, 240);
                mainActivity.runOnUiThread(() -> {
                    mainActivity.setImage(bitmap);
                });
            }
        } catch (InterruptedException ie){
            interrupt();
        }
    }

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
}
