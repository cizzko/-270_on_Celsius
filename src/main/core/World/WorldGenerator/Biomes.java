package core.World.WorldGenerator;

import core.World.StaticWorldObjects.StaticWorldObjects;

public enum Biomes {
    //чем ближе к 90 тем меньше максимальный угол наклона линии генерации
    mountains(60, 20, 160, StaticWorldObjects.createStatic("Blocks/grass"), StaticWorldObjects.createStatic("Blocks/dirt")),
    plain(40, 40, 140, StaticWorldObjects.createStatic("Blocks/grass"), StaticWorldObjects.createStatic("Blocks/dirt")),
    forest(40, 40, 140, StaticWorldObjects.createStatic("Blocks/grass"), StaticWorldObjects.createStatic("Blocks/dirt")),
    desert(30, 60, 120, StaticWorldObjects.createStatic("Blocks/grass"), StaticWorldObjects.createStatic("Blocks/dirt"));
    //..и надо еще что то с лесами придумать, там же деревья, может сделать как с деревьями (см строку 17 - 18) массив "важности" возможных генерируемых объектов на местности?
    //чем ближе к нулю, тем чаще генерируется объект, условно
    //может это будет и не массив, а просто набор объект - шанс

    private static final Biomes defaultBiome = forest;
    private final int blockGradientChance, upperBorder, bottomBorder;
    private final short mainBlock, secondBlock;

    //main n second надо в массив, но надо придумать как с этим потом работать
    //или просто сделать массив по "важности" блоков, самые важные [0] наверху, и по убыванию
    Biomes(int blockGradientChance, int upperBorder, int bottomBorder, short mainBlock, short secondBlock) {
        this.blockGradientChance = blockGradientChance;
        this.upperBorder = upperBorder;
        this.bottomBorder = bottomBorder;
        this.mainBlock = mainBlock;
        this.secondBlock = secondBlock;
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

    public static Biomes getDefault() {
        return defaultBiome;
    }

    public static Biomes getRand() {
        return Biomes.values()[(int) (Math.random() * Biomes.values().length)];
    }
}
