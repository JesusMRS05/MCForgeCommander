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
import android.util.SparseArray;
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
import java.util.EnumMap;
import java.util.EnumSet;
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

    // Guardamos la relación dedo->botón
    private final SparseArray<ImageButton> activeButtons = new SparseArray<>();
    private final SparseArray<TouchCapture> lastCaptures = new SparseArray<>();


    // Cuenta cuántos dedos mantienen pulsada cada dirección
    private final EnumMap<Instruction, Integer> dirCount =
            new EnumMap<>(Instruction.class);

    // Posición (left, top) del ImageView en pantalla
    private final int[] ivPos = new int[2];

    /** Si el contador pasa de 0→1 enviamos true al servidor. */
    private void press(Instruction instr) {
        int n = dirCount.getOrDefault(instr, 0) + 1;
        dirCount.put(instr, n);
        if (n == 1) client.enqueueCommand(new Command(instr, Boolean.TRUE));
    }

    /** Si el contador pasa de 1→0 enviamos false al servidor. */
    private void release(Instruction instr) {
        int n = dirCount.getOrDefault(instr, 0) - 1;
        if (n < 0) n = 0;
        dirCount.put(instr, n);
        if (n == 0) client.enqueueCommand(new Command(instr, Boolean.FALSE));
    }

    /** Devuelve las direcciones que controla cada botón. */
    private EnumSet<Instruction> dirsFor(ImageButton b) {
        if (b == btnTopLeft)     return EnumSet.of(Instruction.TOGGLE_MOVE_LEFT,
                Instruction.TOGGLE_MOVE_FORWARD);
        if (b == btnTop)         return EnumSet.of(Instruction.TOGGLE_MOVE_FORWARD);
        if (b == btnTopRight)    return EnumSet.of(Instruction.TOGGLE_MOVE_RIGHT,
                Instruction.TOGGLE_MOVE_FORWARD);
        if (b == btnLeft)        return EnumSet.of(Instruction.TOGGLE_MOVE_LEFT);
        if (b == btnRight)       return EnumSet.of(Instruction.TOGGLE_MOVE_RIGHT);
        if (b == btnBottomLeft)  return EnumSet.of(Instruction.TOGGLE_MOVE_LEFT,
                Instruction.TOGGLE_MOVE_BACKWARD);
        if (b == btnBottom)      return EnumSet.of(Instruction.TOGGLE_MOVE_BACKWARD);
        if (b == btnBottomRight) return EnumSet.of(Instruction.TOGGLE_MOVE_RIGHT,
                Instruction.TOGGLE_MOVE_BACKWARD);
        return EnumSet.noneOf(Instruction.class);
    }

    /** Activa/desactiva solo las direcciones que cambian de prev → now. */
    private void updateDirections(ImageButton prev, ImageButton now) {
        EnumSet<Instruction> prevDirs = dirsFor(prev);
        EnumSet<Instruction> nowDirs  = dirsFor(now);

        for (Instruction i : EnumSet.copyOf(prevDirs)) {
            if (!nowDirs.contains(i)) release(i);          // ya no se mantiene
        }
        for (Instruction i : EnumSet.copyOf(nowDirs)) {
            if (!prevDirs.contains(i)) press(i);           // nueva dirección
        }
    }


    /**
     * Maneja multitáctil + logs del D-Pad y Shift
     */
    private boolean handleTouchEvent(MotionEvent event) {

        imageView.getLocationOnScreen(ivPos);

        int actionMasked  = event.getActionMasked();
        int pointerIndex  = event.getActionIndex();
        int pointerId     = event.getPointerId(pointerIndex);

        switch (actionMasked) {

            /* ---------- Dedo NUEVO ---------- */
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_POINTER_DOWN: {

                ImageButton btn = findButtonUnder(event, pointerIndex);
                activeButtons.put(pointerId, btn);

                if (btn != null && btn != btnShift) {
                    btn.setPressed(true);
                    Log.d("DPAD_DEBUG", "Dedo ENCIMA de: " + getButtonName(btn));
                    updateDirections(null, btn);           // <-- DIFERENCIAS
                }

                if (btn == null) {                         // pantalla remota
                    float relX = event.getRawX(pointerIndex) - ivPos[0];
                    float relY = event.getRawY(pointerIndex) - ivPos[1];
                    sendCapture(pointerId, relX, relY, MotionEvent.ACTION_DOWN);
                }
                break;
            }

            /* ---------- Algún dedo SE MUEVE ---------- */
            case MotionEvent.ACTION_MOVE: {

                int pc = event.getPointerCount();
                for (int i = 0; i < pc; i++) {

                    int id  = event.getPointerId(i);
                    ImageButton now  = findButtonUnder(event, i);
                    ImageButton prev = activeButtons.get(id);

                    if (prev != now) {                     // transición de botón
                        if (prev != null && prev != btnShift) {
                            prev.setPressed(false);
                            Log.d("DPAD_DEBUG", "Dedo QUITADO de: " + getButtonName(prev));
                        }
                        if (now  != null && now  != btnShift) {
                            now.setPressed(true);
                            Log.d("DPAD_DEBUG", "Dedo ENCIMA de: " + getButtonName(now));
                        }
                        updateDirections(prev, now);       // <-- DIFERENCIAS
                        activeButtons.put(id, now);
                    }

                    if (now == null) {                     // movimiento preview
                        float relX = event.getRawX(i) - ivPos[0];
                        float relY = event.getRawY(i) - ivPos[1];
                        sendCapture(id, relX, relY, MotionEvent.ACTION_MOVE);
                    }
                }
                break;
            }

            /* ---------- Dedo SE LEVANTA ---------- */
            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_POINTER_UP:
            case MotionEvent.ACTION_CANCEL: {

                ImageButton btn = activeButtons.get(pointerId);
                boolean inside = btn != null && findButtonUnder(event, pointerIndex) == btn;

                /* Shift: doble-tap */
                if (btn == btnShift && inside) {
                    long now = System.currentTimeMillis();
                    if (now - lastShiftClickTime < 300) {
                        isShiftLocked = !isShiftLocked;
                        btn.setPressed(isShiftLocked);
                        Log.d("DPAD_DEBUG", "DOBLE CLIC en Shift");
                        lastShiftClickTime = 0;
                    } else {
                        lastShiftClickTime = now;
                    }
                }

                /* Otros botones */
                if (btn != null && btn != btnShift) {
                    btn.setPressed(false);
                    updateDirections(btn, null);           // <-- LIBERAR direcciones
                    if (inside) {
                        Log.d("DPAD_DEBUG", "CLIC en: " + getButtonName(btn));
                    } else {
                        Log.d("DPAD_DEBUG", "Dedo QUITADO de: " + getButtonName(btn));
                    }
                }
                activeButtons.remove(pointerId);

                /* Pantalla remota: UP */
                if (btn == null) {
                    float relX = event.getRawX(pointerIndex) - ivPos[0];
                    float relY = event.getRawY(pointerIndex) - ivPos[1];
                    sendCapture(pointerId, relX, relY, MotionEvent.ACTION_UP);
                }
                lastCaptures.remove(pointerId);
                break;
            }
        }
        return true;
    }


    /** Envía los comandos de dirección correspondientes a un botón del D-Pad. */
    private void sendDirectionalCommands(ImageButton btn, boolean pressed) {
        Boolean state = Boolean.valueOf(pressed);   // true = pulsar, false = soltar

        if (btn == btnTopLeft) {
            client.enqueueCommand(new Command(Instruction.TOGGLE_MOVE_LEFT, state));
            client.enqueueCommand(new Command(Instruction.TOGGLE_MOVE_FORWARD, state));
        } else if (btn == btnTop) {
            client.enqueueCommand(new Command(Instruction.TOGGLE_MOVE_FORWARD, state));
        } else if (btn == btnTopRight) {
            client.enqueueCommand(new Command(Instruction.TOGGLE_MOVE_RIGHT, state));
            client.enqueueCommand(new Command(Instruction.TOGGLE_MOVE_FORWARD, state));
        } else if (btn == btnLeft) {
            client.enqueueCommand(new Command(Instruction.TOGGLE_MOVE_LEFT, state));
        } else if (btn == btnRight) {
            client.enqueueCommand(new Command(Instruction.TOGGLE_MOVE_RIGHT, state));
        } else if (btn == btnBottomLeft) {
            client.enqueueCommand(new Command(Instruction.TOGGLE_MOVE_LEFT, state));
            client.enqueueCommand(new Command(Instruction.TOGGLE_MOVE_BACKWARD, state));
        } else if (btn == btnBottom) {
            client.enqueueCommand(new Command(Instruction.TOGGLE_MOVE_BACKWARD, state));
        } else if (btn == btnBottomRight) {
            client.enqueueCommand(new Command(Instruction.TOGGLE_MOVE_RIGHT, state));
            client.enqueueCommand(new Command(Instruction.TOGGLE_MOVE_BACKWARD, state));
        }
    }

    private void sendCapture(int pointerId, float x, float y, int action) {
        TouchCapture prev = lastCaptures.get(pointerId);
        TouchCapture tc = new TouchCapture(pointerId, (int) x, (int) y, action,
                prev == null ? null : new TouchCapture(prev),
                imageView.getWidth(), imageView.getHeight());
        try {
            client.enqueueTouchCapture(tc);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Log.e("TouchEvent", "enqueueTouchCapture interrumpido", e);
        }
        lastCaptures.put(pointerId, tc);
    }

    /**
     * Devuelve el ImageButton que está bajo el dedo indicado, o null si no hay ninguno.
     *
     * @param event        MotionEvent con todos los dedos
     * @param pointerIndex índice del dedo que queremos comprobar (0‒pointerCount-1)
     */
    /**
     * Devuelve el ImageButton que está bajo el dedo indicado, o null si no hay ninguno.
     */
    private ImageButton findButtonUnder(MotionEvent event, int pointerIndex) {

        float rawX = event.getRawX(pointerIndex);
        float rawY = event.getRawY(pointerIndex);

        for (ImageButton b : new ImageButton[]{
                btnTopLeft, btnTop, btnTopRight,
                btnLeft, btnShift, btnRight,
                btnBottomLeft, btnBottom, btnBottomRight
        }) {
            Rect r = new Rect();
            b.getGlobalVisibleRect(r);          // rectángulo absoluto
            if (r.contains((int) rawX, (int) rawY)) {
                return b;
            }
        }
        return null;
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
