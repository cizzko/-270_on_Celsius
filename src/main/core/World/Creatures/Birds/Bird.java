package core.World.Creatures.Birds;

import core.World.Creatures.DynamicWorldObjects;

public class Bird {
    public int flyTime, soaringTime;
    public float hunger, sleep;
    public Bird() {
        //время от взлета
        this.flyTime = 0;
        //время парения
        this.soaringTime = 0;
        //сытость
        this.hunger = 100;
        //уровень сна, при 20 пытается найти место для сна, при 0 засыпает где угодно
        this.sleep = 100;
    }
}
