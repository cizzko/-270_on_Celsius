package core.World;

public class PerlinNoiseGenerator {
    public static boolean[][] createBoolNoise(int sizeX, int sizeY, float chance) {
        boolean[][] noise = new boolean[sizeX][sizeY];
        boolean seed = false;

        for (int x = 0; x < sizeX; x++) {
            for (int y = 0; y < sizeY; y++) {
                if (!seed && Math.random() * sizeX < 1) {
                    noise[x][y] = true;
                }
                noise[x][y] = around(x, y, noise) && Math.random() * chance < 1;
            }
        }
        return noise;
    }

    private static boolean around(int x, int y, boolean[][] noise) {
        return noise[Math.max(0, x - 1)][y] ||
                noise[Math.min(noise.length - 1, x + 1)][y] ||
                noise[x][Math.max(0, y - 1)];
    }
}
