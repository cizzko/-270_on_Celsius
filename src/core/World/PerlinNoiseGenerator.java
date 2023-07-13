package core.World;

import java.util.Random;

public class PerlinNoiseGenerator {
    public static boolean[][] noise;
    private static int smoothness, height = 0, width = 0;
    private static double amplitude, density, octaves = 0, persistence = 0;

    public PerlinNoiseGenerator(int width, int height, double amplitude, double persistence, double octaves, double density, int smoothness) {
        PerlinNoiseGenerator.density = density;
        PerlinNoiseGenerator.amplitude = amplitude;
        PerlinNoiseGenerator.persistence = persistence;
        PerlinNoiseGenerator.octaves = octaves;
        PerlinNoiseGenerator.smoothness = smoothness;
        PerlinNoiseGenerator.width = width;
        PerlinNoiseGenerator.height = height;
        noise = new boolean[width][height];
    }

    public static void main(int width, int height, double amplitude, double persistence, double octaves, double density, int smoothness) {
        PerlinNoiseGenerator noiseGenerator = new PerlinNoiseGenerator(width, height, amplitude, persistence, octaves, density, smoothness);
        noiseGenerator.generateNoise();
    }

    public void generateNoise() {
        double[][] baseNoise = generateBaseNoise();
        double[][] smoothedNoise;

        if (smoothness > 0) {
            smoothedNoise = smoothNoise(baseNoise);
        } else {
            smoothedNoise = baseNoise;
        }

        double[][] perlinNoise = new double[width][height];

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

    private double[][] generateBaseNoise() {
        double[][] baseNoise = new double[width][height];
        Random random = new Random();

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                baseNoise[i][j] = random.nextDouble();
            }
        }

        return baseNoise;
    }

    private double[][] smoothNoise(double[][] baseNoise) {
        int samplePeriod = smoothness * 2;
        double[][] smoothNoise = new double[width][height];

        for (int i = 0; i < width; i++) {
            int i0 = (i / samplePeriod) * samplePeriod;
            int i1 = (i0 + samplePeriod) % width;
            double horizontalBlend = (i - i0) / (double) samplePeriod;

            for (int j = 0; j < height; j++) {
                int j0 = (j / samplePeriod) * samplePeriod;
                int j1 = (j0 + samplePeriod) % height;
                double verticalBlend = (j - j0) / (double) samplePeriod;

                double top = interpolate(baseNoise[i0][j0], baseNoise[i1][j0], horizontalBlend);
                double bottom = interpolate(baseNoise[i0][j1], baseNoise[i1][j1], horizontalBlend);
                smoothNoise[i][j] = interpolate(top, bottom, verticalBlend);
            }
        }

        return smoothNoise;
    }

    private double interpolate(double x0, double x1, double alpha) {
        return x0 * (1 - alpha) + alpha * x1;
    }

    private double[][] generatePerlinNoise(double[][] baseNoise, int octave) {
        smoothness = (int) Math.pow(2, octave);
        double[][] perlinNoise = new double[width][height];
        double[][] octaveBaseNoise = smoothNoise(baseNoise);

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                perlinNoise[i][j] += octaveBaseNoise[i][j];
            }
        }

        return perlinNoise;
    }

    private void addNoise(double[][] perlinNoise, double amplitude) {
        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                perlinNoise[i][j] += amplitude;
            }
        }
    }

    private void normalizeNoise(double[][] perlinNoise) {
        double maxNoise = Double.MIN_VALUE;
        double minNoise = Double.MAX_VALUE;

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

        double range = maxNoise - minNoise;

        for (int i = 0; i < width; i++) {
            for (int j = 0; j < height; j++) {
                perlinNoise[i][j] = (perlinNoise[i][j] - minNoise) / range;
            }
        }
    }
}
