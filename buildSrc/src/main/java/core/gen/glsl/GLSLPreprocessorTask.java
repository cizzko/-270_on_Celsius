package core.gen.glsl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import org.gradle.api.DefaultTask;
import org.gradle.api.file.Directory;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.FileSystemOperations;
import org.gradle.api.provider.ProviderFactory;
import org.gradle.api.tasks.*;
import org.jspecify.annotations.NonNull;

import javax.inject.Inject;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

@CacheableTask
public abstract class GLSLPreprocessorTask extends DefaultTask {

    public static final String META_EXT = ".meta.json";
    public static final String VERT_EXT = ".vert";
    public static final String FRAG_EXT = ".frag";

    public static final String CLASS_NAME_POSTFIX = "Shader";
    public static final String BASE_PACKAGE = "core.gen";

    @InputDirectory
    @SkipWhenEmpty
    @PathSensitive(PathSensitivity.RELATIVE)
    public abstract DirectoryProperty getShadersDir();

    @OutputDirectory
    public abstract DirectoryProperty getOutputDir();

    @Inject
    protected abstract FileSystemOperations getFileSystemOperations();

    @Inject
    protected abstract ProviderFactory getProviderFactory();

    @TaskAction
    public void run() throws Throwable {
        var shadersDir = getShadersDir().get();
        var generatedDir = getOutputDir().get();

        generate(shadersDir, generatedDir);
    }

    void generate(Directory shadersDir, Directory generatedDir) throws Throwable {
        var preprocessor = new UniformCollector();
        var shaders = new ObjectArrayList<UniformCollector.PipelineResult>();
        var objectMapper = new ObjectMapper();

        var shadersDirPath = shadersDir.getAsFile().toPath();
        try (var distr = Files.newDirectoryStream(shadersDirPath, "*" + META_EXT)) {
            for (Path metaFile : distr) {
                ObjectNode node;
                try (var is = Files.newInputStream(metaFile)) {
                    node = (ObjectNode) objectMapper.readTree(is);
                }
                var fileName = removeExt(metaFile.getFileName().toString());

                var vertexFilename =
                        node.optional("vertex")
                                .map(JsonNode::asText)
                                .orElseGet(() -> fileName + VERT_EXT);
                var fragmentFilename =
                        node.optional("fragment")
                                .map(JsonNode::asText)
                                .orElseGet(() -> fileName + FRAG_EXT);

                var vertRef = shadersDir.file(vertexFilename);
                var fragRef = shadersDir.file(fragmentFilename);

                var providerFactory = getProviderFactory();
                String vertSource = providerFactory.fileContents(vertRef).getAsText().get();
                String fragSource = providerFactory.fileContents(fragRef).getAsText().get();

                shaders.add(preprocessor.processPipeline(fileName, vertSource, fragSource));
            }
        }

        generateUniformLocationsConstants(generatedDir, shaders);
    }

    void generateUniformLocationsConstants(Directory generatedDir, ObjectArrayList<UniformCollector.PipelineResult> shaders) throws Throwable {
        var packageDir = generatedDir
                .dir(BASE_PACKAGE.replace('.', '/'));

        Files.createDirectories(packageDir.getAsFile().toPath());

        var uniformsFile = packageDir.file("Uniforms.java");
        try (var wr = Files.newBufferedWriter(uniformsFile.getAsFile().toPath())) {
            wr.write("package ");
            wr.write(BASE_PACKAGE);
            wr.write(";\n\n");
            wr.write("public final class Uniforms {\n\tprivate Uniforms() {}\n");
            for (var shader : shaders) {
                String className = computeClassName(shader.shaderName());
                wr.write("\n\tpublic static final class " + className + " {\n");
                wr.write("\t\tprivate " + className + "() {}\n\n");
                var uniforms = shader.uniformLocations().object2IntEntrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByValue())
                        .toList();
                for (var entry : uniforms) {
                    wr.write(String.format("\t\tpublic static final byte %s = %d;\n", entry.getKey(), entry.getIntValue()));
                }
                wr.write("\t}\n");
            }
            wr.write("}\n");
        }

        var relocationsFile = packageDir.file("UniformRelocations.java");
        try (var wr = Files.newBufferedWriter(relocationsFile.getAsFile().toPath())) {
            wr.write("package ");
            wr.write(BASE_PACKAGE);
            wr.write(";\n\n");
            wr.write("import core.g2d.Shader;\n\n");
            wr.write("public final class UniformRelocations {\n\tprivate UniformRelocations() {}\n");
            wr.write("\n\tpublic static short[] computeTable(Shader shader) {\n");
            wr.write("\t\tvar uniforms = shader.uniforms();\n");
            wr.write("\t\treturn switch (shader.name()) {\n");
            for (var shader : shaders) {
                wr.write("\t\t\tcase ");
                wr.write(enquote(shader.shaderName()));
                wr.write(" -> {\n");
                wr.write("\t\t\t\tvar relocationTable = new short[" + shader.uniformLocations().size() + "];\n");
                var uniforms = shader.uniformLocations().object2IntEntrySet()
                        .stream()
                        .sorted(Map.Entry.comparingByValue())
                        .toList();
                for (var entry : uniforms) {
                    wr.write(String.format("\t\t\t\trelocationTable[Uniforms.%s.%s] = uniforms.get(%s).position();\n",
                            computeClassName(shader.shaderName()),
                            entry.getKey(),
                            enquote(entry.getKey())));
                }
                wr.write("\t\t\t\tyield relocationTable;\n");
                wr.write("\t\t\t}\n");
            }
            wr.write("\t\t\tdefault -> throw new IllegalArgumentException(");
            wr.write(enquote("Unknown shader with name: '") + " + shader.name() + " + enquote("'"));
            wr.write(");\n");
            wr.write("\t\t};\n");
            wr.write("\t}\n");

            wr.write("}\n");
        }

    }

    private static @NonNull String computeRelocationTableName(UniformCollector.PipelineResult shader) {
        return "relocation_table_" + shader.shaderName();
    }

    private static String enquote(String str) {
        return "\"" + str + "\"";
    }

    private static String computeClassName(String shaderName) {
        StringBuilder camelName = new StringBuilder(shaderName);
        if (Character.isLowerCase(shaderName.charAt(0))) {
            camelName.setCharAt(0, Character.toUpperCase(shaderName.charAt(0)));
        }
        camelName.append(CLASS_NAME_POSTFIX);
        return camelName.toString();
    }

    private static String removeExt(String string) {
        int idx = string.indexOf('.');
        if (idx == -1) return string;
        return string.substring(0, idx);
    }
}
