package core.util;

import javax.tools.JavaCompiler;
import javax.tools.ToolProvider;
import java.lang.reflect.Method;

import static core.EventHandling.Logging.Logger.printException;

public class ImportClassMethod {

    public static String startMethod(String classPath, String methodName, Object[] args, Class<?> implementsClass) {
        try {
            JavaCompiler compiler = ToolProvider.getSystemJavaCompiler();
            int result = compiler.run(null, null, null, classPath);

            if (result != 0) {
                printException("Error at compiling .java file", new RuntimeException());
            }

            Class<?> loadedClass = Class.forName(classPath.replace("\\", ".").replaceAll(".*src\\.", "").replace(".java", ""));

            if (implementsClass != null && !implementsClass.isAssignableFrom(loadedClass)) {
                printException("Class at path: '" + classPath + "' not implements class: '" + implementsClass + "'", new RuntimeException());
            }

            Object instance = loadedClass.newInstance();
            Method method;

            if (args != null) {
                Class<?>[] argTypes = new Class[args.length];
                for (int i = 0; i < args.length; i++) {
                    argTypes[i] = args[i].getClass();
                }
                method = loadedClass.getMethod(methodName, argTypes);
                Object startResult = method.invoke(instance, args);

                return startResult == null ? "Successfully" : "Returned: " + startResult;
            } else {
                method = loadedClass.getMethod(methodName);
                Object startResult = method.invoke(instance);

                return startResult == null ? "Successfully" : "Returned: " + startResult;
            }

        } catch (Exception e) {
            printException("Some error at start method, class path: '" + classPath + "', method: '" + methodName, e);
        }
        return null;
    }
}
