package io.github.pkgde;

import com.badlogic.gdx.graphics.glutils.ShaderProgram;

public class BlurShader {

    public static ShaderProgram createShader(boolean horizontal) {

        String vertex =
            "attribute vec4 a_position;\n" +
                "attribute vec2 a_texCoord0;\n" +
                "uniform mat4 u_projTrans;\n" +
                "varying vec2 v_texCoords;\n" +
                "void main() {\n" +
                "    v_texCoords = a_texCoord0;\n" +
                "    gl_Position = u_projTrans * a_position;\n" +
                "}";

        String fragment =
            "#ifdef GL_ES\nprecision mediump float;\n#endif\n" +
                "varying vec2 v_texCoords;\n" +
                "uniform sampler2D u_texture;\n" +
                "uniform float blur;\n" +
                "void main() {\n" +
                "    vec4 sum = vec4(0.0);\n";

        if (horizontal) {
            fragment +=
                "sum += texture2D(u_texture, v_texCoords + vec2(-4.0*blur, 0.0)) * 0.05;\n" +
                    "sum += texture2D(u_texture, v_texCoords + vec2(-2.0*blur, 0.0)) * 0.09;\n" +
                    "sum += texture2D(u_texture, v_texCoords) * 0.62;\n" +
                    "sum += texture2D(u_texture, v_texCoords + vec2(2.0*blur, 0.0)) * 0.09;\n" +
                    "sum += texture2D(u_texture, v_texCoords + vec2(4.0*blur, 0.0)) * 0.05;\n";
        } else {
            fragment +=
                "sum += texture2D(u_texture, v_texCoords + vec2(0.0, -4.0*blur)) * 0.05;\n" +
                    "sum += texture2D(u_texture, v_texCoords + vec2(0.0, -2.0*blur)) * 0.09;\n" +
                    "sum += texture2D(u_texture, v_texCoords) * 0.62;\n" +
                    "sum += texture2D(u_texture, v_texCoords + vec2(0.0, 2.0*blur)) * 0.09;\n" +
                    "sum += texture2D(u_texture, v_texCoords + vec2(0.0, 4.0*blur)) * 0.05;\n";
        }

        fragment += "gl_FragColor = sum;\n}";

        ShaderProgram.pedantic = false;

        ShaderProgram shader = new ShaderProgram(vertex, fragment);

        if (!shader.isCompiled()) {
            System.out.println("SHADER ERROR:\n" + shader.getLog());
        }

        return shader;
    }
}
