package com.github.jesusmrs05.mcforgecommander.app;

import com.google.gson.annotations.Expose;

public class ConnectionInfo {
    @Expose
    private String host = "";
    @Expose
    private int port = 0;
    @Expose
    private String password = "";
    private boolean connected = false;

    public ConnectionInfo() {
    }

    public ConnectionInfo(String host, int port, String password, boolean connected) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.connected = connected;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public String getPassword() {
        return password;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public boolean isConnected() {
        return connected;
    }

    public void setConnected(boolean connected) {
        this.connected = connected;
    }
}
