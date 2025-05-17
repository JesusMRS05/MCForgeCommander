package com.github.jesusmrs05.mcforgecommander.app.client.threads;

import com.github.jesusmrs05.mcforgecommander.app.client.Client;
import com.github.jesusmrs05.mcforgecommander.common.Command;
import com.github.jesusmrs05.mcforgecommander.common.Instruction;
import com.github.jesusmrs05.mcforgecommander.common.TouchCapture;

public class TouchCaptureSenderThread extends Thread{
    @Override
    public void run() {
        try{
            Client client = Client.getInstance();
            while (!isInterrupted()){
                TouchCapture touchCapture = client.takeTouchCapture();
                Command command = new Command(Instruction.SCREEN_TOUCH, touchCapture);
                client.sendCommand(command);
            }
        } catch (InterruptedException ie){
        }
    }
}
