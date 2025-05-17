package com.github.jesusmrs05.mcforgecommander.app;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.github.jesusmrs05.mcforgecommander.R;
import com.github.jesusmrs05.mcforgecommander.app.client.Client;

import java.util.List;

public class ConnectionInfoAdapter extends RecyclerView.Adapter<ConnectionInfoAdapter.ViewHolder> {

    private List<ConnectionInfo> connectionInfos;

    public ConnectionInfoAdapter(List<ConnectionInfo> connectionInfos) {
        this.connectionInfos = connectionInfos;
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
            holder.tvConnectionStatus.setTextColor(holder.tvConnectionStatus.getResources().getColor(android.R.color.holo_green_light));
        } else {
            holder.tvConnectionStatus.setText("Status: Disconnected");
            holder.tvConnectionStatus.setTextColor(holder.tvConnectionStatus.getResources().getColor(android.R.color.holo_red_light));
        }
        holder.btnConnect.setOnClickListener(v -> {
            Client client = Client.getInstance();
            client.connect(connectionInfo.getHost(), connectionInfo.getPort(), connectionInfo.getPassword());
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
