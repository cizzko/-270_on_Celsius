package core;

import core.EventHandling.Logging.Logger;

public class main {
    public static void main(String[] args) {
        Logger.log("-------- Log started -------- + \nStarting...");

        Window window = new Window();
        window.run();
    }
}