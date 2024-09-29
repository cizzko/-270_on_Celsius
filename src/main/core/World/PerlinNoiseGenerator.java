package core.World;

import java.util.Random;

public class PerlinNoiseGenerator {
    public static boolean[][] noise;
    private static int smoothness, height = 0, width = 0;
    private static float amplitude, density, octaves = 0, persistence = 0;

    public PerlinNoiseGenerator(int width, int height, float amplitude, float persistence, float octaves, float density, int smoothness) {
        PerlinNoiseGenerator.density = density;
        PerlinNoiseGenerator.amplitude = amplitude;
        PerlinNoiseGenerator.persistence = persistence;
        PerlinNoiseGenerator.octaves = octaves;
        PerlinNoiseGenerator.smoothness = smoothness;
        PerlinNoiseGenerator.width = width;
        PerlinNoiseGenerator.height = height;
        noise = new boolean[width][height];
    }

    public static void main(int width, int height, float amplitude, float persistence, float octaves, float density, int smoothness) {
        PerlinNoiseGenerator noiseGenerator = new PerlinNoiseGenerator(width, height, amplitude, persistence, octaves, density, smoothness);
        noiseGenerator.generateNoise();
    }

    public void generateNoise() {
        float[][] smoothedNoise = generateBaseNoise();

        if (smoothness > 0) {
            smoothedNoise = smoothNoise(smoothedNoise);
        }

        float[][] perlinNoise = new float[width][height];

        for (int octave = 0; octave < octaves; octave++) {
            perlinNoise = generatePerlinNoise(smoothedNoise, octave);
            addNoise(perlinNoise, amplitude);
            amplitude *= persistence;
        }

        normalizeNoise(perlinNoise);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                noise[i][j] = perlinNoise[i][j] >= density;
            }
        }

    }

    private float[][] generateBaseNoise() {
        float[][] baseNoise = new float[width][height];
        Random random = new Random();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                baseNoise[i][j] = (float) random.nextDouble();
            }
        }

        return baseNoise;
    }

    private float[][] smoothNoise(float[][] baseNoise) {
        int samplePeriod = smoothness * 2;
        float[][] smoothNoise = new float[width][height];

        for (int i = 0; i < width; i++) {
            int i0 = (i / samplePeriod) * samplePeriod;
            int i1 = (i0 + samplePeriod) % width;
            float horizontalBlend = (i - i0) / (float) samplePeriod;

            for (int j = 0; j < height; j++) {
                int j0 = (j / samplePeriod) * samplePeriod;
                int j1 = (j0 + samplePeriod) % height;
                float verticalBlend = (j - j0) / (float) samplePeriod;

                float top = interpolate(baseNoise[i0][j0], baseNoise[i1][j0], horizontalBlend);
                float bottom = interpolate(baseNoise[i0][j1], baseNoise[i1][j1], horizontalBlend);
                smoothNoise[i][j] = interpolate(top, bottom, verticalBlend);
            }
        }

        return smoothNoise;
    }

    private float interpolate(float x0, float x1, float alpha) {
        return x0 * (1 - alpha) + alpha * x1;
    }

    private float[][] generatePerlinNoise(float[][] baseNoise, int octave) {
        smoothness = (int) Math.pow(2, octave);
        float[][] perlinNoise = new float[width][height];
        float[][] octaveBaseNoise = smoothNoise(baseNoise);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                perlinNoise[i][j] += octaveBaseNoise[i][j];
            }
        }

        return perlinNoise;
    }

    private void addNoise(float[][] perlinNoise, float amplitude) {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                perlinNoise[i][j] += amplitude;
            }
        }
    }

    private void normalizeNoise(float[][] perlinNoise) {
        float maxNoise = Float.MIN_VALUE;
        float minNoise = Float.MAX_VALUE;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                if (perlinNoise[i][j] > maxNoise) {
                    maxNoise = perlinNoise[i][j];
                }
                if (perlinNoise[i][j] < minNoise) {
                    minNoise = perlinNoise[i][j];
                }
            }
        }

        float range = maxNoise - minNoise;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                perlinNoise[i][j] = (perlinNoise[i][j] - minNoise) / range;
            }
        }
    }

    //frequency - frequency of seeds, how many there will be
    //expansion - how quickly the noise level will decrease
    //minForce & maxForce - minimum and maximum possible seed value
    //minSeedSize & maxSeedSize - minimum and maximum possible seed value
    public static short[][] generateNoise(int sizeX, int sizeY, int frequency, int minSeedSize, int maxSeedSize, int minForce, int maxForce, boolean directed) {
        short[][] noise = new short[sizeX][sizeY];

        //first step - place seeds
        for (int x = 0; x < noise.length; x++) {
            for (int y = 0; y < noise[0].length; y++) {
                if ((Math.random() * (frequency + (directed ? sizeY - y : 0))) < 1) {
                    short seedSize = (short) Math.max(Math.random() * maxSeedSize, Math.max(1, minSeedSize));
                    short seedNumber = (short) Math.max(Math.random() * maxForce, minForce);

                    for (int xSeed = seedSize / -2; xSeed < seedSize / 2; xSeed++) {
                        for (int ySeed = seedSize / -2; ySeed < seedSize / 2; ySeed++) {
                            if (xSeed + x > 0 && ySeed + y > 0 && xSeed + x < noise.length && ySeed + y < noise[0].length) {
                                noise[x + xSeed][y + ySeed] = seedNumber;
                            }
                        }
                    }
                }
            }
        }

        return noise;
    }

    public static short[][] smoothNoise(short[][] noise, int iterations) {
        for (int i = 0; i < iterations; i++) {
            for (int x = 1; x < noise.length - 1; x++) {
                for (int y = 1; y < noise[0].length - 1; y++) {
                    noise[x][y] = (short) ((noise[x][y - 1] + noise[x][y + 1] + noise[x - 1][y] + noise[x + 1][y]) / 4);
                }
            }
        }
        return noise;
    }
}
