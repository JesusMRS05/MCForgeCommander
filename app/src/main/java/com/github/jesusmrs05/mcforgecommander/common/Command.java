package com.github.jesusmrs05.mcforgecommander.common;

import java.io.Serializable;

public class Command implements Serializable {
    private Instruction instruction;
    private Serializable params;

    public Command(Instruction instruction, Serializable params) {
        this.instruction = instruction;
        this.params = params;
    }

    public Instruction getInstruction() {
        return instruction;
    }

    public Serializable getParams() {
        return params;
    }
}
