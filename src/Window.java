import World.Textures.TextureDrawing;
import World.WorldGenerator;
import org.lwjgl.glfw.GLFWErrorCallback;
import org.lwjgl.opengl.GL;
import java.awt.*;
import static java.sql.Types.NULL;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.*;

public class Window {
    private int width, height;
    private final String title;
    public static long glfwWindow;
    private static Window window;

    public Window() {
        //чекает размеры твоего экрана
        Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        this.width = dim.width;
        this.height = dim.height;
        this.title = "-270 on Celsius";
    }

    public static Window get() {
        //если окно не существует (null), то оно создается
        if (Window.window == null) {
            Window.window = new Window();
        }
        return Window.window;
    }

    public void run(){
        init();
        loop();
    }

    public void init(){
        //glfwSwapInterval(20);
        //инициализирует библиотеку
        glfwInit();
        System.out.println("'glfw' has been initialized");
        GLFWErrorCallback.createPrint(System.err).set();

        if(glfwWindow == NULL){
            //если окна не существует - создаст
            glfwWindow = glfwCreateWindow(this.width, this.height, this.title, NULL, NULL);
        }
        else {
            //при попытке создания окна возникла ошибка - сообщит
            System.err.println("error at create glfwWindow");
        }
        glfwMakeContextCurrent(glfwWindow);
        //vsync
        //glfwSwapInterval(1);
        //настройка отображения
        glfwShowWindow(glfwWindow);
        //подключает инструменты библиотеки
        GL.createCapabilities();

    }
    public void loop() {
        int targerFps = 75;
        glClear(GL_COLOR_BUFFER_BIT);
        //пока окно не закрыто - каждый такт опрашивает glfw
        while (!glfwWindowShouldClose(glfwWindow)) {
            glfwSwapBuffers(glfwWindow);
            try {
                Thread.sleep(1000/targerFps);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }

            //считывание нажатой клавиши
            //enter
            if (glfwGetKey(glfwWindow, 257) == 1) {
                //TextureDrawing.draw("D:\\-270_on_Celsius\\src\\assets\\World\\air.png", 0, 0, null, null);
                //TextureDrawing.draw("D:\\-270_on_Celsius\\src\\assets\\World\\grass1.png", 60, 0, null, null);
                WorldGenerator.Generate(20, 20);
            }
            glfwWaitEvents();
        }
    }
}
