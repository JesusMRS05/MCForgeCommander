package com.github.jesusmrs05.mcforgecommander.common;

import java.io.Serializable;

public class TouchCapture implements Serializable {
    public static final int MAX_DEPTH = 5;

    private int x;
    private int y;
    private int action;
    private TouchCapture lastCapture;
    private int inputWidth;
    private int inputHeight;

    public TouchCapture(int x, int y, int action, TouchCapture lastCapture, int inputWidth, int inputHeight) {
        this.x = x;
        this.y = y;
        this.action = action;
        this.lastCapture = lastCapture;
        this.inputWidth = inputWidth;
        this.inputHeight = inputHeight;
        try {
            TouchCapture lastLastCapture = this.lastCapture;
            for (int i = 0; i < MAX_DEPTH; i++) {
                lastLastCapture = lastLastCapture.lastCapture;
            }
            lastLastCapture.setLastCapture(null);
        } catch (NullPointerException npe){
        }
    }

    public TouchCapture(TouchCapture touchCapture) {
        this.x = touchCapture.x;
        this.y = touchCapture.y;
        this.action = touchCapture.action;
        this.lastCapture = touchCapture.lastCapture;
        this.inputWidth = touchCapture.inputWidth;
        this.inputHeight = touchCapture.inputHeight;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public int getAction() {
        return action;
    }

    public TouchCapture getLastCapture() {
        return lastCapture;
    }

    public boolean isClick() {
        if (lastCapture == null) return false;

        boolean wasDown = lastCapture.action == 0;
        boolean isUp = this.action == 1;
        boolean isClose = getDistanceToLast() < 20;

        return wasDown && isUp && isClose;
    }

    public boolean isMovement() {
        return lastCapture != null && action == 2 && getDistanceToLast() >= 2;
    }

    public double getDistanceToLast() {
        if (lastCapture == null) return 0;
        int dx = x - lastCapture.x;
        int dy = y - lastCapture.y;
        return Math.sqrt(dx * dx + dy * dy);
    }

    public float getDeltaYaw() {
        if (lastCapture == null) return 0;
        return (x - lastCapture.x) * 0.1f;
    }

    public float getDeltaPitch() {
        if (lastCapture == null) return 0;
        return (lastCapture.y - y) * 0.1f;
    }

    public int getInputWidth() {
        return inputWidth;
    }

    public int getInputHeight() {
        return inputHeight;
    }

    public void setLastCapture(TouchCapture lastCapture){
        this.lastCapture = lastCapture;
    }
}
