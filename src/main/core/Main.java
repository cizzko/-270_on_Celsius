package core;

import core.EventHandling.Logging.Logger;

public class Main {
    public static void main(String[] args) {
        Logger.log("-------- Log started -------- \nStarting...");

        new Window().run();
    }
}