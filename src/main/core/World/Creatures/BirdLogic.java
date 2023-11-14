package core.World.Creatures;

import java.util.HashMap;

public class BirdLogic {
    //soar time
    private static final HashMap<DynamicWorldObjects, Integer> birdFlying = new HashMap<>();
    private static int maxSoarTime = 2000;

    //TODO: rewrite
    public static void update(DynamicWorldObjects object) {
        if (object.path.contains("bird")) {
            if (birdFlying.get(object) != null) {
                int flyingTime = birdFlying.get(object);
                birdFlying.put(object, flyingTime - 1);

                object.currentFrame = 1;
                object.animSpeed = 0.0f;

                if (flyingTime <= 0) {
                    birdFlying.remove(object);
                }
            } else {
                object.motionVector.x = 0.3f;
                object.animSpeed = 0.1f;

                 if (Math.random() * 1000 < 1) {
                    birdFlying.put(object, (int) (Math.random() * maxSoarTime));
                }
            }
        }
    }
}