package core.Utils;

import core.EventHandling.EventHandler;
import core.EventHandling.Logging.Config;
import core.EventHandling.Logging.Logger;
import core.Window;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

import static core.EventHandling.EventHandler.keyLoggingText;
import static core.Global.*;
import static org.lwjgl.glfw.GLFW.*;

public class Commandline {
    private static final String prefix = Config.getFromFC("Prefix");
    public static boolean created = false;

    private static void startReflection(String target) {
        if (target.equalsIgnoreCase("help")) {
            EventHandler.setKeyLoggingText("Prefix: " + prefix + ", write '" + prefix + " help'");
            return;
        }
        if (target.startsWith(prefix) && target.trim().length() > prefix.length()) {
            target = Config.getFromFC(target.substring(prefix.length() + 1));

            if (target == null || target.equals("null") || target.contains("sendStateMessage")) {
                EventHandler.setKeyLoggingText("No access or target is null");
                return;
            }
            if (target.startsWith("output:")) {
                EventHandler.setKeyLoggingText(target.substring(7));
            }
        } else {
            EventHandler.setKeyLoggingText("Command not found");
        }

        switch (target.trim().split("\\s+")[0]) {
            case "modify" -> modifyField(target.substring(7));
            case "start" -> startMethod(target.substring(6));
            case "eval" -> {
                final String[] targetMethod = target.split(" ");
                new Thread(() -> EventHandler.setKeyLoggingText(ImportClassMethod.startMethod(targetMethod[1], targetMethod[targetMethod.length - 1], null, null))).start();
            }
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

            EventHandler.setKeyLoggingText(fieldName + " modified to " + convertToType(parts[parts.length - 1]));
        } catch (Exception e) {
            EventHandler.setKeyLoggingText(e.getMessage());
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
                Logger.printException("Method not found", new NoSuchMethodException("Target = " + target));
            }

            Object result = method.invoke(null, convertedArgs);
            EventHandler.setKeyLoggingText(result != null ? "Returned: " + result : "Successfully");
        } catch (Exception e) {
            EventHandler.setKeyLoggingText(e.getMessage());
        }
    }

    private static Object convertToType(String arg) {
        arg = arg.trim();

        if (Character.isDigit(arg.charAt(arg.length() - 2))) {
            char type = arg.charAt(arg.length() - 1);
            arg = arg.substring(0, arg.length() - 1);

            switch (type) {
                case 'l' -> { return Long.parseLong(arg);     }
                case 'd' -> { return Double.parseDouble(arg); }
                case 'f' -> { return Float.parseFloat(arg);   }
                case 'i' -> { return Integer.parseInt(arg);   }
            }
        }

        return (arg.equals("true") || arg.equals("false")) ? Boolean.parseBoolean(arg) : arg;
    }

    public static void createLine() {
        EventHandler.startKeyLogging();
        EventHandler.resetKeyLogginText();
        created = true;
    }

    public static void deleteLine() {
        EventHandler.endKeyLogging();
        created = false;
    }

    public static void inputUpdate() {
        if (input.justPressed(GLFW_KEY_F5)) {
            if (created) {
                Commandline.deleteLine();
            } else {
                Commandline.createLine();
            }
        }
        if (input.justPressed(GLFW_KEY_DELETE)) {
            EventHandler.resetKeyLogginText();
        }

        if (created) {
            if (input.justPressed(GLFW_KEY_ENTER)) {
                startReflection(keyLoggingText.toString());
            }

            if (input.pressed(GLFW_KEY_LEFT_CONTROL) && input.justPressed(GLFW_KEY_V)) {
                String text = glfwGetClipboardString(Window.glfwWindow);
                if (text != null) {
                    keyLoggingText.append(text);
                }
            }
        }
    }
}
