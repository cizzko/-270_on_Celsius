package core.World.WorldGenerator;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import core.Global;
import core.assets.AssetsManager;
import core.content.blocks.Block;
import core.content.serialize.SerializableContent;
import core.g2d.Texture;
import core.util.FutureUtil;

import java.io.IOException;

public enum Biomes implements SerializableContent {
    //чем ближе к 90 тем меньше максимальный угол наклона линии генерации
    mountains(60, 20, 160, 1, getMountains(), "World/Backdrops/backMountains.png"),
    plain(30, 40, 140, 1, getPlain(), "World/Backdrops/backPlain.png"),
    forest(40, 40, 140, 1, getForest(), "World/Backdrops/backForest.png"),
    desert(30, 60, 120, 1, getDesert(), "World/Backdrops/backDesert.png"),
    snowed(30, 60, 120, 1, getSnowed(), "World/Backdrops/backSnowed.png");

    private Texture backdropTex = null;
    private static final Biomes defaultBiome = forest;
    private final int blockGradientChance, upperBorder, bottomBorder, chanceDecrease;
    private final String backdrop;
    private final short[] blocks;

    //int blockGradientChance - насколько острым могут быть углы,
    //int upperBorder - максимальный угол, int bottomBorder - максимальный угол,
    //int chanceDecrease - шанс удаления одного или нескольких верхних блоков 0-100
    Biomes(int blockGradientChance, int upperBorder, int bottomBorder, int chanceDecrease, short[] blocks, String backdrop) {
        this.blockGradientChance = blockGradientChance;
        this.upperBorder = upperBorder;
        this.bottomBorder = bottomBorder;
        this.blocks = blocks;
        this.backdrop = backdrop;
        this.chanceDecrease = chanceDecrease;

        if (backdrop != null) {
            this.backdropTex = FutureUtil.join(Global.assets.load(Texture.class, backdrop, AssetsManager.LoadType.SYNC));
        }
    }

    public int getBlockGradientChance() {
        return blockGradientChance;
    }

    public int getUpperBorder() {
        return upperBorder;
    }

    public int getBottomBorder() {
        return bottomBorder;
    }

    public int getChanceDecrease() {
        return chanceDecrease;
    }

    public short[] getBlocks() {
        return blocks;
    }

    public Texture getBackdrop() {
        return backdropTex;
    }

    public static Biomes getDefault() {
        return defaultBiome;
    }

    private static final Biomes[] values = Biomes.values();

    public static Biomes getRand() {
        return values[(int) (Math.random() * values.length)];
    }

    private static short shortIdByName(String name) {
        Block block = Global.content.blockById(name);
        return (short) Global.content.blocksRegistry.idByType(block);
    }

    private static short[] getMountains() {
        return new short[]{
                shortIdByName("snow"),
                shortIdByName("stone")
        };
    }

    private static short[] getPlain(){
        return new short[]{
                shortIdByName("grass"),
                shortIdByName("dirt"),
                shortIdByName("dirt"),
                shortIdByName("dirtStone"),
                shortIdByName("stone")
        };
    }

    private static short[] getForest() {
        return new short[]{
                shortIdByName("grass"),
                shortIdByName("dirt"),
                shortIdByName("dirtStone"),
                shortIdByName("stone")
        };
    }

    private static short[] getDesert() {
        return new short[]{
                shortIdByName("sand"),
                shortIdByName("sand"),
                shortIdByName("sand"),
                shortIdByName("stone")
        };
    }

    private static short[] getSnowed() {
        return new short[]{
                shortIdByName("snow"),
                shortIdByName("snow"),
                shortIdByName("snow"),
                shortIdByName("snow"),
                shortIdByName("stone")
        };
    }

    @Override
    public void serialize(JsonGenerator gen, SerializerProvider provider) throws IOException {

    }
}
