package core.EventHandling.Logging;

import core.Window;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import static core.Window.defPath;
import static org.lwjgl.glfw.GLFW.glfwGetVersionString;

public class logger {
    public static boolean err = false, cleanup = false;
    public static void log(String message) {
        if (config.jetFromConfig("Debug").equals("true")) {
            System.out.println(message);
            if (!cleanup) {
                try {
                    cleanup = true;
                    FileWriter fileWriter = new FileWriter(defPath + "\\log.txt");
                    PrintWriter printWriter = new PrintWriter(fileWriter);

                    printWriter.print("");
                    printWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                FileWriter fileWriter = new FileWriter(defPath + "\\log.txt", true);
                PrintWriter printWriter = new PrintWriter(fileWriter);

                printWriter.println(message);
                printWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (!err) {
            err = true;
            System.err.println("logger: access denied, because debug false or null");
        }
    }

    public static void logExit(int status) {
        logger.log("program exit at: " + LocalDateTime.now() + "\n--------");
        System.exit(status);
    }

    public static void logStart() {
        logger.log("--------" + "\ninit: true" + "\nglfw version: " + glfwGetVersionString() + "\ngame version: " + Window.version + "\nstart time: " + LocalDateTime.now());
    }
}
