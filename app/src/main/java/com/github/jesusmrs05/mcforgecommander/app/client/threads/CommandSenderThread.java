package com.github.jesusmrs05.mcforgecommander.app.client.threads;

import com.github.jesusmrs05.mcforgecommander.app.client.Client;
import com.github.jesusmrs05.mcforgecommander.common.Command;

public class CommandSenderThread extends Thread {
    @Override
    public void run() {
        try{
            Client client = Client.getInstance();
            while (!isInterrupted()){
                Command command = client.takeCommand();
                client.sendCommand(command);
            }
        } catch (InterruptedException ie){
            interrupt();
        }
    }
}
