package com.github.jesusmrs05.mcforgecommander.common;

import java.io.Serializable;
import java.util.function.Predicate;

public class ServerPacket implements Serializable {
    private final Serializable data;
    private final Type type;

    public ServerPacket(Serializable data, Type type) {
        if(!type.test(data)){
            throw new IllegalArgumentException("Data type does not match the specified packet type");
        }
        this.data = data;
        this.type = type;
    }

    public Type getType() {
        return type;
    }
    public Serializable getData() {
        return data;
    }

    public enum Type implements Serializable, Predicate<Serializable> {
        IMAGE{
            @Override
            public boolean test(Serializable t) {
                return t instanceof byte[];
            }
        },
        GUI_STATUS{
            @Override
            public boolean test(Serializable t) {
                return t instanceof GUIStatus;
            }
        };
    }

    public enum GUIStatus implements Serializable {
        CAMERA,
        MAIN_MENU,
        OTHER;
    }
}