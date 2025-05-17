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
import com.github.jesusmrs05.mcforgecommander.app.client.Client;
import com.github.jesusmrs05.mcforgecommander.common.Command;
import com.github.jesusmrs05.mcforgecommander.common.Instruction;
import com.github.jesusmrs05.mcforgecommander.common.ServerPacket;
import com.github.jesusmrs05.mcforgecommander.common.TouchCapture;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity {

    public ImageView imageView;
    public Thread streamThread;
    public ObjectOutputStream output;
    public Bitmap reusableBitmap = null;
    public TouchCapture lastCapture = null;
    public BlockingQueue<TouchCapture> touchCaptures = new LinkedBlockingQueue();
    public LinearLayout drawer;
    public ImageButton btnMenu;
    public final boolean[] drawerOpen = {false};

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
        }).start();*/
        Client.setMainActivity(this);
        Client client = Client.getInstance();
        client.connect("192.168.1.22", 6000, "s1C$BlmPGw4Fc87R");
    }

    public void setImage(Bitmap bitmap) {
        // Reciclar bitmap anterior si ya existe
        if (reusableBitmap != null && !reusableBitmap.isRecycled()) {
            reusableBitmap.recycle();
        }
        reusableBitmap = bitmap;
        imageView.setImageBitmap(reusableBitmap);
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
