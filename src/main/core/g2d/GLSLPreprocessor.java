package core.g2d;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectList;
import it.unimi.dsi.fastutil.objects.ObjectLists;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class GLSLPreprocessor {
    private static final char LINE_SEPARATOR = '\n';

    private static final Object2IntOpenHashMap<String> FIXED_LOCATIONS = new Object2IntOpenHashMap<>();

    static {
        FIXED_LOCATIONS.put("u_texture", 0);
        FIXED_LOCATIONS.put("u_logical_ratio", 1);
        FIXED_LOCATIONS.put("u_camera_pos", 2);
    }

    static class VariableInfo {
        String name;
        int location;
        String type;
        boolean hasLayout;
        String keyword;

        VariableInfo(String name, int location, String type, boolean hasLayout, String keyword) {
            this.name = name;
            this.location = location;
            this.type = type;
            this.hasLayout = hasLayout;
            this.keyword = keyword;
        }
    }

    private static final Pattern GLSL_PATTERN = Pattern.compile(
            "(?:layout\\s*\\(\\s*location\\s*=\\s*(\\d+)\\s*\\)\\s*|\\b(flat)\\s+)?\\b(uniform|in|out)\\s+((?:(?:highp|mediump|lowp)\\s+)?\\w+)\\s+(\\w+)\\s*;",
            Pattern.CASE_INSENSITIVE
    );


    public record PipelineResult(
            String shaderName,
            String modifiedVertexSource,
            String modifiedFragmentSource
    ) {
    }

    public static PipelineResult processPipeline(String shaderName, String vertSource, String fragSource,
                                                 List<String> injectedText,
                                                 boolean explicitUniformLocations,
                                                 boolean explicitOutLocations,
                                                 boolean bindlessSamplers) {
        String modifiedVert;
        String modifiedFrag;

        var errors = new ObjectArrayList<String>();
        if (explicitUniformLocations || explicitOutLocations) {
            var vertVariables = parseVariables(vertSource);
            var fragVariables = parseVariables(fragSource);

            var vertIns = filterByKeyword(vertVariables, "in");
            assignPoolLocations(vertIns, errors, "in (vertex)");

            var vertOuts = filterByKeyword(vertVariables, "out");
            var fragIns = filterByKeyword(fragVariables, "in");

            var interstageVariables = new LinkedHashMap<String, VariableInfo>();
            mergeInterstage(interstageVariables, vertOuts, errors, shaderName + " (vert out)");
            mergeInterstage(interstageVariables, fragIns, errors, shaderName + " (frag in)");

            assignPoolLocations(interstageVariables.values(), errors, "interstage io");

            var unifiedUniforms = new LinkedHashMap<String, VariableInfo>();
            mergeUniforms(unifiedUniforms, vertVariables, errors, shaderName);
            mergeUniforms(unifiedUniforms, fragVariables, errors, shaderName);

            assignPoolLocations(unifiedUniforms.values(), errors, "uniforms");

            if (!errors.isEmpty()) {
                throw new IllegalArgumentException("Ошибки при валидации GLSL:\n" + String.join("\n", errors));
            }

            modifiedVert = rebuildShaderText(vertSource, unifiedUniforms, interstageVariables, vertIns,
                    explicitUniformLocations, explicitOutLocations, bindlessSamplers);
            modifiedFrag = rebuildShaderText(fragSource, unifiedUniforms, interstageVariables, ObjectLists.emptyList(),
                    explicitUniformLocations, explicitOutLocations, bindlessSamplers);
        } else {
            modifiedVert = vertSource;
            modifiedFrag = fragSource;
        }

        modifiedVert = injectHeaderText(modifiedVert, injectedText);
        modifiedFrag = injectHeaderText(modifiedFrag, injectedText);

        return new PipelineResult(shaderName, modifiedVert, modifiedFrag);
    }

    private static ObjectArrayList<VariableInfo> parseVariables(String source) {
        var list = new ObjectArrayList<VariableInfo>();
        Matcher matcher = GLSL_PATTERN.matcher(source);
        while (matcher.find()) {
            String locStr = matcher.group(1);

            String keyword = matcher.group(3).toLowerCase(Locale.ROOT);
            String type = matcher.group(4);
            String name = matcher.group(5);

            int location = (locStr != null) ? Integer.parseInt(locStr) : -1;
            boolean hasLayout = locStr != null;

            list.add(new VariableInfo(name, location, type, hasLayout, keyword));
        }
        return list;
    }

    private static ObjectArrayList<VariableInfo> filterByKeyword(ObjectArrayList<VariableInfo> src, String keyword) {
        var filtered = new ObjectArrayList<VariableInfo>();
        for (var var : src) {
            if (keyword.equals(var.keyword)) filtered.add(var);
        }
        return filtered;
    }

    private static void mergeInterstage(Map<String, VariableInfo> unified,
                                        ObjectArrayList<VariableInfo> vars,
                                        ObjectArrayList<String> errors,
                                        String stageLabel) {
        for (var var : vars) {
            if (unified.containsKey(var.name)) {
                VariableInfo existing = unified.get(var.name);
                if (!existing.type.equals(var.type)) {
                    errors.add(String.format("Конфликт типов для переменной '%s' между стадиями: '%s' и '%s' (%s)",
                            var.name, existing.type, var.type, stageLabel));
                }
                if (var.hasLayout && existing.hasLayout && var.location != existing.location) {
                    errors.add(String.format("Конфликт локаций для переменной '%s': %d и %d (%s)",
                            var.name, existing.location, var.location, stageLabel));
                }
                if (var.hasLayout && !existing.hasLayout) {
                    existing.location = var.location;
                    existing.hasLayout = true;
                }
            } else {
                unified.put(var.name, new VariableInfo(var.name, var.location, var.type, var.hasLayout, var.keyword));
            }
        }
    }

    private static void mergeUniforms(Map<String, VariableInfo> unified,
                                      ObjectArrayList<VariableInfo> shaderVars,
                                      ObjectArrayList<String> errors,
                                      String fileName) {
        for (var var : shaderVars) {
            if (!"uniform".equals(var.keyword)) continue;

            if (unified.containsKey(var.name)) {
                VariableInfo existing = unified.get(var.name);
                if (!existing.type.equals(var.type)) {
                    errors.add(String.format("Конфликт типов для uniform '%s': '%s' в одном месте и '%s' в файле %s",
                            var.name, existing.type, var.type, fileName));
                }
                if (var.hasLayout && existing.hasLayout && var.location != existing.location) {
                    errors.add(String.format("Конфликт явных локаций для uniform '%s': %d и %d (файл %s)",
                            var.name, existing.location, var.location, fileName));
                }
                if (var.hasLayout && !existing.hasLayout) {
                    existing.location = var.location;
                    existing.hasLayout = true;
                }
            } else {
                unified.put(var.name, new VariableInfo(var.name, var.location, var.type, var.hasLayout, "uniform"));
            }
        }
    }

    private static void assignPoolLocations(
            Collection<VariableInfo> variables,
            ObjectArrayList<String> errors,
            String poolLabel) {

        var withLocation = new ObjectArrayList<VariableInfo>();
        var withoutLocation = new ObjectArrayList<VariableInfo>();
        var usedLocations = new IntOpenHashSet();

        for (VariableInfo var : variables) {
            if ("uniform".equals(var.keyword) && FIXED_LOCATIONS.containsKey(var.name)) {
                int fixedLoc = FIXED_LOCATIONS.getInt(var.name);
                if (var.hasLayout && var.location != fixedLoc) {
                    errors.add(String.format("Конфликт: uniform '%s' имеет глобальный фиксированный ID %d, но в коде задан location = %d",
                            var.name, fixedLoc, var.location));
                }
                var.location = fixedLoc;
                var.hasLayout = true;
                withLocation.add(var);
                usedLocations.add(fixedLoc);
            } else if (var.hasLayout) {
                withLocation.add(var);
                usedLocations.add(var.location);
            } else {
                withoutLocation.add(var);
            }
        }

        if ("uniform".equals(poolLabel)) {
            for (int fixedLoc : FIXED_LOCATIONS.values()) {
                usedLocations.add(fixedLoc);
            }
        }

        var seenLocations = new IntOpenHashSet();
        for (VariableInfo var : withLocation) {
            if (!seenLocations.add(var.location)) {
                errors.add(String.format("Дубликат локации %d в пуле %s для переменной '%s'", var.location, poolLabel, var.name));
            }
        }

        if (!errors.isEmpty()) {
            return;
        }

        int nextLocation = 0;
        for (VariableInfo var : withoutLocation) {
            while (usedLocations.contains(nextLocation)) {
                nextLocation++;
            }
            usedLocations.add(nextLocation);
            var.location = nextLocation;
            nextLocation++;
        }
    }

    private static String rebuildShaderText(
            String source,
            LinkedHashMap<String, VariableInfo> globalUniforms,
            LinkedHashMap<String, VariableInfo> interstageVars,
            ObjectList<VariableInfo> vertInp,
            boolean explicitUniformLocations,
            boolean explicitOutLocations,
            boolean bindlessSamplers)
    {
        var matcher = GLSL_PATTERN.matcher(source);
        var sb = new StringBuilder();
        var vertInpMap = new HashMap<String, VariableInfo>();
        for (var var : vertInp) {
            vertInpMap.put(var.name, var);
        }

        while (matcher.find()) {
            String flatModifier = matcher.group(2);
            String keyword = matcher.group(3).toLowerCase(Locale.ROOT);
            String type = matcher.group(4);
            String name = matcher.group(5);

            int finalLocation = -1;
            VariableInfo var;
            boolean isSamplerOrImage = false;

            switch (keyword) {
                case "uniform" -> {
                    if ((var = globalUniforms.get(name)) != null) {
                        if (explicitUniformLocations) {
                            finalLocation = var.location;
                        }
                        String lowerType = type.toLowerCase(Locale.ROOT);
                        isSamplerOrImage = lowerType.contains("sampler") || lowerType.contains("image");
                    }
                }
                case "in" -> {
                    var = interstageVars.get(name);
                    if (var != null) {
                        finalLocation = var.location;
                    } else if ((var = vertInpMap.get(name)) != null) {
                        finalLocation = var.location;
                    }
                }
                case "out" -> {
                    if (explicitOutLocations && (var = interstageVars.get(name)) != null) {
                        finalLocation = var.location;
                    }
                }
            }

            boolean needBindless = "uniform".equals(keyword) && bindlessSamplers && isSamplerOrImage;

            if (finalLocation != -1 || needBindless) {
                var layoutParts = new ArrayList<String>();

                if (finalLocation != -1) {
                    layoutParts.add("location = " + finalLocation);
                }
                if (needBindless) {
                    layoutParts.add("bindless_sampler");
                }

                String layoutBody = String.join(", ", layoutParts);

                String fullKeyword = (flatModifier != null) ? flatModifier + " " + keyword : keyword;

                String replacement = String.format("layout(%s) %s %s %s;", layoutBody, fullKeyword, type, name);
                matcher.appendReplacement(sb, Matcher.quoteReplacement(replacement));
            } else {
                matcher.appendReplacement(sb, Matcher.quoteReplacement(matcher.group(0)));
            }
        }
        matcher.appendTail(sb);
        return sb.toString();
    }


    private static String injectHeaderText(String content, List<String> injectedText) {
        if (injectedText.isEmpty()) return content;
        var injected = new StringBuilder();
        for (String line : injectedText) {
            injected.append(line).append(LINE_SEPARATOR);
        }
        return injected.append("#line 1").append(LINE_SEPARATOR).append(content).toString();
    }
}
