package core.EventHandling.Logging;

import core.Window;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import static core.EventHandling.Logging.config.jetFromConfig;
import static core.Window.*;
import static org.lwjgl.glfw.GLFW.*;

public class logger {
    public static boolean err = false, cleanup = false, debug = Boolean.parseBoolean(jetFromConfig("Debug"));

    public static void log(String message, boolean forcibly) {
        if (debug || forcibly) {
            System.out.println(message);

            if (!cleanup) {
                try {
                    cleanup = true;
                    PrintWriter printWriter = new PrintWriter(new FileWriter(defPath + "\\log.txt"));

                    printWriter.print("");
                    printWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                PrintWriter printWriter = new PrintWriter(new FileWriter(defPath + "\\log.txt", true));

                printWriter.println(message);
                printWriter.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else if (!err) {
            err = true;
            log("See config. Access denied, because debug false or null.", true);
        }
    }

    public static void log(String message) {
        log(message, false);
    }

    public static void logExit(int status) {
        glfwDestroyWindow(glfwWindow);
        logger.log("program exit at: " + LocalDateTime.now() + "\n--------");
        System.exit(status);
    }

    public static void logStart() {
        logger.log("--------" + "\nGLFW version: " + glfwGetVersionString() + "\nGame version: " + Window.version + "\nStart time: " + LocalDateTime.now() + "\n--------");
        logger.log("Screen width: " + width + "\nScreen height: " + height + "\nFull screen: " + jetFromConfig("FullScreen") + "\nVertical sync: " + config.jetFromConfig("VerticalSync") + " (" + verticalSync + ")" + "\nLanguage: " + jetFromConfig("Language"));
    }
}
