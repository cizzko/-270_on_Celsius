package core;

import core.EventHandling.Logging.Logger;
import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.io.File;
import java.lang.reflect.Method;

public class ImportClassMethod {

    public static Object startMethod(String classPath, String methodName, Object[] args, Class<?> implementsClass) {
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            int result = compiler.run(null, null, null, classPath);
            if (result != 0) {
                throw new RuntimeException("Error at compiling .java file");
            }

            String fileName = new File(classPath).getName();
            int index = fileName.lastIndexOf('.');
            String className = fileName.substring(0, index);
            Class<?> loadedClass = Class.forName(className);

            if (implementsClass != null && !implementsClass.isAssignableFrom(loadedClass)) {
                throw new RuntimeException("Class at path: '" + classPath + "' not implements class: '" + implementsClass + "'");
            }

            Object instance = loadedClass.newInstance();
            Method method;

            if (args != null) {
                Class<?>[] argTypes = new Class[args.length];
                for (int i = 0; i < args.length; i++) {
                    argTypes[i] = args[i].getClass();
                }
                method = loadedClass.getMethod(methodName, argTypes);
                return method.invoke(instance, args);
            } else {
                method = loadedClass.getMethod(methodName);
                return method.invoke(instance);
            }

        } catch (Exception e) {
            Logger.log("Some error at start method, class path: '" + classPath + "', method: '" + methodName + "', exception: '" + e + "'");
            return null;
        }
    }
}
