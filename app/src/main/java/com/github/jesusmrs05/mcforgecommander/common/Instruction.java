package com.github.jesusmrs05.mcforgecommander.common;

import java.io.Serializable;

public enum Instruction implements Serializable {
    TOGGLE_MOVE_FORWARD,
    TOGGLE_MOVE_BACKWARD,
    TOGGLE_MOVE_LEFT,
    TOGGLE_MOVE_RIGHT,
    SEND_MESSAGE_TO_CHAT,
    SCREEN_TOUCH,
    PRESS_CHAT_KEY,
    PRESS_INVENTORY_KEY,
    PRESS_MENU_KEY,
    PRESS_CERTAIN_HOTBAR_KEY,
    PRESS_JUMP_KEY,
    SET_FPS,
    SET_JPEG_QUALITY,
    LEFT_CLICK,
    RIGHT_CLICK,
}
