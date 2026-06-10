package core.tool.lang;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.lang.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;

import java.io.IOException;
import java.lang.classfile.*;
import java.lang.classfile.constantpool.*;
import java.lang.classfile.instruction.ConstantInstruction;
import java.lang.classfile.instruction.InvokeInstruction;
import java.lang.classfile.instruction.LineNumber;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

import static core.lang.TrMap.TR_ORDER;
import static core.lang.TrMap.makeTrMap;

public final class TranslationProcessor {

    static final ClassDesc CD_Translation = LangTranslation.Translation.class.describeConstable().orElseThrow();

    record MethodRef(ClassDesc classDesc, String methodName, MethodTypeDesc methodTypeDesc) {
    }

    static MethodRef makeKey(MemberRefEntry e) {
        return switch (e) {
            case MethodRefEntry m -> new MethodRef(m.owner().asSymbol(), m.name().stringValue(), m.typeSymbol());
            case InterfaceMethodRefEntry m -> new MethodRef(m.owner().asSymbol(), m.name().stringValue(), m.typeSymbol());
            default -> throw new IllegalArgumentException("Unknown method ref: " + e);
        };
    }

    record MethodInfo(IntArrayList indexes) {
    }

    static boolean hasTranslationAnnotation(List<Annotation> annotations) {
        for (Annotation annotation : annotations) {
            if (annotation.className().isFieldType(CD_Translation)) {
                return true;
            }
        }
        return false;
    }

    public static void main(String[] args) throws IOException {
        var classPathDir = Path.of("build/classes/java/main");
        var baseDir = Path.of("src/main");

        boolean removeUnknownKeys = false;

        var assetsDir = Path.of("src/assets");
        var langsDir = assetsDir.resolve("langs");
        var langsFile = langsDir.resolve("langs.json");

        var objectMapper = new ObjectMapper();

        var dpp = new DefaultPrettyPrinter(Separators.createDefaultInstance()
                .withObjectFieldValueSpacing(Separators.Spacing.AFTER));
        dpp.indentArraysWith(new ArrayIndenter());
        objectMapper.setDefaultPrettyPrinter(dpp);

        LanguageSettings languageSettings;
        try (var is = Files.newInputStream(langsFile)) {
            languageSettings = objectMapper.readValue(is, LanguageSettings.class);
        }

        var cf = ClassFile.of(ClassFile.DebugElementsOption.DROP_DEBUG);

        HashMap<MethodRef, MethodInfo> markedMethods;
        List<Path> classFiles;

        long t = System.currentTimeMillis();
        try (var str = Files.walk(classPathDir)) {
            classFiles = str.filter(file -> isRegularClassFile(file.getFileName().toString())).toList();
        }

        System.out.println("Class file scan: " + ((System.currentTimeMillis() - t) / 1000f) + "s");
        t = System.currentTimeMillis();

        markedMethods = classFiles.parallelStream()
                .map(file -> parseMarkedMethods(cf, file))
                .collect(HashMap::new, HashMap::putAll, HashMap::putAll);

        System.out.println("Marked method collect: " + ((System.currentTimeMillis() - t) / 1000f) + "s");

        markedMethods.forEach((ref, methodInfo) -> {

            System.out.print("| " + canonicalClass(ref.classDesc) + "." + ref.methodName + "(");
            var methodTypeDesc = ref.methodTypeDesc;
            var indexes = methodInfo.indexes;
            int parameterCount = methodTypeDesc.parameterCount();
            for (int i = 0; i < parameterCount; i++) {
                if (indexes.contains(i)) {
                    System.out.print("@");
                    System.out.print(canonicalClass(CD_Translation));
                    System.out.print(' ');
                }
                System.out.print(canonicalClass(methodTypeDesc.parameterType(i)));
                if (i != parameterCount - 1) {
                    System.out.print(", ");
                }
            }
            System.out.println(')');
        });

        t = System.currentTimeMillis();
        var trRefMap = classFiles.parallelStream()
                .map(file -> parseTrMap(cf, baseDir, file, markedMethods))
                .collect(TrMap::makeTrMap, TranslationProcessor::mergeTrMap, TranslationProcessor::mergeTrMap);
        System.out.println("Translation scan: " + ((System.currentTimeMillis() - t) / 1000f) + "s");
        System.out.println("Found keys: " + trRefMap.size());

        trRefMap.forEach((_, e) -> e.comments().sort(TR_ORDER));
        t = System.currentTimeMillis();

        var refFile = langsDir.resolve(languageSettings.fileName(languageSettings.reference()));
        var ctx = new Context(languageSettings, objectMapper, removeUnknownKeys);

        languageSettings.format().write(refFile, trRefMap, ctx);

        for (String lang : languageSettings.supported()) {
            var langFile = langsDir.resolve(languageSettings.fileName(lang));
            if (Files.exists(langFile)) {
                checkExistingTrFile(langFile, trRefMap, ctx);
            } else {
                System.out.println("Creating bundle for language '" + lang + "' from reference");
                Files.copy(refFile, langFile);
            }
        }

        System.out.println("Translation saving: " + ((System.currentTimeMillis() - t) / 1000f) + "s");
    }

    private static void checkExistingTrFile(Path file, TreeMap<String, TrLine> trRefMap, Context ctx) throws IOException {
        var trMap = ctx.languageSettings().format().read(file, ctx);

        var trKeys = new ObjectOpenHashSet<>(trMap.keySet());
        var trRefKeys = new ObjectOpenHashSet<>(trRefMap.keySet());
        boolean anyModification = false;
        if (!trKeys.containsAll(trRefKeys)) {
            var diff = new ObjectOpenHashSet<>(trRefKeys);
            diff.removeAll(trKeys);
            System.out.println("['" + file.getParent().relativize(file) + "'] Adding missing keys");

            {
                System.out.println("Missing keys:");
                for (String key : diff) {
                    System.out.print("| ");
                    System.out.print(key);
                    System.out.println();
                }
            }

            for (String key : diff) {
                trMap.put(key, trRefMap.get(key));
            }
            anyModification = true;
        }

        if (ctx.removeUnknownKeys()) {
            trKeys.removeAll(trRefKeys);
            if (!trKeys.isEmpty()) {

                System.out.println("['" + file.getParent().relativize(file) + "'] Removing unknown keys");
                {
                    System.out.println("Unknown keys:");
                    for (String key : trKeys) {
                        System.out.print("| ");
                        System.out.print(key);
                        System.out.println();
                    }
                }

                trMap.keySet().removeAll(trKeys);
                anyModification = true;
            }
        } else {
            var diff = new ObjectOpenHashSet<>(trKeys);
            diff.removeAll(trRefKeys);
            if (!diff.isEmpty()) {
                System.out.println("['" + file.getParent().relativize(file) + "'] Found unknown keys, passing...");
                {
                    System.out.println("Unknown keys:");
                    for (String key : diff) {
                        System.out.print("| ");
                        System.out.print(key);
                        System.out.println();
                    }
                }
            }
        }

        if (anyModification) {
            ctx.languageSettings().format().write(file, trMap, ctx);
        }
    }

    static void writePot(Path trDir, String fileName, TreeMap<String, ArrayList<String>> trMap) throws IOException {
        var dateTimeFormat = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mmZ");
        String header = """
                msgid ""
                msgstr ""
                "Project-Id-Version: PACKAGE VERSION\\n"
                "POT-Creation-Date: {POT_CREATION_DATE}\\n"
                "PO-Revision-Date: YEAR-MO-DA HO:MI+ZONE\\n"
                "Last-Translator: FULL NAME\\n"
                "Language-Team: LANGUAGE\\n"
                "Language: \\n"
                "MIME-Version: 1.0\\n"
                "Content-Type: text/plain; charset=CHARSET\\n"
                "Content-Transfer-Encoding: 8bit\\n"
                """
                .replace("{POT_CREATION_DATE}", dateTimeFormat.format(ZonedDateTime.now()));

        try (var wr = Files.newBufferedWriter(trDir.resolve(fileName + ".pot"))) {
            wr.write(header);
            wr.newLine();

            for (var entry : trMap.entrySet()) {
                String key = entry.getKey();

                for (String s : entry.getValue()) {
                    wr.write("#:");
                    wr.write(s);
                    wr.newLine();
                }

                wr.write("msgid \"");
                wr.write(key
                        .replace("\"", "\\\""));
                wr.write("\"");
                wr.newLine();

                wr.write("msgstr \"\"");
                wr.newLine();

                wr.newLine();
            }
        }
    }

    static void mergeTrMap(TreeMap<String, TrLine> lhs, Map<String, TrLine> rhs) {

        rhs.forEach((key, lines) -> {
            var old = lhs.putIfAbsent(key, lines);
            if (old != null) {
                old.comments().addAll(lines.comments());
            }
        });
    }

    static Map<String, TrLine> parseTrMap(ClassFile cf, Path baseDir, Path file,
                                          HashMap<MethodRef, MethodInfo> markedMethods) {
        ClassModel classModel;
        try {
            classModel = cf.parse(file);
        } catch (IOException e) {
            e.printStackTrace();
            return Map.of();
        }
        var cp = classModel.constantPool();

        boolean any = false;
        for (PoolEntry poolEntry : cp) {
            if (poolEntry instanceof MemberRefEntry e && isMethodRefEntry(e) && markedMethods.containsKey(makeKey(e))) {
                any = true;
                break;
            }
        }
        if (!any) {
            return Map.of();
        }

        var trMap = makeTrMap();
        var className =
                baseDir.resolve(classModel.thisClass().asSymbol().packageName()
                                .replace('.', '/'),
                        classModel.findAttribute(Attributes.sourceFile()).orElseThrow()
                                .sourceFile().stringValue());

        final int lineNumberOffset = 1; // или 0 для конфига выше
        for (var method : classModel.methods()) {
            method.code().ifPresent(code -> {
                var elementList = code.elementList();
                for (int j = 0; j < elementList.size(); j++) {
                    var codeElement = elementList.get(j);
                    if (!(codeElement instanceof InvokeInstruction i)) {
                        continue;
                    }
                    var methodInfo = markedMethods.get(makeKey(i.method()));
                    if (methodInfo == null) {
                        continue;
                    }
                    var instr = elementList.subList(Math.max(0, j - i.typeSymbol().parameterCount() - lineNumberOffset), j);
                    if (!hasLDCTranslation(instr)) {
                        continue;
                    }

                    String lineNumber = instr.stream()
                            .filter(c -> c instanceof LineNumber)
                            .findFirst()
                            .map(c -> (LineNumber) c)
                            .map(ln -> className + ":" + ln.line())
                            .orElseThrow();

                    var insnList = instr.stream()
                            .filter(c -> !(c instanceof LineNumber))
                            .toList();

                    for (int index : methodInfo.indexes) {
                        if (insnList.get(index) instanceof ConstantInstruction ci &&
                            ci.constantValue() instanceof String key) {

                            trMap.computeIfAbsent(key, s -> new TrLine(s, new ArrayList<>())).comments().add(lineNumber);
                        }
                    }
                }
            });
        }

        return trMap;
    }

    static boolean isMethodRefEntry(MemberRefEntry e) {
        return !(e instanceof FieldRefEntry);
    }

    static boolean hasLDCTranslation(List<CodeElement> instr) {
        for (CodeElement codeElement : instr) {
            if (!(codeElement instanceof ConstantInstruction) && !(codeElement instanceof LineNumber)) {
                return false;
            }
        }
        return true;
    }

    static String canonicalClass(ClassDesc classDesc) {
        String pkg = classDesc.packageName();
        String name = classDesc.displayName()
                .replace('$', '.');
        return pkg.isEmpty() ? name : pkg + '.' + name;
    }

    static Map<MethodRef, MethodInfo> parseMarkedMethods(ClassFile cf, Path file) {
        ClassModel classModel;
        try {
            classModel = cf.parse(file);
        } catch (IOException e) {
            e.printStackTrace();
            return Map.of();
        }
        var map = new HashMap<MethodRef, MethodInfo>();
        for (var method : classModel.methods()) {
            var attr = method.findAttribute(Attributes.runtimeInvisibleParameterAnnotations())
                    .orElse(null);
            if (attr == null) {
                continue;
            }

            var annotations = attr.parameterAnnotations();
            var indexes = new IntArrayList();
            for (int i = 0; i < annotations.size(); i++)
                if (hasTranslationAnnotation(annotations.get(i)))
                    indexes.add(i);

            if (indexes.isEmpty()) {
                continue;
            }

            map.put(
                    new MethodRef(classModel.thisClass().asSymbol(),
                            method.methodName().stringValue(),
                            method.methodTypeSymbol()),
                    new MethodInfo(indexes));
        }
        return map;
    }

    static boolean isRegularClassFile(String filename) {
        return !filename.equals("module-info.class") && filename.endsWith(".class");
    }

    static void processContent(Path contentDir, List<Path> subDirs) {
        for (Path dir : subDirs) {
            try (var dirstr = Files.newDirectoryStream(contentDir.resolve(dir), "*.json")) {
                for (Path file : dirstr) {
                    var fileNameExt = file.getFileName().toString();
                    String fileName = fileNameExt.substring(0, fileNameExt.lastIndexOf('.'));
                    String nameKey = dir + "." + fileName + ".name";
                    String descriptionKey = dir + "." + fileName + ".description";

                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
