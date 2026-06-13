package core.g2d;

import com.fasterxml.jackson.databind.node.ObjectNode;
import core.util.Config;
import core.assets.AssetHandler;
import core.assets.AssetReleaser;
import core.assets.AssetResolver;

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
            try (var is = Files.newInputStream(dir.resolve(name + META_EXT))) {
                meta = (ObjectNode) Config.json.readTree(is);
            }

            var atlas = new Atlas();
            var tmpRegions = new HashMap<String, Atlas.Region>();
            int atlasWidth = meta.required("width").asInt();
            int atlasHeight = meta.required("height").asInt();
            meta.required("regions").forEachEntry((regionName, regMeta) -> {
                var regionObject = (ObjectNode) regMeta;
                short x = toShortExact(regionObject.required("x").asInt());
                short y = toShortExact(regionObject.required("y").asInt());
                short width = toShortExact(regionObject.required("width").asInt());
                short height = toShortExact(regionObject.required("height").asInt());
                tmpRegions.put(regionName, new Atlas.Region(atlas, regionName, x, y, width, height,
                        atlasWidth, atlasHeight));
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
