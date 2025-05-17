package com.github.jesusmrs05.mcforgecommander.app.client.threads;

import com.github.jesusmrs05.mcforgecommander.app.client.Client;

public class GUIStatusThread extends Thread{
    @Override
    public void run() {
        try{
            Client client = Client.getInstance();
            while (!isInterrupted()){
                client.takeGUIStatus();
            }
        } catch (InterruptedException ie){
            interrupt();
        }
    }
}
