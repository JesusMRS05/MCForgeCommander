package com.github.jesusmrs05.mcforgecommander.app.activities;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Layout;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jesusmrs05.mcforgecommander.R;
import com.github.jesusmrs05.mcforgecommander.app.ConnectionInfo;
import com.github.jesusmrs05.mcforgecommander.app.ConnectionInfoAdapter;
import com.github.jesusmrs05.mcforgecommander.app.ModifyConnectionInfoDialog;
import com.github.jesusmrs05.mcforgecommander.app.SecurePreferencesHelper;
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
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MainActivity extends AppCompatActivity {

    public ImageView imageView;
    public Thread streamThread;
    public ObjectOutputStream output;
    public Bitmap reusableBitmap = null;
    public TouchCapture lastCapture = null;
    public LinearLayout drawer, dPadContainer;
    public ImageButton btnMenu, btnAddConnection;
    public final boolean[] drawerOpen = {false};
    private Client client;
    private List<ConnectionInfo> connectionInfos;
    private RecyclerView rvConnections;
    private ConnectionInfoAdapter connectionInfoAdapter;
    private ImageButton btnTopLeft, btnTop, btnTopRight, btnLeft, btnShift, btnRight, btnBottomLeft, btnBottom, btnBottomRight;

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
        Client.setMainActivity(this);
        client = Client.getInstance();
        final int drawerWidth = 240; // in dp
        final float density = getResources().getDisplayMetrics().density;
        final float drawerPx = drawerWidth * density;
        connectionInfos = SecurePreferencesHelper.loadConnections(this);
        connectionInfoAdapter = new ConnectionInfoAdapter(connectionInfos, this);
        rvConnections = findViewById(R.id.rvConnections);
        rvConnections.setAdapter(connectionInfoAdapter);
        rvConnections.setLayoutManager(new LinearLayoutManager(this));
        dPadContainer = findViewById(R.id.dPadContainer);
        drawer = findViewById(R.id.drawerMenu);
        btnMenu = findViewById(R.id.btnMenu);
        btnTopLeft = findViewById(R.id.btnTopLeft);
        btnTop = findViewById(R.id.btnTop);
        btnTopRight = findViewById(R.id.btnTopRight);
        btnLeft = findViewById(R.id.btnLeft);
        btnShift = findViewById(R.id.btnShift);
        btnRight = findViewById(R.id.btnRight);
        btnBottomLeft = findViewById(R.id.btnBottomLeft);
        btnBottom = findViewById(R.id.btnBottom);
        btnBottomRight = findViewById(R.id.btnBottomRight);
        btnAddConnection = findViewById(R.id.btnAddConnection);
        btnAddConnection.setOnClickListener(v -> {
            ConnectionInfo connectionInfo = new ConnectionInfo();
            Runnable callback = () -> {
                connectionInfos.add(connectionInfo);
                SecurePreferencesHelper.saveConnections(this, connectionInfos);
                connectionInfoAdapter.notifyItemInserted(connectionInfos.size() - 1);
            };
            ModifyConnectionInfoDialog dialog = new ModifyConnectionInfoDialog(this, connectionInfo, callback);
            dialog.show();
        });
        btnMenu.setOnClickListener(v -> {
            float targetX = drawerOpen[0] ? -drawerPx : 0;
            float buttonTargetX = drawerOpen[0] ? 0 : drawerPx;

            drawer.animate().translationX(targetX).setDuration(300).start();
            btnMenu.animate().translationX(buttonTargetX).setDuration(300).start();

            drawerOpen[0] = !drawerOpen[0];
        });
        ImageButton[] buttons = new ImageButton[]{btnTopLeft, btnTop, btnTopRight, btnLeft, btnRight, btnBottomLeft, btnBottom, btnBottomRight, btnShift};

        View mainLayout = findViewById(R.id.main);
        mainLayout.setOnTouchListener((v, event) -> {
            return handleTouchEvent(event);
        });

        Bitmap defaultImage = createMultilineTextImageFullScreen(this, "Disconnected.\nOpen the leftside menu to add connections.", 18, Color.WHITE, 0x2B2B2B);
        setImage(defaultImage);

        //client.connect("192.168.1.22", 6000, "s1C$BlmPGw4Fc87R");
    }

    public void setImage(Bitmap bitmap) {
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
        if (reusableBitmap != null && !reusableBitmap.isRecycled()) {
            reusableBitmap.recycle();
        }
    }

    public Bitmap createMultilineTextImageFullScreen(Context context, String text, int textSizeSp, int textColor, int bgColor) {
        DisplayMetrics metrics = context.getResources().getDisplayMetrics();
        int width = metrics.widthPixels;
        int height = metrics.heightPixels;

        float textSizePx = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_SP,
                textSizeSp,
                metrics
        );

        Typeface typeface = ResourcesCompat.getFont(context, R.font.minecraftia_regular);

        TextPaint textPaint = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        textPaint.setColor(textColor);
        textPaint.setTextSize(textSizePx);
        textPaint.setTypeface(typeface);

        StaticLayout staticLayout = StaticLayout.Builder.obtain(text, 0, text.length(), textPaint, width)
                .setAlignment(Layout.Alignment.ALIGN_CENTER)
                .setLineSpacing(0, 1)
                .setIncludePad(false)
                .build();

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        canvas.drawColor(bgColor);

        int textHeight = staticLayout.getHeight();
        float textY = (height - textHeight) / 2f;

        canvas.save();
        canvas.translate(0, textY);
        staticLayout.draw(canvas);
        canvas.restore();

        return bitmap;
    }

    private long lastShiftClickTime = 0; // Variable de clase
    private boolean isShiftLocked = false;

    private boolean handleTouchEvent(MotionEvent event) {
        float rawX = event.getRawX();
        float rawY = event.getRawY();

        // Obtener coordenadas relativas a imageView
        int[] imageViewLocation = new int[2];
        imageView.getLocationOnScreen(imageViewLocation);
        float imageViewX = rawX - imageViewLocation[0];
        float imageViewY = rawY - imageViewLocation[1];

        ImageButton currentTouchedButton = null;

        // Detectar botón actualmente tocado
        for (ImageButton button : new ImageButton[]{
                btnTopLeft, btnTop, btnTopRight,
                btnLeft, btnShift, btnRight,
                btnBottomLeft, btnBottom, btnBottomRight
        }) {
            Rect rect = new Rect();
            button.getGlobalVisibleRect(rect);
            if (rect.contains((int) rawX, (int) rawY)) {
                currentTouchedButton = button;
                break;
            }
        }

        // Procesar estado de cada botón
        for (ImageButton button : new ImageButton[]{
                btnTopLeft, btnTop, btnTopRight,
                btnLeft, btnShift, btnRight,
                btnBottomLeft, btnBottom, btnBottomRight
        }) {
            Rect rect = new Rect();
            button.getGlobalVisibleRect(rect);
            boolean isInside = rect.contains((int) rawX, (int) rawY);
            boolean wasPressed = button.isPressed();

            switch (event.getActionMasked()) {
                case MotionEvent.ACTION_DOWN:
                case MotionEvent.ACTION_MOVE:
                    // Excluir efecto de brillo para Shift
                    if (button != btnShift) {
                        button.setPressed(button == currentTouchedButton);
                    }

                    // Logs solo para botones normales
                    if (button != btnShift) {
                        if (!wasPressed && button.isPressed()) {
                            Log.d("DPAD_DEBUG", "Dedo ENCIMA de: " + getButtonName(button));
                        } else if (wasPressed && !button.isPressed()) {
                            Log.d("DPAD_DEBUG", "Dedo QUITADO de: " + getButtonName(button));
                        }
                    }
                    break;

                case MotionEvent.ACTION_UP:
                case MotionEvent.ACTION_CANCEL:
                    button.setPressed(false);

                    if (isInside) {
                        if (button == btnShift) {
                            // Manejar doble click para Shift
                            long now = System.currentTimeMillis();
                            if (now - lastShiftClickTime < 300) {
                                isShiftLocked = !isShiftLocked;
                                button.setPressed(isShiftLocked);
                                Log.d("DPAD_DEBUG", "DOBLE CLIC en Shift");
                                lastShiftClickTime = 0; // Reset para evitar triple clic
                            } else {
                                lastShiftClickTime = now;
                            }
                        } else {
                            button.performClick();
                            Log.d("DPAD_DEBUG", "CLIC en: " + getButtonName(button));
                        }
                    }
                    break;
            }
        }

        // Enviar evento al servidor si no es sobre el D-pad
        if (currentTouchedButton == null) {
            if (imageViewX >= 0 && imageViewX <= imageView.getWidth() &&
                    imageViewY >= 0 && imageViewY <= imageView.getHeight()) {

                TouchCapture touchCaptureObj = new TouchCapture(
                        (int) imageViewX,
                        (int) imageViewY,
                        event.getAction(),
                        lastCapture == null ? null : new TouchCapture(lastCapture),
                        imageView.getWidth(),
                        imageView.getHeight()
                );

                try {
                    client.enqueueTouchCapture(touchCaptureObj);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Log.e("TouchEvent", "Interrupción en enqueueTouchCapture", e);
                }
                lastCapture = touchCaptureObj;
            }
        }

        return true;
    }

    // Método auxiliar para nombres de botones
    private String getButtonName(ImageButton button) {
        if (button == btnTopLeft) return "Arriba-Izquierda";
        if (button == btnTop) return "Arriba";
        if (button == btnTopRight) return "Arriba-Derecha";
        if (button == btnLeft) return "Izquierda";
        if (button == btnShift) return "Shift";
        if (button == btnRight) return "Derecha";
        if (button == btnBottomLeft) return "Abajo-Izquierda";
        if (button == btnBottom) return "Abajo";
        if (button == btnBottomRight) return "Abajo-Derecha";
        return "Desconocido";
    }
}
