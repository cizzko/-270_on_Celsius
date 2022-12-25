package core.World;

import core.World.Textures.TextureDrawing;

public class MainMenu {
    public static void Create(){
        TextureDrawing.draw(".\\src\\assets\\GUI\\buttonStart1.png", 10, 200, null, null);
        TextureDrawing.draw(".\\src\\assets\\GUI\\buttonStart.png", 10, 400, null, null);
    }
}
