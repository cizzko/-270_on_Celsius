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

        //package.class.method
        try {
            String[] strings = targetMethod.split("\\.");
            String className = String.join(".", Arrays.copyOfRange(strings, 0, strings.length - 1));
            String methodName = strings[strings.length - 1];

            Class<?>[] argTypes = new Class<?>[args == null ? 0 : args.length];
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    argTypes[i] = args[i].getClass();
                }
            }

            Object[] convertedArgs = new Object[args == null ? 0 : args.length];
            if (args != null) {
                for (int i = 0; i < args.length; i++) {
                    convertedArgs[i] = convertToType(args[i], argTypes[i]);
                }
            }
            Class<?> targetClass = Class.forName(className);

            Method[] methods = targetClass.getMethods();
            Method method = null;
            for (Method m : methods) {
                if (m.getName().equals(methodName) && Arrays.equals(m.getParameterTypes(), argTypes)) {
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

    private static Object convertToType(Object arg, Class<?> type) {
        if (type.equals(int.class) || type.equals(Integer.class)) {
            return Integer.parseInt(arg.toString());
        } else if (type.equals(double.class) || type.equals(Double.class)) {
            return Double.parseDouble(arg.toString());
        } else if (type.equals(float.class) || type.equals(Float.class)) {
            return Float.parseFloat(arg.toString());
        } else if (type.equals(boolean.class) || type.equals(Boolean.class)) {
            return Boolean.parseBoolean(arg.toString());
        } else if (type.equals(char.class) || type.equals(Character.class)) {
            return arg.toString().charAt(0);
        } else if (type.equals(long.class) || type.equals(Long.class)) {
            return Long.parseLong(arg.toString());
        } else if (type.equals(short.class) || type.equals(Short.class)) {
            return Short.parseShort(arg.toString());
        } else if (type.equals(byte.class) || type.equals(Byte.class)) {
            return Byte.parseByte(arg.toString());
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
                        Thread.sleep(100);
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
