package com.chat.client;

import java.awt.Color;

public class Config {
    private Color backgroundColour;
    private Color textColour;
    private Color cursorColour;
    private final Color autoMessageColour = new Color(Color.red.getRGB());
    private int fontSize;

    private Config() {
    }

    public static Config getInstance(Color backgroundColour, Color textColour, Color cursorColour, int fontSize) {
        ConfigHolder.INSTANCE.backgroundColour = backgroundColour;
        ConfigHolder.INSTANCE.textColour = textColour;
        ConfigHolder.INSTANCE.cursorColour = cursorColour;
        ConfigHolder.INSTANCE.fontSize = fontSize;
        return ConfigHolder.INSTANCE;
    }

    private static class ConfigHolder {

        private static final Config INSTANCE = new Config();
    }

    public void setBackgroundColour(Color backgroundColour) {
        this.backgroundColour = backgroundColour;
    }

    public void setTextColour(Color textColour) {
        this.textColour = textColour;
    }

    public void setCursorColour(Color cursorColour) {
        this.cursorColour = cursorColour;
    }

    public void setFontSize(int fontSize) {
        this.fontSize = fontSize;
    }

    public Color getBackgroundColour() {
        return backgroundColour;
    }

    public Color getTextColour() {
        return textColour;
    }

    public Color getCursorColour() {
        return cursorColour;
    }

    public int getFontSize() {
        return fontSize;
    }

    public Color getAutoMessageColour() {
        return autoMessageColour;
    }
}
