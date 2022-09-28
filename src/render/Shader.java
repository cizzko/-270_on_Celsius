/* package render;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.lwjgl.opengl.GL20.*;

public class Shader {
    private int shaderProgramId;
    private String vertexSource;
    private String fragmentSource;
    private String filepath;

    public Shader(String filepath){
        this.filepath = filepath;
        try{
            //ищет два шаблона #type, обрезает им все точки и пробелы

            String source = new String(Files.readAllBytes(Paths.get(filepath)));
            String[] splitString = source.split("(#type)( )+([a-zA-Z]+)");

            int index = source.indexOf("#type") + 6;
            int eol = source.indexOf("\r\n", index);
            String firstPattern = source.substring(index, eol).trim();

            index = source.indexOf("#type", eol) + 6;
            eol = source.indexOf("\r\n", index);
            String secondPattern = source.substring(index, eol).trim();

            if(firstPattern.equals("vertex")){
                vertexSource = splitString[1];
            } else if (firstPattern.equals("fragment")){
                fragmentSource = splitString[1];
            } else {
                throw new IOException("Unexpected token '" + firstPattern + "'");
            }
            //////////////////////////////////////

            if(secondPattern.equals("vertex")){
                vertexSource = splitString[1];
            } else if (secondPattern.equals("fragment")){
                fragmentSource = splitString[1];
            } else {
                throw new IOException("Unexpected token '" + secondPattern + "'");
            }

        } catch (IOException e){
            e.printStackTrace();
            assert false: "Err: could not open file for shader '" + filepath + "'";
        }
        System.out.println(vertexSource);
        System.out.println(fragmentSource);
    }

    public void compile(){
        int vertexID, fragmentID;

        vertexID = glCreateShader(GL_VERTEX_SHADER);
        glShaderSource(vertexID, vertexSource);
        glCompileShader(vertexID);

        int succes = glGetShaderi(vertexID, GL_COMPILE_STATUS);
        if(succes == GL_FALSE){
            int len = glGetShaderi(vertexID, GL_INFO_LOG_LENGTH);

            System.out.println("err on 'defaultShader.glsl': vertex shader compilation failed");
            System.out.println(glGetShaderInfoLog(vertexID, len));
            assert false: "fatal err";
        }
        fragmentID = glCreateShader(GL_FRAGMENT_SHADER);
        glShaderSource(fragmentID, fragmentSource);
        glCompileShader(fragmentID);

        succes = glGetShaderi(fragmentID, GL_COMPILE_STATUS);
        if(succes == GL_FALSE){
            int len = glGetShaderi(vertexID, GL_INFO_LOG_LENGTH);

            System.out.println("err on 'defaultShader.glsl': vertex shader compilation failed");
            System.out.println(glGetShaderInfoLog(fragmentID, len));
            assert false: "fatal err";
        }
    }

    public void use(){

    }

    public void detach(){

    }
}
*/