package core.Utils;

import java.io.File;
import java.util.*;

public class ArrayUtils {

    public static int findFreeCell(Object[] array) {
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] == null) {
                    return i;
                }
            }
        }
        return -1; //not found or null
    }

    public static int findEqualsObjects(Object[] array, Object equalsObject) {
        return (int) Arrays.stream(array).filter(obj -> obj != null && obj.equals(equalsObject)).count();
    }

    public static int findDistinctObjects(Object[] array) {
        return (int) Arrays.stream(array).filter(Objects::nonNull).distinct().count();
    }

    public static String[] getAllFiles(String directory, String filesExtension) {
        ArrayList<String> fileList = new ArrayList<>();
        Stack<File> stack = new Stack<>();
        stack.push(new File(directory));

        while (!stack.isEmpty()) {
            File currentFile = stack.pop();
            if (currentFile.isDirectory()) {
                File[] subFiles = currentFile.listFiles();
                if (subFiles != null) {
                    for (File subFile : subFiles) {
                        stack.push(subFile);
                    }
                }
            } else if (filesExtension == null || currentFile.getName().endsWith(filesExtension)) {
                fileList.add(currentFile.getAbsolutePath().replace('\\', '/'));
            }
        }
        return fileList.toArray(new String[0]);
    }

    public static String findFile(String directory, String fileName) {
        Stack<File> stack = new Stack<>();
        stack.push(new File(directory));

        while (!stack.isEmpty()) {
            File currentFile = stack.pop();
            if (currentFile.isDirectory()) {
                File[] subFiles = currentFile.listFiles();
                if (subFiles != null) {
                    for (File subFile : subFiles) {
                        stack.push(subFile);
                    }
                }
            } else if (currentFile.getName().equals(fileName)) {
                return currentFile.getAbsolutePath().replace('\\', '/');
            }
        }
        return null;
    }
}
