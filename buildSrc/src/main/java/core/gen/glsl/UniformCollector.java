package core.gen.glsl;

import it.unimi.dsi.fastutil.ints.IntOpenHashSet;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class UniformCollector {
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
            "(?:layout\\s*\\(\\s*location\\s*=\\s*(\\d+)\\s*\\)\\s*)?\\b(uniform)\\s+(\\w+)\\s+(\\w+)\\s*;",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE
    );

    public record PipelineResult(
            String shaderName,
            Object2IntOpenHashMap<String> uniformLocations
    ) {
    }

    public PipelineResult processPipeline(String shaderName, String vertSource, String fragSource) throws IOException {
        var errors = new ObjectArrayList<String>();

        var vertVariables = parseVariables(vertSource);
        var fragVariables = parseVariables(fragSource);

        Map<String, VariableInfo> unifiedUniforms = new LinkedHashMap<>();
        mergeUniforms(unifiedUniforms, vertVariables, errors, shaderName);
        mergeUniforms(unifiedUniforms, fragVariables, errors, shaderName);

        var uniformList = new ObjectArrayList<>(unifiedUniforms.values());
        var uniformMap = assignPoolLocations(uniformList, errors, "uniform");

        if (!errors.isEmpty()) {
            throw new IllegalArgumentException("Ошибки при валидации GLSL:\n" + String.join("\n", errors));
        }

        return new PipelineResult(shaderName, uniformMap);
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

    private ObjectArrayList<VariableInfo> parseVariables(String source) {
        var list = new ObjectArrayList<VariableInfo>();
        Matcher matcher = GLSL_PATTERN.matcher(source);
        while (matcher.find()) {
            String locStr = matcher.group(1);
            String keyword = matcher.group(2).toLowerCase();
            String type = matcher.group(3);
            String name = matcher.group(4);

            int location = (locStr != null) ? Integer.parseInt(locStr) : -1;
            boolean hasLayout = locStr != null;

            list.add(new VariableInfo(name, location, type, hasLayout, keyword));
        }
        return list;
    }

    private ObjectArrayList<VariableInfo> filterByKeyword(ObjectArrayList<VariableInfo> src, String keyword) {
        var filtered = new ObjectArrayList<VariableInfo>();
        for (var var : src) {
            if (keyword.equals(var.keyword)) filtered.add(var);
        }
        return filtered;
    }

    private void mergeUniforms(Map<String, VariableInfo> unified,
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

    private static Object2IntOpenHashMap<String> assignPoolLocations(
            ObjectArrayList<VariableInfo> variables,
            ObjectArrayList<String> errors,
            String poolLabel
    ) {
        var withLocation = new ObjectArrayList<VariableInfo>();
        var withoutLocation = new ObjectArrayList<VariableInfo>();
        var usedLocations = new IntOpenHashSet();
        var locationsMap = new Object2IntOpenHashMap<String>();

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

        if (!errors.isEmpty()) return locationsMap;

        for (VariableInfo var : withLocation) {
            locationsMap.put(var.name, var.location);
        }

        int nextLocation = 0;
        for (VariableInfo var : withoutLocation) {
            while (usedLocations.contains(nextLocation)) {
                nextLocation++;
            }
            locationsMap.put(var.name, nextLocation);
            usedLocations.add(nextLocation);
            var.location = nextLocation;
            nextLocation++;
        }

        return locationsMap;
    }
}
