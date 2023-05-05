package core.World.creatures;

import static core.World.WorldGenerator.DynamicObjects;
import static core.World.creatures.CreaturesGenerate.getEntityName;

public class BirdLogic {
    //длительность парения
    private static final int[] birdFlying = new int[DynamicObjects.length];
    private static long deltaTime = System.currentTimeMillis();
    private static long deltaTime1 = System.currentTimeMillis();

    public static void update() {
        if (System.currentTimeMillis() - deltaTime > 10) {
            for (int x = 0; x < DynamicObjects.length; x++) {
                if (DynamicObjects[x] != null && !DynamicObjects[x].isPlayer && getEntityName(DynamicObjects[x].path).equals("bird")) {
                    DynamicObjects[x].x++;

                    if (birdFlying[x] == 0 && Math.random() * 500 < 1) {
                        birdFlying[x] = (int) (Math.random() * 5);
                    }
                    if (System.currentTimeMillis() - deltaTime1 > 1000 && birdFlying[x] > 0) {
                        birdFlying[x]--;
                        deltaTime1 = System.currentTimeMillis();
                    }
                    if (birdFlying[x] != 0) {
                        DynamicObjects[x].currentFrame = 1;
                        DynamicObjects[x].animSpeed = 0.0f;
                    }
                    else if (birdFlying[x] == 0) {
                        DynamicObjects[x].animSpeed = 0.1f;
                    }
                }
            }
            deltaTime = System.currentTimeMillis();
        }
    }
 }