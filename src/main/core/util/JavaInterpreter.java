package core.util;

import core.Global;
import core.ui.widget.Console;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import jdk.jshell.JShell;
import jdk.jshell.execution.LocalExecutionControlProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.lang.management.ManagementFactory;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.function.Consumer;

public class JavaInterpreter {
    private static final Logger log = LogManager.getLogger("Console");

    private static ThreadFactory JSHELL_THREAD_FACTORY;

    public static JShell jshell;

    public static void init() {
        if (Debug.debugLevel < 3) {
            return;
        }
        JSHELL_THREAD_FACTORY = Thread.ofVirtual()
                .name("JShellThread", 0)
                .factory();

        execute(JavaInterpreter::init0);
    }

    private static void execute(Runnable action) {
        var vthread = JSHELL_THREAD_FACTORY.newThread(action);
        vthread.start();
    }

    private static void init0() {
        var runtimeMxBean = ManagementFactory.getRuntimeMXBean();

        var jvmArgs = runtimeMxBean.getInputArguments();
        int ver = Runtime.version().feature();

        var opts = new ArrayList<String>();
        for (String jvmArg : jvmArgs) {
            if (jvmArg.startsWith("--module-path")) {
                opts.add(jvmArg);
            }
        }
        var isModular = JavaInterpreter.class.getModule().getDescriptor() != null;
        if (!isModular) {
            opts.add("--module-path=" + runtimeMxBean.getClassPath());
        }

        opts.add("--enable-preview");
        opts.add("--release=" + ver);
        opts.add("--add-modules=core.main");

        var builder = JShell.builder()
                .executionEngine(new LocalExecutionControlProvider(), Map.of());
        builder.compilerOptions(opts.toArray(new String[0]));

        var comp = builder.build();

        if (!Global.assets.isExploded()) {
            // Этот магический ключик указывает на modules у jlink образа
            comp.addToClasspath(System.getProperty("sun.boot.library.path"));
        }

        jshell = comp;

        Consumer<String> out = (_) -> {};

        executeSync("import module java.base;", out);
        executeSync("import module core.main;", out);

        executeSync("import static core.Global.*;", out);
        executeSync("import static core.Application.*;", out);

        readScripts();
    }

    private static void readScripts() {
        var scriptsDir = Global.assets.assetsDir().resolve("scripts");
        try (var dirstr = Files.newDirectoryStream(scriptsDir, "*.java")) {
            StringBuilder sb = new StringBuilder();
            var sca = JavaInterpreter.jshell.sourceCodeAnalysis();
            for (Path jscript : dirstr) {
                try (var reader = Files.newBufferedReader(jscript)) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        sb.append(line);
                        String str = sb.toString();
                        var status = sca.analyzeCompletion(str);
                        var comp = status.completeness();

                        if (comp.isComplete()) {
                            JavaInterpreter.executeSync(str, _ -> {});
                            sb.setLength(0);
                        }
                    }
                } catch (IOException e) {
                    log.error("Failed to read script '{}'", jscript, e);
                } catch (Exception e) {
                    log.error("Failed to load script '{}'", jscript, e);
                }
            }
        } catch (IOException e) {
            log.error("Failed to read directory '{}'", scriptsDir, e);
        }
    }

    public static void execute(String snippet, Console console) {

        if (jshell == null) {
            return; // TODO не критично?
        }

        execute(() -> {
            var outLines = new ObjectArrayList<String>();
            executeSync(snippet, outLines::add);
            Global.scheduler.post(() -> {
                outLines.forEach(console::add);
            });
        });
    }

    public static void executeSync(String snippet, Consumer<String> out) {
        for (var snippetEvent : jshell.eval(snippet)) {
            switch (snippetEvent.status()) {
                case VALID, OVERWRITTEN -> {
                    if (snippetEvent.value() != null && !snippetEvent.value().isEmpty()) {
                        log.info("{} ==> {}", snippetEvent.snippet().id(), snippetEvent.value());
                        out.accept(snippetEvent.snippet().id() + " ==> " + snippetEvent.value());
                    } else {
                        if (snippetEvent.exception() != null) {
                            log.error("", snippetEvent.exception());
                            if (out != null) {
                                StringWriter sw = new StringWriter();
                                var str = new PrintWriter(sw);
                                snippetEvent.exception().printStackTrace(str);
                                for (String s : sw.toString().split("\n")) {
                                    out.accept(s);
                                }
                            }
                        } else {
                            log.trace("OK");
                            if (log.isTraceEnabled()) {
                                out.accept("OK");
                            }
                        }
                    }
                }
                case RECOVERABLE_DEFINED, DROPPED, NONEXISTENT -> {
                }
                case REJECTED, RECOVERABLE_NOT_DEFINED -> {
                    jshell.diagnostics(snippetEvent.snippet())
                            .forEach(diag -> {
                                if (diag.isError()) {
                                    log.error("Error:");
                                    out.accept("Error:");
                                    for (String line : diag.getMessage(Locale.US).split("\n")) {
                                        log.error(line);
                                        out.accept(line);
                                    }
                                    long start = diag.getStartPosition();
                                    long end = diag.getEndPosition();
                                    long pos = diag.getPosition();
                                    String source = snippetEvent.snippet().source();
                                    log.error(source);
                                    out.accept(source);
                                    String caret = "^".repeat(Math.toIntExact(end - pos));
                                    log.error("{}{}", " ".repeat(Math.toIntExact(start)), caret);
                                    out.accept(" ".repeat(Math.toIntExact(start)) + caret);
                                }
                            });

                }
            }
        }
    }

    public static void close() {
        var jsh = jshell;
        if (jsh != null) {
            jsh.stop();
            jsh.close();
        }
    }
}
