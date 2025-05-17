package com.github.jesusmrs05.mcforgecommander.app.client.threads;

import com.github.jesusmrs05.mcforgecommander.app.client.Client;
import com.github.jesusmrs05.mcforgecommander.common.ServerPacket;

import java.io.IOException;

public class ProducerThread extends Thread {
    @Override
    public void run() {
        try {
            Client client = Client.getInstance();
            while (!isInterrupted()) {
                ServerPacket serverPacket = client.getServerPacket();
                switch (serverPacket.getType()) {
                    case IMAGE:
                        client.enqueueImage((byte[]) serverPacket.getData());
                        break;
                    case GUI_STATUS:
                        client.enqueueGUIStatus((ServerPacket.GUIStatus) serverPacket.getData());
                }
            }
        } catch (InterruptedException ie) {
            interrupt();
        } catch (IOException ioe) {
            interrupt();
        }
    }
}
