import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_FALSE;
import static org.lwjgl.opengl.GL11.GL_TRUE;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.system.MemoryUtil.NULL;
import static org.lwjgl.system.glfw.GLFW.GLFW_KEY_ESCAPE;
import static org.lwjgl.system.glfw.GLFW.GLFW_RELEASE;
import static org.lwjgl.system.glfw.GLFW.GLFW_RESIZABLE;
import static org.lwjgl.system.glfw.GLFW.GLFW_VISIBLE;
import static org.lwjgl.system.glfw.GLFW.glfwCreateWindow;
import static org.lwjgl.system.glfw.GLFW.glfwDefaultWindowHints;
import static org.lwjgl.system.glfw.GLFW.glfwDestroyWindow;
import static org.lwjgl.system.glfw.GLFW.glfwGetPrimaryMonitor;
import static org.lwjgl.system.glfw.GLFW.glfwGetVideoMode;
import static org.lwjgl.system.glfw.GLFW.glfwInit;
import static org.lwjgl.system.glfw.GLFW.glfwMakeContextCurrent;
import static org.lwjgl.system.glfw.GLFW.glfwPollEvents;
import static org.lwjgl.system.glfw.GLFW.glfwSetErrorCallback;
import static org.lwjgl.system.glfw.GLFW.glfwSetWindowPos;
import static org.lwjgl.system.glfw.GLFW.glfwSetWindowShouldClose;
import static org.lwjgl.system.glfw.GLFW.glfwShowWindow;
import static org.lwjgl.system.glfw.GLFW.glfwSwapBuffers;
import static org.lwjgl.system.glfw.GLFW.glfwSwapInterval;
import static org.lwjgl.system.glfw.GLFW.glfwTerminate;
import static org.lwjgl.system.glfw.GLFW.glfwWindowHint;
import static org.lwjgl.system.glfw.GLFW.glfwWindowShouldClose;

import java.nio.ByteBuffer;

import org.lwjgl.Sys;
import org.lwjgl.opengl.ContextCapabilities;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GLContext;
import org.lwjgl.system.glfw.ErrorCallback;
import org.lwjgl.system.glfw.GLFWvidmode;
import org.lwjgl.system.glfw.WindowCallback;
import org.lwjgl.system.glfw.WindowCallbackAdapter;
import org.lwjgl.system.linux.GLFWLinux;
import org.lwjgl.system.linux.GLX;
import org.lwjgl.system.linux.opengl.LinuxGLContext;
 
public class HelloWorld {
 
    private long window;
 
    public void execute() {
        System.out.println("Hello LWJGL " + Sys.getVersion() + "!");
 
        try {
            init();
            loop();
            glfwDestroyWindow(window);
        } finally {
            glfwTerminate();
        }
    }
 
    private void init() {
        glfwSetErrorCallback(ErrorCallback.Util.getDefault());
 
        if ( glfwInit() != GL11.GL_TRUE )
            throw new IllegalStateException("Unable to initialize GLFW");
 
        glfwDefaultWindowHints();
        glfwWindowHint(GLFW_VISIBLE, GL_FALSE);
        glfwWindowHint(GLFW_RESIZABLE, GL_TRUE);
 
        int WIDTH = 300;
        int HEIGHT = 300;
 
        window = glfwCreateWindow(WIDTH, HEIGHT, "Hello World!", NULL, NULL);
        if ( window == NULL )
            throw new RuntimeException("Failed to create the GLFW window");
 
        WindowCallback.set(window, new WindowCallbackAdapter() {
            @Override
            public void key(long window, int key, int scancode, int action, int mods) {
                if ( key == GLFW_KEY_ESCAPE && action == GLFW_RELEASE )
                    glfwSetWindowShouldClose(window, GL_TRUE);
            }
        });
 
        ByteBuffer vidmode = glfwGetVideoMode(glfwGetPrimaryMonitor());
        glfwSetWindowPos(
            window,
            (GLFWvidmode.width(vidmode) - WIDTH) / 2,
            (GLFWvidmode.height(vidmode) - HEIGHT) / 2
        );
 
        glfwMakeContextCurrent(window);
        System.out.println("GLX CURR: " + GLX.glXGetCurrentContext());
        System.out.println("GLFW CTX: " + GLFWLinux.glfwGetGLXContext(window));
        System.out.println("GLFW DSP: " + GLFWLinux.glfwGetX11Display());
        
        glfwSwapInterval(1);
        
        glfwShowWindow(window);
    }
 
    private void loop() {
        GLContext.createFromCurrent();
        //GLFWLinux.glfwGetGLXContext(window);
        //GLX.glXGetCurrentContext();
        //glfwMakeContextCurrent(window);
 
        glClearColor(1.0f, 0.0f, 0.0f, 0.0f);
        while ( glfwWindowShouldClose(window) == GL_FALSE ) {
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
 
            glfwSwapBuffers(window);
            glfwPollEvents();
        }
    }
 
    public static void main(String[] args) {
        new HelloWorld().execute();
    }
}