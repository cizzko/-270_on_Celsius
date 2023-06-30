package core.EventHandling.Logging;

import com.sun.management.OperatingSystemMXBean;
import core.AnonymousStatistics;
import core.Window;
import core.World.Weather.Sun;

import java.awt.*;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.Random;

import static core.EventHandling.Logging.Config.getFromConfig;
import static core.Window.*;
import static org.lwjgl.glfw.GLFW.*;

public class Logger {
    private static final long sessionId = (long) (new Random().nextDouble() * Long.MAX_VALUE);
    public static boolean cleanup = false, debug = Boolean.parseBoolean(getFromConfig("Debug"));

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
        }
    }

    public static void log(String message) {
        log(message, false);
    }

    public static void logExit(int status, String reason) {
        String exit;

        if (reason != null) {
            log("\nExit reason: " + reason);
        } else {
            reason = "none";
        }

        if (status == 0) {
            exit = " (normal)";
        } else if (status == 1863) {
            exit = " (sudden closure)";
        } else if (status == 1) {
            exit = " (critical error)";
        } else {
            exit = " (unknown state)";
        }

        AnonymousStatistics.sendStateMessage("Session '" + sessionId + "' exit, time: '" + LocalDateTime.now() + "', reason: '" + reason + "', status: " + status + exit);
        log("\nProgram exit at: " + LocalDateTime.now() + "\nExit code: " + status + exit + "\nGame time: " + Sun.currentTime +  "\n-------- Log ended --------");

        glfwDestroyWindow(glfwWindow);
        System.exit(status);
    }

    public static void logExit(int status) {
        logExit(status, null);
    }

    public static void logStart() {
        String computerInfo = String.format(" | Разрешение экрана: %d x %d | Процессор: %s | Количество потоков: %s | Количество ОЗУ: %d MB", Toolkit.getDefaultToolkit().getScreenSize().width, Toolkit.getDefaultToolkit().getScreenSize().height, System.getenv("PROCESSOR_IDENTIFIER"), Runtime.getRuntime().availableProcessors(), ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class).getTotalMemorySize() / (1024 * 1024));
        String system = System.getProperty("os.name").toLowerCase();

        AnonymousStatistics.sendStateMessageThread("Session '" + sessionId + "' started, time: '" + LocalDateTime.now() + "', system info: " + system + computerInfo);

        log(!system.contains("windows 10") ? "Warning: " + System.getProperty("os.name") + " not supported!\n" : "" + "\nGLFW version: " + glfwGetVersionString() + "\nGame version: " + Window.version + "\n");
        log("Start time: " + LocalDateTime.now() + "\nPreload textures: " + getFromConfig("PreLoadTextures"));
        log("Vertical sync: " + Config.getFromConfig("VerticalSync") + " (" + verticalSync + ")" + "\n\nCurrent language: " + getFromConfig("Language"));
    }
}
