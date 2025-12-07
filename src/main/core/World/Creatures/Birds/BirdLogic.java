package core.World.Creatures.Birds;

import core.World.Creatures.DynamicWorldObjects;

import java.util.ArrayList;
import java.util.HashMap;

public class BirdLogic {
    public static ArrayList<Bird> birds = new ArrayList<>();

    public static void create(int x) {
        birds.add(new Bird());
        DynamicWorldObjects.createDynamic("bird", x);
    }

    public static void update() {
        for (Bird bird : birds) {
            bird.sleep -= 0.001f;
            if (bird.sleep <= 20) {
                //тут ищет место для сна
            } else if (bird.sleep <= 0) {
                //тут пытается сесть хоть куда нибудь для сна
            }
            bird.hunger -= 0.003f;
            if (bird.hunger <= 20) {
                //тут ищет еду (семечки всякие, прочее)
            } else if (bird.hunger <= 0) {
                //тут начинает терять хп
            }


        }
    }
}
