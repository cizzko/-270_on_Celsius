package core.World.Creatures;

import java.util.ArrayList;
import java.util.Collections;
import static core.World.WorldGenerator.DynamicObjects;

public class BirdLogic {
    //длительность парения
    private static final ArrayList<Integer> birdFlying = new ArrayList<>(Collections.nCopies(10, 0));
    private static long deltaTime = System.currentTimeMillis();
    private static long deltaTime1 = System.currentTimeMillis();

    public static void update() {
        if (System.currentTimeMillis() - deltaTime > 10) {
            for (int x = 0; x < DynamicObjects.size(); x++) {
                if (DynamicObjects.get(x) != null && birdFlying.get(x) != null && DynamicObjects.get(x).path.contains("bird")) {
                    if (birdFlying.get(x) == 0 && Math.random() * 500 < 1) {
                        birdFlying.set(x, (int) (Math.random() * 5));
                    }
                    if (System.currentTimeMillis() - deltaTime1 > 1000 && birdFlying.get(x) > 0) {
                        birdFlying.set(x, birdFlying.get(x) - 1);
                        deltaTime1 = System.currentTimeMillis();
                    }
                    if (birdFlying.get(x) != 0) {
                        DynamicObjects.get(x).currentFrame = 1;
                        DynamicObjects.get(x).animSpeed = 0.0f;
                    }
                    else if (birdFlying.get(x) == 0) {
                        DynamicObjects.get(x).motionVector.x = 0.3f;
                        DynamicObjects.get(x).animSpeed = 0.1f;
                    }
                }
            }
            deltaTime = System.currentTimeMillis();
        }
    }
}