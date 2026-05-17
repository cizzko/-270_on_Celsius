package core.util;

import core.EventHandling.EventHandler;
import jdk.jshell.JShell;
import jdk.jshell.SnippetEvent;
import jdk.jshell.execution.LocalExecutionControlProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jetbrains.annotations.Nullable;

import java.lang.management.ManagementFactory;
import java.lang.module.ModuleDescriptor;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

public class JavaInterpreter {
    private static final Logger log = LogManager.getLogger();

    public static final JShell jshell = JShell.builder()
            .executionEngine(new LocalExecutionControlProvider(), Map.of())
            .build();

    public static final ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();

    public static void init(boolean exploded) {
        var runtimeMxBean = ManagementFactory.getRuntimeMXBean();
        var jvmArgs = runtimeMxBean.getInputArguments();
        for (String jvmArg : jvmArgs) {
            if (jvmArg.startsWith("--module-path")) {
                String list = jvmArg.substring("--module-path=".length());
                jshell.addToClasspath(list);
            }
        }
        if (!exploded) {
            jshell.addToClasspath(System.getProperty("sun.boot.library.path"));
        }

        execute0("import static core.Global.*;", null);
        execute0("import static core.Application.*;", null);
        var packages = JavaInterpreter.class.getModule().getDescriptor().exports().stream()
                .map(ModuleDescriptor.Exports::source)
                .collect(Collectors.toCollection(TreeSet::new));
        for (String aPackage : packages) {
            execute0("import " + aPackage + ".*;", null);
        }
    }

    public static void execute(String snippet) {
        exec.execute(() -> {
            Thread.currentThread().setName("JShell Thread");
            EventHandler.resetKeyLogginText();
            var out = new StringJoiner("\\n").setEmptyValue("");
            execute0(snippet, out);

            EventHandler.setKeyLoggingText(out.toString());
        });
    }

    public static void execute0(String snippet, @Nullable StringJoiner out) {
        for (SnippetEvent snippetEvent : jshell.eval(snippet)) {
            // log.debug(snippetEvent);
            switch (snippetEvent.status()) {
                case VALID, OVERWRITTEN -> {
                    if (snippetEvent.value() != null && !snippetEvent.value().isEmpty()) {
                        log.info("{} ==> {}", snippetEvent.snippet().id(), snippetEvent.value());
                        if (out != null)
                            out.add(snippetEvent.snippet().id() + " ==> " + snippetEvent.value());
                    } else {
                        if (snippetEvent.exception() != null) {
                            log.error(snippetEvent.exception());
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
                                    if (out != null)
                                        out.add("Error:");
                                    for (String line : diag.getMessage(Locale.US).split("\n")) {
                                        log.error(line);
                                        if (out != null)
                                            out.add(line);
                                    }
                                    long start = diag.getStartPosition();
                                    long end = diag.getEndPosition();
                                    long pos = diag.getPosition();
                                    String source = snippetEvent.snippet().source();
                                    log.error(source);
                                    if (out != null)
                                        out.add(source);
                                    String caret = "^".repeat(Math.toIntExact(end - pos));
                                    log.error("{}{}", " ".repeat(Math.toIntExact(start)), caret);
                                    if (out != null)
                                        out.add(" ".repeat(Math.toIntExact(start)) + caret);
                                }
                            });

                }
            }
        }
    }
}
