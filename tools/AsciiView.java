import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Prints a PNG as ASCII art so textures can be eyeballed from a terminal.
 * Transparent pixels become spaces, brightness picks the ramp character.
 * <p>
 * Run:  java tools/AsciiView.java <file.png> [more.png ...]
 */
public class AsciiView {
    static final String RAMP = " .:-=+*#%@";

    public static void main(String[] args) throws Exception {
        for (String path : args) {
            BufferedImage img = ImageIO.read(new File(path));
            if (img == null) {
                System.out.println("!! cannot read " + path);
                continue;
            }
            System.out.println("== " + new File(path).getName() + "  " + img.getWidth() + "x" + img.getHeight() + " ==");
            int step = Math.max(1, img.getWidth() / 40);
            for (int y = 0; y < img.getHeight(); y += step) {
                StringBuilder sb = new StringBuilder();
                for (int x = 0; x < img.getWidth(); x += step) {
                    int argb = img.getRGB(x, y);
                    int a = (argb >>> 24) & 0xFF;
                    if (a < 40) {
                        sb.append(' ');
                        continue;
                    }
                    int lum = (int) (0.299 * ((argb >> 16) & 0xFF) + 0.587 * ((argb >> 8) & 0xFF) + 0.114 * (argb & 0xFF));
                    int idx = lum * (RAMP.length() - 1) / 255;
                    sb.append(RAMP.charAt(idx));
                }
                System.out.println(sb);
            }
            System.out.println();
        }
    }
}
