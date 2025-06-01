package com.github.jesusmrs05.mcforgecommander.app.client.threads;

import com.github.jesusmrs05.mcforgecommander.app.activities.MainActivity;
import com.github.jesusmrs05.mcforgecommander.app.client.Client;
import com.github.jesusmrs05.mcforgecommander.common.ServerPacket;

public class GUIStatusThread extends Thread {
    @Override
    public void run() {
        try {
            Client client = Client.getInstance();
            MainActivity mainActivity = Client.getMainActivityRef().get();
            while (!isInterrupted()) {
                ServerPacket.GUIStatus guiStatus = client.takeGUIStatus();
                mainActivity.runOnUiThread(() -> {
                    mainActivity.changeLayout(guiStatus);
                });
            }
        } catch (InterruptedException ie) {
            interrupt();
        }
    }
}
