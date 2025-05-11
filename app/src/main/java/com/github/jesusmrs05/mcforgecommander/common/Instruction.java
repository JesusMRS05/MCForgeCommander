package com.github.jesusmrs05.mcforgecommander.common;

import java.io.Serializable;

public enum Instruction implements Serializable {
    TOGGLE_MOVE_FORWARD,
    TOGGLE_MOVE_BACKWARD,
    TOGGLE_MOVE_LEFT,
    TOGGLE_MOVE_RIGHT,
    SEND_MESSAGE_TO_CHAT,
    SCREEN_TOUCH;
}
