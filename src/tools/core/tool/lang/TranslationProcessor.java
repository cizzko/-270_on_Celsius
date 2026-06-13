package core.tool.lang;

import com.fasterxml.jackson.core.util.DefaultPrettyPrinter;
import com.fasterxml.jackson.core.util.Separators;
import com.fasterxml.jackson.databind.ObjectMapper;
import core.lang.*;
import it.unimi.dsi.fastutil.ints.IntArrayList;
import it.unimi.dsi.fastutil.objects.ObjectOpenHashSet;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.*;
import org.objectweb.asm.tree.analysis.*;

import java.io.IOException;
import java.lang.classfile.*;
import java.lang.classfile.constantpool.*;
import java.lang.constant.ClassDesc;
import java.lang.constant.MethodTypeDesc;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

import static core.lang.TrMap.TR_ORDER;
import static core.lang.TrMap.makeTrMap;
import static java.lang.classfile.Attributes.*;
import static java.lang.classfile.ClassFile.ACC_ABSTRACT;
import static java.lang.classfile.ClassFile.ACC_NATIVE;

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

        var cf = ClassFile.of();

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

        markedMethods.forEach(TranslationProcessor::printMethod);

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

    private static void printMethod(MethodRef ref, MethodInfo methodInfo) {
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
        byte[] bytecode;
        try {
            bytecode = Files.readAllBytes(file);
        } catch (IOException e) {
            e.printStackTrace();
            return Map.of();
        }

        {
            var classModel = cf.parse(bytecode);
            boolean any = false;
            for (var poolEntry : classModel.constantPool()) {
                if (poolEntry instanceof MemberRefEntry e && isMethodRefEntry(e) && markedMethods.containsKey(makeKey(e))) {
                    any = true;
                    break;
                }
            }
            if (!any) {
                return Map.of();
            }
        }

        var cr = new ClassReader(bytecode);
        var cn = new ClassNode();
        cr.accept(cn, 0);

        var trMap = makeTrMap();
        var cd = ClassDesc.ofInternalName(cn.name);
        var className = baseDir.resolve(cd.packageName().replace('.', '/'), cn.sourceFile);

        int currentLineNumber = -1;

        var interp = new SourceInterpreter();
        for (MethodNode mn : cn.methods) {
            if ((mn.access & (ACC_ABSTRACT | ACC_NATIVE)) != 0) {
                continue;
            }

            var analyzer = new Analyzer<>(interp);
            Frame<SourceValue>[] frames;
            try {
                frames = analyzer.analyze(cn.name, mn);
            } catch (AnalyzerException e) {
                e.printStackTrace();
                continue;
            }

            var insnList = mn.instructions;
            for (int i = 0, n = insnList.size(); i < n; i++) {
                var frame = frames[i];
                var asmInstr = insnList.get(i);

                if (asmInstr instanceof LineNumberNode lineNode) {
                    currentLineNumber = lineNode.line;
                }

                if (asmInstr.getType() != AbstractInsnNode.METHOD_INSN) {
                    continue;
                }

                var invoke = (MethodInsnNode) asmInstr;
                if (frame == null || invoke.owner.indexOf('[') != -1) {
                    continue;
                }
                var desc = MethodTypeDesc.ofDescriptor(invoke.desc);
                var marked = markedMethods.get(new MethodRef(ClassDesc.ofInternalName(invoke.owner), invoke.name, desc));
                if (marked == null) {
                    continue;
                }

                int begin = frame.getStackSize() - desc.parameterCount();
                for (int index : marked.indexes) {
                    var paramsInsnList = frame.getStack(index + begin);
                    for (var insn : paramsInsnList.insns) {
                        if (insn instanceof LdcInsnNode ldc && ldc.cst instanceof String text) {
                            String lineLink = className + ":" + currentLineNumber;
                            trMap.computeIfAbsent(text, k -> new TrLine(k, new ArrayList<>()))
                                    .comments().add(lineLink);
                        }
                    }
                }
            }
        }
        return trMap;
    }

    static boolean isMethodRefEntry(MemberRefEntry e) {
        return !(e instanceof FieldRefEntry);
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
        return processMarkedMethod(classModel);
    }

    private static HashMap<MethodRef, MethodInfo> processMarkedMethod(ClassModel classModel) {
        var map = new HashMap<MethodRef, MethodInfo>();
        for (var method : classModel.methods()) {
            var attr = method.findAttribute(runtimeInvisibleParameterAnnotations())
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
