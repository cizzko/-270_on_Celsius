package core;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import java.awt.*;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.lang.reflect.Method;
import java.util.Arrays;
import static core.EventHandling.EventHandler.getKey;
import static core.EventHandling.EventHandler.getKeyClick;
import static core.EventHandling.Logging.Logger.logExit;
import static org.lwjgl.glfw.GLFW.*;

public class Commandline {
    public static boolean created = false;

    public static void startMethod(String targetMethod, Object[] args) {
        if (targetMethod.equals("Exit") || targetMethod.equals("exit") && args != null) {
            logExit(Integer.parseInt((String) args[0]), "Exit from console");
        }
        if (targetMethod.contains("sendStateMessage")) {
            EventHandler.keyLoggingText = "No access to send state message";
            return;
        }

        //package.class.method argumentX
        //X - data type
        try {
            String[] strings = targetMethod.split("\\.");
            String className = String.join(".", Arrays.copyOfRange(strings, 0, strings.length - 1));
            String methodName = strings[strings.length - 1];

            Object[] convertedArgs = new Object[args == null ? 0 : args.length];
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    convertedArgs[i] = convertToType((String) args[i]);
                }
            }
            Class<?> targetClass = Class.forName(className);

            Method[] methods = targetClass.getMethods();
            Method method = null;
            for (Method m : methods) {
                if (m.getName().equals(methodName)) {
                    method = m;
                    break;
                }
            }

            if (method == null) {
                throw new NoSuchMethodException("Method not found");
            }

            Object result = method.invoke(null, convertedArgs);
            EventHandler.keyLoggingText = result != null ? "Returned: " + result : "Successfully";
        } catch (Exception e) {
            EventHandler.keyLoggingText = e.toString();
        }
    }

    private static Object convertToType(String arg) {
        if (arg.endsWith("l")) {
            arg = arg.substring(0, arg.length() - 1);
            return Long.parseLong(arg.substring(0, arg.length() - 1));

        } else if (arg.endsWith("d")) {
            arg = arg.substring(0, arg.length() - 1);
            return Double.parseDouble(arg.substring(0, arg.length() - 1));

        } else if (arg.endsWith("f")) {
            arg = arg.substring(0, arg.length() - 1);
            return Float.parseFloat(arg.substring(0, arg.length() - 1));

        } else if (arg.equals("true") || arg.equals("false")) {
            return Boolean.parseBoolean(arg);

        } else if (arg.endsWith("i")) {
            arg = arg.substring(0, arg.length() - 1);
            return Integer.parseInt(arg);

        } else {
            return arg;
        }
    }

    public static void createLine(String text) {
        if (Config.getFromConfig("Debug").equals("true") && !created) {
            if (text != null) {
                EventHandler.keyLoggingText = text;
            }

            EventHandler.startKeyLogging();
            created = true;
        }
    }

    public static void deleteLine() {
        EventHandler.endKeyLogging();
        created = false;
    }

    public static void updateLine() {
        if (Commandline.created && getKeyClick(GLFW_KEY_F5)) {
            Commandline.deleteLine();
        }
        if (getKeyClick(GLFW_KEY_F5) && !Commandline.created) {
            Commandline.createLine();
        }

        if (created) {
            if (getKeyClick(GLFW_KEY_ENTER)) {
                try {
                    String[] parts = EventHandler.keyLoggingText.split("\\s+");
                    String method = parts[0];

                    Object[] args;
                    if (parts.length > 1) {
                        String[] argStrings = Arrays.copyOfRange(parts, 1, parts.length);
                        args = Arrays.stream(argStrings).map(s -> (Object) s).toArray();
                    } else {
                        args = null;
                    }

                    startMethod(method, args);
                } catch (Exception e) {
                    EventHandler.keyLoggingText = e.toString();
                }
            }

            if (getKey(GLFW_KEY_LEFT_CONTROL) && getKey(GLFW_KEY_V)) {
                Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
                Transferable transferable = clipboard.getContents(null);

                if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    try {
                        EventHandler.keyLoggingText = (String) transferable.getTransferData(DataFlavor.stringFlavor);
                        Thread.sleep(200);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    public static void createLine() {
        createLine(null);
    }
}
