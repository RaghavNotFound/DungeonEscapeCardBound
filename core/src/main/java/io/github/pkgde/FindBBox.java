package io.github.pkgde;

import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class FindBBox {
    public static void main(String[] args) throws Exception {
        BufferedImage img = ImageIO.read(new File("assets/Enemy_Sprite/Individual Sprite/Idle/Bringer-of-Death_Idle_1.png"));
        int minX = img.getWidth(), minY = img.getHeight(), maxX = 0, maxY = 0;
        for (int y = 0; y < img.getHeight(); y++) {
            for (int x = 0; x < img.getWidth(); x++) {
                int alpha = (img.getRGB(x, y) >> 24) & 0xff;
                if (alpha > 10) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        System.out.println("Size: " + img.getWidth() + "x" + img.getHeight());
        System.out.println("BBox: " + minX + ", " + minY + " to " + maxX + ", " + maxY);
        System.out.println("Center X: " + (minX + maxX)/2);
    }
}
