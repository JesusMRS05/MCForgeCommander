package com.github.jesusmrs05.mcforgecommander.app;

import com.google.gson.annotations.Expose;

public class ConnectionInfo {
    @Expose
    private String host = "";
    @Expose
    private int port = 0;
    @Expose
    private String password = "";
    @Expose
    private int fps = 25;
    @Expose
    private float imageQuality = 0.2f;
    private boolean connected = false;

    public ConnectionInfo() {
    }

    public ConnectionInfo(String host, int port, String password, boolean connected, int fps, float imageQuality) {
        this.host = host;
        this.port = port;
        this.password = password;
        this.connected = connected;
        this.fps = fps;
        this.imageQuality = imageQuality;
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

    public int getFps() {
        return fps;
    }

    public void setFps(int fps) {
        this.fps = fps;
    }

    public float getImageQuality() {
        return imageQuality;
    }

    public void setImageQuality(float imageQuality) {
        this.imageQuality = imageQuality;
    }
}
