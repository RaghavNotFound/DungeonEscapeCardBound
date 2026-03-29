package io.github.pkgde;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class BlurShader
{
    public static ShaderProgram createShader(boolean horizontal)
    {
        String vertex=
            """
                attribute vec4 a_position;
                attribute vec2 a_texCoord0;
                uniform mat4 u_projTrans;
                varying vec2 v_texCoords;
                void main() {
                v_texCoords=a_texCoord0;
                gl_Position=u_projTrans*a_position;
                }""";

        String fragment =
            """
                #ifdef GL_ES
                precision medium float;
                #endif
                varying vec2 v_texCoords;
                uniform sampler2D u_texture;
                uniform float blur;
                void main(){
                vec4 sum=vec4(0.0);
                """;

        if (horizontal) {
            fragment+=
                """
                    sum+=texture2D(u_texture,v_texCoords+vec2(-4.0*blur,0.0))*0.05;
                    sum+=texture2D(u_texture,v_texCoords+vec2(-2.0*blur,0.0))*0.09;
                    sum+=texture2D(u_texture,v_texCoords)*0.62;
                    sum+=texture2D(u_texture,v_texCoords+vec2(2.0*blur,0.0))*0.09;
                    sum+=texture2D(u_texture,v_texCoords+vec2(4.0*blur,0.0))*0.05;
                    """;
        }
        else
        {
            fragment+=
                """
                    sum+=texture2D(u_texture,v_texCoords+vec2(0.0,-4.0*blur))*0.05;
                    sum+=texture2D(u_texture,v_texCoords+vec2(0.0,-2.0*blur))*0.09;
                    sum+=texture2D(u_texture,v_texCoords)*0.62;
                    sum+=texture2D(u_texture,v_texCoords+vec2(0.0,2.0*blur))*0.09;
                    sum+=texture2D(u_texture,v_texCoords+vec2(0.0,4.0*blur))*0.05;
                    """;
        }
        fragment+="gl_FragColor=sum;\n}";

        ShaderProgram.pedantic=false;

        ShaderProgram shader=new ShaderProgram(vertex,fragment);

        if (!shader.isCompiled())
        {
            System.out.println("SHADER ERROR:\n"+shader.getLog());
        }
        return shader;
    }
    public static void dispose(ShaderProgram shader)
    {
        if (shader!=null)
        {
            shader.dispose();
        }
    }
}
