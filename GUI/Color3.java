package GUI;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
public class Color3 {
    public static @NotNull List<Float> main(@NotNull String hex){
        List<Float> RGB = new ArrayList<>();
        float R= (float)Integer.parseInt(hex.substring(0,1), 16);
        float G = (float)Integer.parseInt(hex.substring(2,3), 16);
        float B = (float)Integer.parseInt(hex.substring(5,6), 16);
        RGB.add(R);
        RGB.add(G);
        RGB.add(B);
        return RGB;
    }
}
