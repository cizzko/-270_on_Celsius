package core.g2d;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import core.assets.AssetHandler;
import core.assets.AssetReleaser;
import core.assets.AssetResolver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import static core.g2d.Atlas.ATLAS_EXT;
import static core.g2d.Atlas.META_EXT;

public final class AtlasHandler extends AssetHandler<Atlas, Void, AtlasHandler.State> {
    public AtlasHandler() {
        super(Atlas.class, "");
    }

    @Override
    public void release(AssetReleaser rel, Atlas asset) {
        rel.release(asset.texture);
    }

    @Override
    public void loadAsync(AssetResolver res, String name, Void params, State state) {
        state.texture = res.load(Texture.class, name + ATLAS_EXT);
        state.meta = res.fork(() -> {
            var atlas = new Atlas();

            JsonObject meta;
            try (var reader = Files.newBufferedReader(dir.resolve(name + META_EXT), StandardCharsets.UTF_8)) {
                meta = JsonParser.parseReader(reader).getAsJsonObject();
            }

            HashMap<String, Atlas.Region> tmpRegions = new HashMap<>();
            meta.getAsJsonObject("regions").asMap().forEach((regionName, regMeta) -> {
                JsonObject regionObject = regMeta.getAsJsonObject();
                int x = regionObject.get("x").getAsInt();
                int y = regionObject.get("y").getAsInt();
                int width = regionObject.get("width").getAsInt();
                int height = regionObject.get("height").getAsInt();
                tmpRegions.put(regionName, new Atlas.Region(atlas, regionName, x, y, width, height));
            });
            String errorRegionName = meta.get("error").getAsString();

            Map<String, Atlas.Region> regions = Map.copyOf(tmpRegions);
            Atlas.Region errorRegion = regions.get(errorRegionName);
            if (errorRegion == null) {
                throw new IllegalArgumentException("No error region");
            }
            atlas.regions = regions;
            atlas.errorRegion = errorRegion;
            return atlas;
        });
    }

    @Override
    public Atlas loadSync(String name, Void params, State state) {
        var atlas = state.meta.resultNow();
        atlas.texture = state.texture.resultNow();
        for (Atlas.Region region : atlas.regions.values()) {
            region.computeTextureCoordinates();
        }
        return atlas;
    }

    @Override
    protected Void createParams() {
        return null;
    }

    @Override
    protected State createState() {
        return new State();
    }

    public static final class State {
        private Future<Texture> texture;
        private Future<Atlas> meta;
    }
}
