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
                    if(client.isConnected() && client.isReceivingImage()){
                        mainActivity.setImage(bitmap);
                    }
                });
            }
        } catch (InterruptedException ie){
            interrupt();
        }
    }

    private Bitmap decodeSampledBitmap(byte[] imageBytes, int reqWidth, int reqHeight) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);

        options.inJustDecodeBounds = false;
        options.inPreferredConfig = Bitmap.Config.RGB_565;

        return BitmapFactory.decodeByteArray(imageBytes, 0, imageBytes.length, options);
    }

    private int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {
        final int height = options.outHeight;
        final int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            final int halfHeight = height / 2;
            final int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }

        return inSampleSize;
    }
}
