package core.Logging;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;

import static core.Window.defPath;

public class logger {
    public static boolean err = false, cleanup = false;
    public static void log(String message) {
        if (config.jetFromConfig("Debug").equals("true")) {
            System.out.println(message);
            if (!cleanup) {
                try {
                    cleanup = true;
                    FileWriter fileWriter = new FileWriter(defPath + "\\src\\assets\\log.txt");
                    PrintWriter printWriter = new PrintWriter(fileWriter);

                    printWriter.print("");
                    printWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
            try {
                FileWriter fileWriter = new FileWriter(defPath + "\\src\\assets\\log.txt", true);
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
}
