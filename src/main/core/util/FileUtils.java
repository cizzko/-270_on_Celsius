package core.util;

import core.Application;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

public class FileUtils {

    ///@return ArrayList<String> файлов в директории без расширения
    public static ArrayList<String> getNamesFromDir(Path dir) {
        try (var dirstr = Files.newDirectoryStream(dir)) {
            ArrayList<String> paths = new ArrayList<>();

            for (var path : dirstr) {
                String fileName = path.getFileName().toString();
                int dotIndex = fileName.lastIndexOf('.');
                if (dotIndex > 0) {
                    fileName = fileName.substring(0, dotIndex);
                }
                paths.add(fileName);
            }
            return paths;
        } catch (IOException e) {
            Application.log.error(e);
        }
        return null;
    }
}
