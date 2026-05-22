package core.g2d;

import com.fasterxml.jackson.databind.node.ObjectNode;
import core.EventHandling.Config;
import core.assets.AssetHandler;
import core.assets.AssetReleaser;
import core.assets.AssetResolver;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Future;

import static core.g2d.Atlas.*;
import static core.math.MathUtil.toShortExact;

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
            ObjectNode meta;
            try (var reader = Files.newBufferedReader(dir.resolve(name + META_EXT), StandardCharsets.UTF_8)) {
                meta = (ObjectNode) Config.json.readTree(reader);
            }

            var atlas = new Atlas();
            var tmpRegions = new HashMap<String, Atlas.Region>();
            meta.required("regions").forEachEntry((regionName, regMeta) -> {
                var regionObject = (ObjectNode) regMeta;
                int x = regionObject.required("x").asInt();
                int y = regionObject.required("y").asInt();
                short width = toShortExact(regionObject.required("width").asInt());
                short height = toShortExact(regionObject.required("height").asInt());
                tmpRegions.put(regionName, new Atlas.Region(atlas, regionName, x, y, width, height));
            });
            String errorRegionName = meta.required("error").asText();

            Atlas.Region errorRegion = tmpRegions.get(errorRegionName);
            if (errorRegion == null) {
                throw new IllegalArgumentException("No error region");
            }
            atlas.regions = Map.copyOf(tmpRegions);
            atlas.errorRegion = errorRegion;
            return atlas;
        });
    }

    @Override
    public Atlas loadSync(AssetResolver res, String name, Void params, State state) {
        var atlas = res.join(state.meta);
        atlas.texture = res.join(state.texture);
        res.checkIfFailed();

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
