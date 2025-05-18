package com.github.jesusmrs05.mcforgecommander.app;

import android.app.AlertDialog;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jesusmrs05.mcforgecommander.R;
import com.github.jesusmrs05.mcforgecommander.app.activities.MainActivity;
import com.github.jesusmrs05.mcforgecommander.app.client.Client;

import java.util.List;

public class ConnectionInfoAdapter extends RecyclerView.Adapter<ConnectionInfoAdapter.ViewHolder> {

    private List<ConnectionInfo> connectionInfos;
    private Context context;

    public ConnectionInfoAdapter(List<ConnectionInfo> connectionInfos, Context context) {
        this.connectionInfos = connectionInfos;
        this.context = context;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.item_connection_info, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        ConnectionInfo connectionInfo = connectionInfos.get(position);
        holder.tvHost.setText("Host: " + connectionInfo.getHost());
        holder.tvPort.setText("Port: " + connectionInfo.getPort());
        if(connectionInfo.isConnected()) {
            holder.tvConnectionStatus.setText("Status: Connected");
            holder.tvConnectionStatus.setTextColor(holder.tvConnectionStatus.getResources().getColor(android.R.color.holo_green_dark));
            holder.btnConnect.setImageResource(R.drawable.minecraft_frame_pause);

        } else {
            holder.tvConnectionStatus.setText("Status: Disconnected");
            holder.tvConnectionStatus.setTextColor(holder.tvConnectionStatus.getResources().getColor(android.R.color.holo_red_light));
            holder.btnConnect.setImageResource(R.drawable.minecraft_frame_right_arrow);
        }
        holder.btnConnect.setOnClickListener(v -> {
            if (connectionInfo.isConnected()) {
                Client.getInstance().disconnect(() -> {
                    MainActivity mainActivity = (MainActivity) context;
                    mainActivity.runOnUiThread(() -> {
                        Bitmap defaultImage = mainActivity.createMultilineTextImageFullScreen(mainActivity, "Disconnected.\nOpen the leftside menu to add connections.", 18, Color.WHITE, 0x2B2B2B);
                        mainActivity.setImage(defaultImage);
                    });
                });
                connectionInfo.setConnected(false);
                notifyDataSetChanged();
            } else {
                for (ConnectionInfo info : connectionInfos) {
                    if (info.isConnected()) {
                        Client.getInstance().disconnect(() -> {});
                        info.setConnected(false);
                        break;
                    }
                }
                Runnable onConnect = () -> {
                    ((MainActivity) context).runOnUiThread(() -> {
                        connectionInfo.setConnected(true);
                        notifyDataSetChanged();
                    });
                };
                Client client = Client.getInstance();
                client.connect(connectionInfo.getHost(), connectionInfo.getPort(), connectionInfo.getPassword(), onConnect);
            }
        });
        holder.itemView.setOnClickListener(v -> {
            new ModifyConnectionInfoDialog(context, connectionInfo, () -> {
                notifyDataSetChanged();
                SecurePreferencesHelper.saveConnections(context, connectionInfos);
            }).show();
        });
        holder.itemView.setOnLongClickListener(v -> {
            AlertDialog alertDialog = new AlertDialog.Builder(context)
                    .setTitle("Delete connection info?")
                    .setPositiveButton("Accept", (dialog, which) -> {
                        connectionInfos.remove(position);
                        notifyDataSetChanged();
                        SecurePreferencesHelper.saveConnections(context, connectionInfos);
                        dialog.dismiss();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .create();

            alertDialog.show();

            Typeface minecraftTypeface = ResourcesCompat.getFont(context, R.font.minecraftia_regular);

            int textViewId = context.getResources().getIdentifier("alertTitle", "id", "android");
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

            Button negativeButton = alertDialog.getButton(AlertDialog.BUTTON_NEGATIVE);
            if (negativeButton != null && minecraftTypeface != null) {
                negativeButton.setTypeface(minecraftTypeface);
            }

            return true;
        });
    }

    @Override
    public int getItemCount() {
        return connectionInfos.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        private TextView tvHost;
        private TextView tvPort;
        private TextView tvConnectionStatus;
        private ImageButton btnConnect;

        public ViewHolder(View itemView) {
            super(itemView);
            tvHost = itemView.findViewById(R.id.tvHost);
            tvPort = itemView.findViewById(R.id.tvPort);
            tvConnectionStatus = itemView.findViewById(R.id.tvConnectionStatus);
            btnConnect = itemView.findViewById(R.id.btnConnect);
        }
    }
}
