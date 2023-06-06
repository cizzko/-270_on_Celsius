package core.EventHandling.Logging;

import core.Window;
import core.World.Weather.Sun;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDateTime;
import static core.EventHandling.Logging.Config.jetFromConfig;
import static core.Window.*;
import static org.lwjgl.glfw.GLFW.*;

public class Logger {
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
            log("See Config. Access denied, because debug false or null.", true);
        }
    }

    public static void log(String message) {
        log(message, false);
    }

    public static void logExit(int status) {
        String exit;

        if (status == 0) {
            exit = " (normal)";
        } else if (status == 1863) {
            exit = " (sudden closure)";
        } else if (status == 1) {
            exit = " (critical error)";
        } else {
            exit = " (unknown state)";
        }

        glfwDestroyWindow(glfwWindow);
        Logger.log("\nProgram exit at: " + LocalDateTime.now() + "\nExit code: " + status + exit + "\nTotal frames: " + totalFrames + "\nGame time: " + Sun.currentTime +  "\n-------- Log ended --------");
        System.exit(status);
    }

    public static void logStart() {
        Logger.log("-------- Log started --------" + "\nGLFW version: " + glfwGetVersionString() + "\nGame version: " + Window.version + "\nStart time: " + LocalDateTime.now() + "\n");
        Logger.log("Screen width: " + width + "\nScreen height: " + height + "\nFull screen: " + jetFromConfig("FullScreen"));
        Logger.log("Vertical sync: " + Config.jetFromConfig("VerticalSync") + " (" + verticalSync + ")" + "\n\nCurrent language: " + jetFromConfig("Language"));
    }
}
