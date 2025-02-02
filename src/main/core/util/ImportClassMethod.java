package core.util;

import core.EventHandling.EventHandler;
import jdk.jshell.JShell;
import jdk.jshell.SnippetEvent;
import jdk.jshell.execution.LocalExecutionControlProvider;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ImportClassMethod {
    private static final Logger log = LogManager.getLogger();

    public static final JShell jshell = JShell.builder()
            .executionEngine(new LocalExecutionControlProvider(), Map.of())
            .build();

    public static final ExecutorService exec = Executors.newVirtualThreadPerTaskExecutor();

    public static void execute(String snippet) {

        exec.execute(() -> {
            Thread.currentThread().setName("JShell Thread");
            EventHandler.resetKeyLogginText();
            var out = new StringJoiner("\\n").setEmptyValue("");

            for (SnippetEvent snippetEvent : jshell.eval(snippet)) {
                switch (snippetEvent.status()) {
                    case VALID, OVERWRITTEN -> {
                        log.info("{} ==> {}", snippetEvent.snippet().id(), snippetEvent.value());
                        out.add(snippetEvent.snippet().id() + " ==> " + snippetEvent.value());
                    }
                    case RECOVERABLE_DEFINED, DROPPED, RECOVERABLE_NOT_DEFINED, NONEXISTENT -> {}
                    case REJECTED -> {
                        jshell.diagnostics(snippetEvent.snippet())
                                .forEach(diag -> {
                                    if (diag.isError()) {
                                        log.error("Error:");
                                        out.add("Error:");
                                        for (String line : diag.getMessage(Locale.US).split("\n")) {
                                            log.error(line);
                                            out.add(line);
                                        }
                                        long start = diag.getStartPosition();
                                        long end = diag.getEndPosition();
                                        long pos = diag.getPosition();
                                        String source = snippetEvent.snippet().source();
                                        log.error(source);
                                        out.add(source);
                                        log.error("{}{}", " ".repeat(Math.toIntExact(start)), "^".repeat(Math.toIntExact(end - pos)));
                                        out.add(" ".repeat(Math.toIntExact(start)) + "^".repeat(Math.toIntExact(end - pos)));
                                    }
                                });

                    }
                }
            }

            EventHandler.setKeyLoggingText(out.toString());
        });
    }
}
