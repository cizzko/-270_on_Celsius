package core;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.UI.GUI.Menu.CreatePlanet;
import core.UI.GUI.Menu.Main;
import core.World.Creatures.Physics;
import core.World.Creatures.Player;

import java.awt.*;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static core.EventHandling.EventHandler.getKey;
import static core.EventHandling.EventHandler.getKeyClick;
import static core.EventHandling.Logging.Logger.logExit;
import static org.lwjgl.glfw.GLFW.*;

public class Commandline {
    public static boolean created = false;

    private static void startReflection(String target) {
        if (target.equals("ExitGame") || target.equals("exitGame")) {
            logExit(1863, "Exit from console", true);

        } else if (target.trim().equals("noclip")) {
            Player.noClip = true;
            Physics.physicsSpeed = 5000;

        } else if (target.trim().equals("modify")) {
            EventHandler.keyLoggingText = "modify.. what modify? <start package.class.field valueX> x - type of value, example: 'core.World.Creatures.Physics.physicsSpeed 400i'";

        } else if (target.trim().equals("start")) {
            EventHandler.keyLoggingText = "start.. what start? <start package.class.method arg1X arg2X..> x - type of value, example: 'start core.EventHandling.Logging.Logger.logExit 0i'";

        } else if (target.contains("sendStateMessage")) {
            EventHandler.keyLoggingText = "No access to send state message";

        } else if (target.contains("modify")) {
            target = target.substring(7);
            modifyField(target);

        } else if (target.contains("start")) {
            target = target.substring(6);
            startMethod(target);
        }
    }

    private static void modifyField(String target) {
        try {
            String[] parts = target.split("\\s+");
            target = parts[0];

            String[] strings = target.split("\\.");
            String className = String.join(".", Arrays.copyOfRange(strings, 0, strings.length - 1));
            String fieldName = strings[strings.length - 1];

            Class<?> clazz = Class.forName(className);
            Field field = clazz.getDeclaredField(fieldName);
            field.set(null, convertToType(parts[parts.length - 1]));

            EventHandler.keyLoggingText = fieldName + " modified to " + convertToType(parts[parts.length - 1]);
        } catch (Exception e) {
            EventHandler.keyLoggingText = e.toString();
        }
    }

    private static void startMethod(String target) {
        String[] parts = target.split("\\s+");
        target = parts[0];

        Object[] args;
        if (parts.length > 1) {
            String[] argStrings = Arrays.copyOfRange(parts, 1, parts.length);
            args = Arrays.stream(argStrings).map(s -> (Object) s).toArray();
        } else {
            args = null;
        }

        try {
            String[] strings = target.split("\\.");
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

    public static void createLine() {
        EventHandler.startKeyLogging();
        created = true;
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
                startReflection(EventHandler.keyLoggingText);
            }

            if (getKey(GLFW_KEY_LEFT_CONTROL) && getKey(GLFW_KEY_V)) {
                Transferable transferable = Toolkit.getDefaultToolkit().getSystemClipboard().getContents(null);

                if (transferable != null && transferable.isDataFlavorSupported(DataFlavor.stringFlavor)) {
                    try {
                        EventHandler.keyLoggingText += (String) transferable.getTransferData(DataFlavor.stringFlavor);
                        Thread.sleep(200);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }
}
