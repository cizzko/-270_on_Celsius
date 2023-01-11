package GUI;
import core.Window;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.nio.ByteBuffer;
import java.util.*;
import java.util.List;
import java.util.concurrent.Callable;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL13.*;
import GUI.Color3;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.BufferUtils;
import core.Window.*;

public class GL_GUI {
    public static <T, E> @Nullable T getKeyByValue(@NotNull Map<T, E> map, E value) {
        for (Map.Entry<T, E> entry : map.entrySet()) {
            System.out.println(entry.toString());
            if (Objects.equals(value, entry.getValue())) {

                return entry.getKey();
            }
        }
        return null;
    }
    static HashMap<String, HashMap<String,Double>> objects= new HashMap<String, HashMap<String,Double>>();
    static HashMap<String,Integer> commands = new HashMap<String,Integer>();
    static HashMap<String,String> colors=new HashMap<String,String>();
    public static @NotNull String Button(String name, double x, double y, double height, double width, String text, String bg_color, Integer command) {
        HashMap<String,Double> object = new HashMap<String,Double>();
        object.put("x",x);
        object.put("y",y);
        object.put("x1",x+width);
        object.put("y1",y+height);
        object.put("type",1.0);
        object.put(text,88005353535.88005353535);
        GL_GUI.commands.put("button_"+name,command);
        GL_GUI.objects.put("button_"+name,object);
        GL_GUI.colors.put("button_"+name,bg_color);
        return "button_"+name;
    }
    public @NotNull String Label(String name, double x, double y, @NotNull @NotNull String text, double size){
        HashMap<String,Double> object = new HashMap<String,Double>();
        object.put("x",x);
        object.put("y",y);
        object.put("x1",x+text.length()*size);
        object.put("y1",y+size);
        object.put("type",2.0);
        object.put(text,88005353535.88005353535);
        GL_GUI.objects.put("label_"+name,object);
        return "label_"+name;
    }
    public static void pack(long window, String name){
        double type = GL_GUI.objects.get(name).get("type");
        int x = GL_GUI.objects.get(name).get("x").intValue();
        int y= GL_GUI.objects.get(name).get("y").intValue();
        int x1 = GL_GUI.objects.get(name).get("x1").intValue();
        int y1= GL_GUI.objects.get(name).get("y1").intValue();
        int textureID;
        int width =(GL_GUI.objects.get(name).get("x1").intValue()-GL_GUI.objects.get(name).get("x").intValue());
        String str = getKeyByValue(GL_GUI.objects.get(name),88005353535.88005353535);
        System.out.println(str);
        int height =(GL_GUI.objects.get(name).get("y1").intValue()-GL_GUI.objects.get(name).get("y").intValue());
        List<Float> color = Color3.main(GL_GUI.colors.get(name));
        BufferedImage b = new BufferedImage(width, height, BufferedImage.TYPE_4BYTE_ABGR);
        Graphics2D g = b.createGraphics();
        g.drawString(str, 0, 0);

        int co = b.getColorModel().getNumComponents();

        byte[] data = new byte[co * str.length()];
        b.getRaster().getDataElements(0, 0, str.length(), 1, data);

        ByteBuffer pixels = BufferUtils.createByteBuffer(data.length);
        pixels.put(data);
        pixels.rewind();
        GL_GUI.update(window);
        if (type==1.0){
            System.out.println("button");
            glDisable(GL_ALPHA);
            glBegin(GL_QUADS);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glColor4f(color.get(0),color.get(1),color.get(2),1f);
            glVertex2i(x,y);
            glVertex2i(x1,y);
            glVertex2i(x1,y1);
            glVertex2i(x,y1);
            textureID = glGenTextures();
            glEnable(GL_TEXTURE_2D);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
            glTexCoord2f(0, 0);
            glVertex2i(x,y);
            glTexCoord2f(1, 0);
            glVertex2i(x1, y);
            glTexCoord2f(1, 1);;
            glVertex2i(x1, y1);
            glTexCoord2f(0, 1);
            glVertex2i(x, y1);
            glEnd();
            glDisable(GL_TEXTURE_2D);

        }
        GL_GUI.update(window);
    }
    public static void update(long window) {
        glfwSwapBuffers(window);
        glfwPollEvents();
    }
    @Contract(pure = true)
    public static @NotNull String Image_import(double x, double y, String path, int start_x_cut, int start_y_cut, int end_x_cut, int end_y_cut){
        
        return path+"_imported";
    }
}
