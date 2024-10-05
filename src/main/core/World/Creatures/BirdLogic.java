package core.World.Creatures;

import java.util.HashMap;

public class BirdLogic {
    // soar time
    private static final HashMap<DynamicWorldObjects, Integer> birdFlying = new HashMap<>();
    private static final int maxSoarTime = 2000;

    // todo переписать
    public static void update(DynamicWorldObjects object) {
        if (object.getTexture().name().contains("bird")) {
            if (birdFlying.get(object) != null) {
                int flyingTime = birdFlying.get(object);
                birdFlying.put(object, flyingTime - 1);

                object.setCurrentFrame((short) 1);
                object.setAnimationSpeed(0);

                if (flyingTime <= 0) {
                    birdFlying.remove(object);
                }
            } else {
                object.setMotionVectorX(0.3f);
                object.setAnimationSpeed(100);

                 if (Math.random() * 1000 < 1) {
                    birdFlying.put(object, (int) (Math.random() * maxSoarTime));
                }
            }
        }
    }
}
