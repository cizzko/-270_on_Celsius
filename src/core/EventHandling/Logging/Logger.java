package core.EventHandling.Logging;

import com.sun.management.OperatingSystemMXBean;
import core.AnonymousStatistics;
import core.Window;
import core.World.Weather.Sun;
import java.awt.*;
import java.io.*;
import java.lang.management.GarbageCollectorMXBean;
import java.lang.management.ManagementFactory;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Random;
import static core.EventHandling.Logging.Config.getFromConfig;
import static core.Window.*;
import static org.lwjgl.glfw.GLFW.*;

public class Logger extends PrintStream {
    private static final long sessionId = (long) (new Random().nextDouble() * Long.MAX_VALUE);
    public static boolean cleanup = false;
    private static byte[] lastErrBuf;
    public Logger() {
        super(System.err);
    }

    @Override
    public void write(byte[] buf, int off, int len) {
        if (!Arrays.equals(buf, lastErrBuf)) {
            lastErrBuf = buf;
            log("Intercepted message from `System.out`: " + new String(buf, off, len));
        }
    }

    public static void printException(String message, Throwable exception) {
        StackTraceElement[] stackTrace = exception.getStackTrace();
        StringBuilder stackTraceMessage = new StringBuilder();

        if (Integer.parseInt(getFromConfig("Debug")) == 2) {
            stackTraceMessage.append("\n-------- Error detected -------- \nMessage: '").append(message);
            stackTraceMessage.append("', exception message: '").append(exception.getMessage());
            stackTraceMessage.append("', cause: '").append(exception.getCause());
            stackTraceMessage.append("', total stack trace length: '").append(stackTrace.length);
            stackTraceMessage.append("', stack trace: ");

            for (StackTraceElement stackTraceElement : stackTrace) {
                stackTraceMessage.append("\nClass name: ").append(stackTraceElement.getClassName());
                stackTraceMessage.append("\nMethod name: ").append(stackTraceElement.getMethodName());
                stackTraceMessage.append("\nLine: ").append(stackTraceElement.getLineNumber());
                stackTraceMessage.append("\nIs native: ").append(stackTraceElement.isNativeMethod());
            }
            stackTraceMessage.append("-------- Error end --------");

        } else if (Integer.parseInt(getFromConfig("Debug")) == 1) {
            stackTraceMessage.append("\nError message: '").append(message);
            stackTraceMessage.append("', exception message: '").append(exception.getMessage()).append("' ");
        }

        log(stackTraceMessage.toString());
    }

    public static void log(String message) {
        if (Integer.parseInt(getFromConfig("Debug")) > 0) {
            System.out.println(message);

            if (!cleanup) {
                try (PrintWriter printWriter = new PrintWriter(new FileWriter(defPath + "\\log.txt"))) {
                    printWriter.print("");
                    cleanup = true;
                } catch (IOException e) {
                    printException("Error when cleanup log", e);
                }
            }

            try (PrintWriter printWriter = new PrintWriter(new FileWriter(defPath + "\\log.txt", true))) {
                printWriter.println(message);
            } catch (IOException e) {
                printException("Error when print to log", e);
            }
        }
    }

    public static void logExit(int status, String reason, boolean exitOnProgram) {
        StringBuilder exitMessage = getExitMessage(status, reason);
        AnonymousStatistics.sendStateMessage("Session '" + sessionId + "' ended, " + exitMessage);
        log(exitMessage.toString());

        if (exitOnProgram) {
            glfwDestroyWindow(glfwWindow);
            System.exit(status);
        }
    }

    private static StringBuilder getExitMessage(int status, String reason) {
        StringBuilder exitMessage = new StringBuilder();

        String exitStatus = switch (status) {
            case 0 -> " (normal)";
            case 1 -> " (critical error)";
            case 1863 -> " (sudden closure)";
            case 6553 -> " (glfw error)";
            default -> " (unknown state)";
        };

        for (GarbageCollectorMXBean gcBean : ManagementFactory.getGarbageCollectorMXBeans()) {
            exitMessage.append("\nName GC: ").append(gcBean.getName());
            exitMessage.append("\nFull collection cycles GC: ").append(gcBean.getCollectionCount());
            exitMessage.append("\nFull time GC (ms): ").append(gcBean.getCollectionTime());
            exitMessage.append("\nRAM use: ").append(ManagementFactory.getMemoryMXBean().getHeapMemoryUsage());
            break;
        }
        exitMessage.append("\nProgram exit at: ").append(LocalDateTime.now());
        exitMessage.append("\nExit code: ").append(status).append(exitStatus).append(", reason: ").append(reason);
        exitMessage.append("\nGame time: ").append(Sun.currentTime);
        exitMessage.append("\n-------- Log ended --------");

        return exitMessage;
    }

    public static void logExit(int status) {
        logExit(status, null, true);
    }

    public static void logStart() {
        System.setErr(new Logger());
        Json.detectLanguage();

        StringBuilder message = getStartMessage();
        StringBuilder computerInfo = getComputerInfo();

        AnonymousStatistics.sendStateMessageThread("Session '" + sessionId + "' started\n" + message + computerInfo);
        log(message.toString() + computerInfo);
    }

    private static StringBuilder getComputerInfo() {
        StringBuilder computerInfo = new StringBuilder();

        computerInfo.append("\nComputer info: ");
        computerInfo.append("\nScreen resolution: ").append(Toolkit.getDefaultToolkit().getScreenSize().width).append(" x ").append(Toolkit.getDefaultToolkit().getScreenSize().height);
        computerInfo.append("\nCPU: ").append(System.getenv("PROCESSOR_IDENTIFIER"));
        computerInfo.append("\nCPU threads count:" ).append(Runtime.getRuntime().availableProcessors());
        computerInfo.append("\nRAM: ").append(ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class).getTotalMemorySize() / (1024 * 1024)).append("\n");

        return computerInfo;
    }

    private static StringBuilder getStartMessage() {
        StringBuilder message = new StringBuilder();

        message.append(!System.getProperty("os.name").toLowerCase().contains("windows 10") ? "Warning: " + System.getProperty("os.name") + " not supported!\n" : "");
        message.append("\nGLFW version: ").append(glfwGetVersionString());
        message.append("\nGame version: " + Window.version);
        message.append("\nStart time: ").append(LocalDateTime.now());
        message.append("\n\nPreload textures: ").append(getFromConfig("PreloadTextures"));
        message.append("\nVertical sync: ").append(Config.getFromConfig("VerticalSync")).append(" (").append(verticalSync).append(")");
        message.append("\nCurrent language: ").append(getFromConfig("Language"));
        message.append("\nAvailable languages: ").append(Json.getAllLanguages().replace(" ", ", ")).append("\n");

        return message;
    }
}
