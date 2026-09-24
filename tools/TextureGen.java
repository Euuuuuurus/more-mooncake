import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * Paints every mooncake texture for the More Mooncake mod.
 * <p>
 * Textures are painted on a 64x64 canvas (4x supersampling, which is what makes the shading
 * steps, grains and patina possible) and then box-averaged down to the final <b>16x16</b>
 * vanilla-sized texture in {@link #finish}.
 * <p>
 * Textures are deterministic: the random seed comes from the texture name, so re-running the
 * generator always produces exactly the same files.
 * <p>
 * Run:  java tools/TextureGen.java <projectRoot>
 * <p>
 * Note for the block textures: the block models map their faces by world position
 * (see ResourceGen.boxElement), which means the side/inside sheets are sampled from the
 * <b>lower half</b> only (uv v 8..16 covers the 8px tall pie). {@link #SIDE_TOP} marks where
 * that visible band starts, so the cross-section is painted exactly there.
 */
public class TextureGen {
    static final String MOD = "more_mooncake";
    /** Painting resolution (4x supersampling). */
    static final int S = 64;
    /** Final texture resolution: vanilla-sized 16x16. */
    static final int OUT = 16;
    static final int BOX = S / OUT;
    static final double CX = 32.0, CY = 32.0;
    /** Pie radius and filling radius, in 64x64 pixels (16px = one block). */
    static final double R = 30.0;
    static final double IR = 23.5;
    /** Half angle of one wedge. */
    static final double HALF = 22.5;
    /** First row of the side/inside sheets that the block models actually sample. */
    static final int SIDE_TOP = 32;

    static final String[] FLAVORS = {"wuren", "dousha", "suzi", "hongzao", "xianyadan"};
    static final String[] STATES = {"", "rusted", "weathered", "oxidized", "waxed", "waxed_rusted", "waxed_weathered", "waxed_oxidized"};

    static Path root, itemDir, blockDir;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        root = Paths.get(args[0]).toAbsolutePath();
        itemDir = root.resolve("common/src/main/resources/assets/" + MOD + "/textures/item");
        blockDir = root.resolve("common/src/main/resources/assets/" + MOD + "/textures/block");
        Files.createDirectories(itemDir);
        Files.createDirectories(blockDir);

        int n = 0;
        for (String flavor : FLAVORS) {
            for (String state : STATES) {
                ImageIO.write(drawSlice(flavor, state), "png", itemDir.resolve(itemId(flavor, state) + ".png").toFile());
                n++;
            }
        }
        ImageIO.write(drawWhole(true, true), "png", itemDir.resolve("mooncake.png").toFile());
        genBlockTextures();
        genPackIcons();
        genPreview();
        System.out.println("TextureGen: " + n + " slices + whole item + 4 block textures + pack icons + preview");
        System.out.println("TextureGen done -> " + root);
    }

    // ---------------- output ----------------

    /**
     * Box-averages the 64x64 painting down to the final 16x16 texture and restores a little
     * contrast, so a texture still reads clearly at vanilla size.
     */
    static BufferedImage finish(BufferedImage hi) {
        BufferedImage out = new BufferedImage(OUT, OUT, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < OUT; y++) {
            for (int x = 0; x < OUT; x++) {
                int r = 0, g = 0, b = 0, a = 0, n = 0;
                for (int dy = 0; dy < BOX; dy++) {
                    for (int dx = 0; dx < BOX; dx++) {
                        int argb = hi.getRGB(x * BOX + dx, y * BOX + dy);
                        int aa = (argb >>> 24) & 0xFF;
                        if (aa == 0) continue;
                        r += (argb >> 16) & 0xFF;
                        g += (argb >> 8) & 0xFF;
                        b += argb & 0xFF;
                        a += aa;
                        n++;
                    }
                }
                if (n == 0 || a / n < 128) continue; // crisp pixel edges, no soft alpha
                int cr = clamp255((int) (128 + (r / n - 128) * 1.10));
                int cg = clamp255((int) (128 + (g / n - 128) * 1.10));
                int cb = clamp255((int) (128 + (b / n - 128) * 1.10));
                out.setRGB(x, y, 0xFF000000 | (cr << 16) | (cg << 8) | cb);
            }
        }
        return out;
    }

    static String itemId(String flavor, String state) {
        return flavor + "_mooncake" + (state.isEmpty() ? "" : "_" + state);
    }

    // ---------------- colour helpers ----------------

    static int[] mix(int[] a, int[] b, double t) {
        t = clamp(t);
        return new int[]{(int) Math.round(a[0] + (b[0] - a[0]) * t),
                (int) Math.round(a[1] + (b[1] - a[1]) * t),
                (int) Math.round(a[2] + (b[2] - a[2]) * t)};
    }

    static double clamp(double v) {
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    static double smoothstep(double t) {
        t = clamp(t);
        return t * t * (3 - 2 * t);
    }

    static int rgba(int[] c) {
        return 0xFF000000 | (clamp255(c[0]) << 16) | (clamp255(c[1]) << 8) | clamp255(c[2]);
    }

    static int clamp255(int v) {
        return v < 0 ? 0 : (v > 255 ? 255 : v);
    }

    static boolean painted(BufferedImage img, int x, int y) {
        if (x < 0 || y < 0 || x >= S || y >= S) return false;
        return ((img.getRGB(x, y) >>> 24) & 0xFF) != 0;
    }

    /** Blends {@code col} into an already painted pixel; fully transparent pixels stay empty. */
    static void blend(BufferedImage img, int x, int y, int[] col, double a) {
        if (x < 0 || y < 0 || x >= S || y >= S || a <= 0) return;
        int argb = img.getRGB(x, y);
        if (((argb >>> 24) & 0xFF) == 0) return;
        int[] base = {(argb >> 16) & 0xFF, (argb >> 8) & 0xFF, argb & 0xFF};
        img.setRGB(x, y, rgba(mix(base, col, a)));
    }

    /** Soft round blob with a fading rim. {@code minR} > 0 keeps it out of the filling centre. */
    static void blob(BufferedImage img, double sx, double sy, double sr, int[] col, double strength, double minR) {
        int x0 = (int) Math.floor(sx - sr), x1 = (int) Math.ceil(sx + sr);
        int y0 = (int) Math.floor(sy - sr), y1 = (int) Math.ceil(sy + sr);
        for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
                double d = Math.hypot(x + 0.5 - sx, y + 0.5 - sy);
                if (d > sr) continue;
                if (minR > 0 && Math.hypot(x + 0.5 - CX, y + 0.5 - CY) < minR) continue;
                blend(img, x, y, col, strength * (1 - d / sr * 0.85));
            }
        }
    }

    // ---------------- point pickers ----------------

    static boolean inWedge(double dx, double dy) {
        return Math.abs(Math.toDegrees(Math.atan2(dy, dx))) <= HALF;
    }

    /** Random point inside the wedge, within {@code rMax} of the centre. */
    static double[] wedgePoint(Random rnd, double rMax) {
        double a = Math.toRadians((rnd.nextDouble() * 2 - 1) * 21.0);
        double r = Math.sqrt(rnd.nextDouble()) * rMax;
        return new double[]{CX + Math.cos(a) * r, CY + Math.sin(a) * r};
    }

    /** Random point on the crust ring of the wedge. */
    static double[] crustPoint(Random rnd) {
        double a = Math.toRadians((rnd.nextDouble() * 2 - 1) * 21.5);
        double r = IR + 1.5 + rnd.nextDouble() * (R - IR - 2.5);
        return new double[]{CX + Math.cos(a) * r, CY + Math.sin(a) * r};
    }

    /** Random point in the full disc between rMin and rMax. */
    static double[] discPoint(Random rnd, double rMin, double rMax) {
        double a = rnd.nextDouble() * 2 * Math.PI;
        double r = rMin + Math.sqrt(rnd.nextDouble()) * (rMax - rMin);
        return new double[]{CX + Math.cos(a) * r, CY + Math.sin(a) * r};
    }

    // ---------------- crust shading ----------------

    /** Crust palette per oxidation state: base, shaded, spot colour. */
    static int[][] palette(String base) {
        switch (base) {
            case "rusted":
                return new int[][]{{198, 118, 62}, {150, 80, 34}, {124, 58, 23}};
            case "weathered":
                return new int[][]{{172, 168, 92}, {128, 126, 62}, {111, 140, 78}};
            case "oxidized":
                return new int[][]{{102, 176, 158}, {64, 134, 120}, {50, 112, 100}};
            default:
                return new int[][]{{228, 170, 100}, {188, 130, 64}, {132, 88, 40}};
        }
    }

    /** Baked crust pixel: radial shading + top-left light + dark rim. */
    static int[] crustPixel(int[][] pal, double d, double dx, double dy) {
        double t = clamp((d - IR) / (R - IR));
        int[] c = mix(pal[0], pal[1], smoothstep(t) * 0.95);
        double l = clamp(-(dx + dy) / (R * 1.45));
        c = mix(c, new int[]{255, 246, 224}, l * 0.20 * (1 - t * 0.55));
        if (t > 0.80) c = mix(c, new int[]{86, 54, 24}, (t - 0.80) / 0.20 * 0.40);
        return c;
    }

    /** Filling pixel: flavour colour with a soft radial falloff. */
    static int[] fillingPixel(String flavor, double d) {
        double t = smoothstep(clamp(d / IR));
        switch (flavor) {
            case "dousha":
                return mix(new int[]{152, 66, 62}, new int[]{116, 38, 38}, t);
            case "suzi":
                return mix(new int[]{92, 58, 120}, new int[]{64, 38, 88}, t);
            case "hongzao":
                return mix(new int[]{188, 68, 62}, new int[]{158, 46, 46}, t);
            case "xianyadan":
                if (d < 9) return new int[]{255, 216, 132};
                if (d < 15) return mix(new int[]{255, 216, 132}, new int[]{243, 172, 72}, (d - 9) / 6.0);
                return mix(new int[]{243, 172, 72}, new int[]{230, 150, 54}, (d - 15) / (IR - 15));
            default: // wuren
                return mix(new int[]{246, 236, 212}, new int[]{230, 212, 176}, t);
        }
    }

    // ---------------- shared passes ----------------

    /** Fine baked mottling so the crust is not a flat gradient. */
    static void bakeMottle(BufferedImage img, Random rnd, boolean wedgeOnly, int count) {
        bakeMottle(img, rnd, wedgeOnly, count, 4.0, R - 2);
    }

    /**
     * @param count  how many speckles to scatter
     * @param rMin   inner radius that stays untouched (keeps seals clean)
     * @param rMax   outer radius the speckles stay inside
     */
    static void bakeMottle(BufferedImage img, Random rnd, boolean wedgeOnly, int count, double rMin, double rMax) {
        for (int i = 0; i < count; i++) {
            double[] p = wedgeOnly ? crustPoint(rnd) : discPoint(rnd, rMin, rMax);
            blob(img, p[0], p[1], 1.6 + rnd.nextDouble() * 3.0, new int[]{142, 96, 44}, 0.13 + rnd.nextDouble() * 0.10, 0);
            if (rnd.nextBoolean()) {
                double[] q = wedgeOnly ? crustPoint(rnd) : discPoint(rnd, rMin, rMax);
                blob(img, q[0], q[1], 1.0 + rnd.nextDouble() * 1.8, new int[]{252, 228, 180}, 0.10 + rnd.nextDouble() * 0.08, 0);
            }
        }
    }

    /** Copper oxidation patina / rust spots, painted on top of the crust. */
    static void oxidation(BufferedImage img, String base, Random rnd, boolean wedgeOnly) {
        int[][] p = palette(base);
        switch (base) {
            case "rusted":
                for (int i = 0; i < 9; i++) rustSpot(img, rnd, p, wedgeOnly);
                break;
            case "weathered":
                for (int i = 0; i < 7; i++) patinaSpot(img, rnd, p[2], wedgeOnly, 3.5);
                for (int i = 0; i < 4; i++) rustSpot(img, rnd, p, wedgeOnly);
                break;
            case "oxidized":
                for (int i = 0; i < 11; i++) patinaSpot(img, rnd, p[2], wedgeOnly, 4.5);
                for (int i = 0; i < 5; i++) patinaSpot(img, rnd, mix(p[2], new int[]{255, 255, 255}, 0.35), wedgeOnly, 2.5);
                for (int i = 0; i < 2; i++) rustSpot(img, rnd, p, wedgeOnly);
                break;
            default:
                break;
        }
    }

    static void rustSpot(BufferedImage img, Random rnd, int[][] pal, boolean wedgeOnly) {
        double[] p = wedgeOnly ? crustPoint(rnd) : discPoint(rnd, IR - 4, R - 2);
        double r = 2.4 + rnd.nextDouble() * 3.6;
        blob(img, p[0], p[1], r, new int[]{108, 48, 18}, 0.80, 0);
        blob(img, p[0] + 0.6, p[1] + 0.6, r * 0.62, new int[]{150, 78, 32}, 0.55, 0);
        blob(img, p[0] - 0.8, p[1] - 0.8, r * 0.45, new int[]{188, 106, 52}, 0.35, 0);
    }

    static void patinaSpot(BufferedImage img, Random rnd, int[] col, boolean wedgeOnly, double extra) {
        double[] p = wedgeOnly ? crustPoint(rnd) : discPoint(rnd, IR - 5, R - 2);
        double r = extra + rnd.nextDouble() * 4.0;
        blob(img, p[0], p[1], r, mix(col, new int[]{20, 40, 34}, 0.35), 0.72, 0);
        blob(img, p[0] + 0.5, p[1] + 0.5, r * 0.65, col, 0.55, 0);
        blob(img, p[0] - 0.6, p[1] - 0.6, r * 0.35, mix(col, new int[]{255, 255, 255}, 0.30), 0.40, 0);
    }

    /** Shading on the two straight cut faces of the wedge. */
    static void wedgeCutFaces(BufferedImage img) {
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                if (!painted(img, x, y)) continue;
                double ang = Math.abs(Math.toDegrees(Math.atan2(y + 0.5 - CY, x + 0.5 - CX)));
                if (ang > HALF) continue;
                if (ang > 20.5) blend(img, x, y, new int[]{84, 50, 22}, 0.42);
                else if (ang > 18.5) blend(img, x, y, new int[]{96, 60, 30}, 0.17);
            }
        }
    }

    /** Glossy wax film for the preserved variants. */
    static void waxSheen(BufferedImage img, Random rnd, boolean wedgeOnly) {
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                if (!painted(img, x, y)) continue;
                int band = (x + y) % 24;
                if (band < 9) blend(img, x, y, new int[]{255, 250, 224}, 0.14);
                if (band < 3) blend(img, x, y, new int[]{255, 255, 244}, 0.20);
            }
        }
        for (int i = 0; i < 14; i++) {
            double[] p = wedgeOnly ? wedgePoint(rnd, R - 5) : discPoint(rnd, 5, R - 4);
            blob(img, p[0], p[1], 0.9 + rnd.nextDouble() * 1.2, new int[]{255, 255, 244}, 0.55, 0);
        }
    }

    /** Dark contour so the icon stays readable in the inventory. */
    static void outline(BufferedImage img) {
        int[][] dirs = {{1, 0}, {-1, 0}, {0, 1}, {0, -1}};
        List<int[]> border = new ArrayList<>();
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                if (!painted(img, x, y)) continue;
                for (int[] d : dirs) {
                    if (!painted(img, x + d[0], y + d[1])) {
                        border.add(new int[]{x, y});
                        break;
                    }
                }
            }
        }
        for (int[] p : border) {
            blend(img, p[0], p[1], new int[]{72, 44, 20}, 0.42);
        }
    }

    // ---------------- slice (1/8 wedge) ----------------

    static BufferedImage drawSlice(String flavor, String state) {
        return finish(paintSlice(flavor, state));
    }

    static BufferedImage paintSlice(String flavor, String state) {
        BufferedImage img = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(itemId(flavor, state).hashCode());
        boolean waxed = state.startsWith("waxed");
        String base = waxed ? state.substring(5) : state;
        int[][] pal = palette(base);

        // 1) base shape: crust ring + flavoured filling
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                double dx = x + 0.5 - CX, dy = y + 0.5 - CY;
                double d = Math.hypot(dx, dy);
                if (d > R || !inWedge(dx, dy)) continue;
                img.setRGB(x, y, rgba(d > IR ? crustPixel(pal, d, dx, dy) : fillingPixel(flavor, d)));
            }
        }
        // 2) filling texture (grains, seeds, paste speckles)
        grains(img, flavor, rnd);
        // 3) baked mottling on the crust (kept sparse: a busy crust looks like a mess at 16x16)
        bakeMottle(img, rnd, true, 7);
        // 4) copper oxidation
        oxidation(img, base, rnd, true);
        // 5) shading on the two cut faces
        wedgeCutFaces(img);
        // 6) wax film
        if (waxed) waxSheen(img, rnd, true);
        // 7) contour
        outline(img);
        return img;
    }

    /** Per-flavour filling detail. */
    static void grains(BufferedImage img, String flavor, Random rnd) {
        switch (flavor) {
            case "dousha":
                for (int i = 0; i < 150; i++) {
                    double[] p = wedgePoint(rnd, IR - 1);
                    blob(img, p[0], p[1], 0.6 + rnd.nextDouble() * 0.9, new int[]{104, 32, 32}, 0.26, 0);
                }
                for (int i = 0; i < 50; i++) {
                    double[] p = wedgePoint(rnd, IR - 2);
                    blob(img, p[0], p[1], 0.5 + rnd.nextDouble() * 0.8, new int[]{176, 100, 94}, 0.20, 0);
                }
                break;
            case "suzi":
                for (int i = 0; i < 70; i++) {
                    double[] p = wedgePoint(rnd, IR - 1);
                    blob(img, p[0], p[1], 0.6 + rnd.nextDouble() * 1.1, new int[]{116, 82, 154}, 0.42, 0);
                }
                for (int i = 0; i < 40; i++) {
                    double[] p = wedgePoint(rnd, IR - 2);
                    blob(img, p[0], p[1], 0.5 + rnd.nextDouble() * 0.8, new int[]{46, 26, 64}, 0.34, 0);
                }
                break;
            case "hongzao":
                for (int i = 0; i < 20; i++) {
                    double[] p = wedgePoint(rnd, IR - 3);
                    double a = rnd.nextDouble() * Math.PI * 2, len = 3.5 + rnd.nextDouble() * 4.5;
                    for (double t = 0; t <= 1.0; t += 0.1) {
                        blob(img, p[0] + Math.cos(a) * len * t, p[1] + Math.sin(a) * len * t, 0.9, new int[]{116, 32, 34}, 0.30, 0);
                    }
                }
                for (int i = 0; i < 40; i++) {
                    double[] p = wedgePoint(rnd, IR - 2);
                    blob(img, p[0], p[1], 0.5 + rnd.nextDouble() * 0.8, new int[]{214, 104, 92}, 0.18, 0);
                }
                break;
            case "xianyadan":
                for (int i = 0; i < 40; i++) {
                    double[] p = wedgePoint(rnd, 12);
                    blob(img, p[0], p[1], 0.8 + rnd.nextDouble() * 1.3, new int[]{255, 236, 176}, 0.32, 0);
                }
                for (int i = 0; i < 30; i++) {
                    double[] p = wedgePoint(rnd, IR - 1);
                    blob(img, p[0], p[1], 0.6 + rnd.nextDouble() * 1.0, new int[]{228, 148, 52}, 0.26, 0);
                }
                break;
            default: // wuren: mixed nuts and seeds
                for (int i = 0; i < 30; i++) {
                    double[] p = wedgePoint(rnd, IR - 1);
                    blob(img, p[0], p[1], 1.3 + rnd.nextDouble() * 2.0, new int[]{96, 62, 32}, 0.80, 0);
                    blob(img, p[0] - 0.5, p[1] - 0.5, 0.8, new int[]{146, 100, 54}, 0.45, 0);
                }
                for (int i = 0; i < 22; i++) {
                    double[] p = wedgePoint(rnd, IR - 1);
                    blob(img, p[0], p[1], 0.9 + rnd.nextDouble() * 1.5, new int[]{250, 242, 220}, 0.70, 0);
                }
                for (int i = 0; i < 14; i++) {
                    double[] p = wedgePoint(rnd, IR - 2);
                    blob(img, p[0], p[1], 0.8 + rnd.nextDouble() * 1.2, new int[]{140, 158, 96}, 0.35, 0);
                }
                for (int i = 0; i < 10; i++) {
                    double[] p = wedgePoint(rnd, IR - 2);
                    blob(img, p[0], p[1], 0.7 + rnd.nextDouble(), new int[]{186, 84, 62}, 0.35, 0);
                }
                break;
        }
    }

    // ---------------- whole mooncake ----------------

    static BufferedImage drawWhole(boolean cutLines, boolean seal) {
        return finish(paintWhole(cutLines, seal));
    }

    static BufferedImage paintWhole(boolean cutLines, boolean seal) {
        BufferedImage img = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(cutLines ? 7717 : 7718);
        int[][] pal = palette("");

        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                double dx = x + 0.5 - CX, dy = y + 0.5 - CY;
                double d = Math.hypot(dx, dy);
                if (d > R) continue;
                int[] c = crustPixel(pal, d, dx, dy);
                // simple centre seal: red disc with a dark rim and a small golden moon dot
                if (seal && d <= 9.6) {
                    c = d > 8.0 ? new int[]{116, 34, 34} : new int[]{152, 46, 44};
                    if (d <= 3.4) c = new int[]{248, 210, 116};
                    else if (d <= 4.1) c = new int[]{204, 158, 78};
                }
                img.setRGB(x, y, rgba(c));
            }
        }
        // keep the mottling light and out of the seal so the icon stays clean
        bakeMottle(img, rnd, false, 9, 12.0, R - 3);
        // 8 radial cut lines
        if (cutLines) {
            for (int y = 0; y < S; y++) {
                for (int x = 0; x < S; x++) {
                    if (!painted(img, x, y)) continue;
                    double dx = x + 0.5 - CX, dy = y + 0.5 - CY;
                    double d = Math.hypot(dx, dy);
                    if (d < 7 || d > R - 2.0) continue;
                    double ang = Math.toDegrees(Math.atan2(dy, dx));
                    double diff = 180;
                    for (int k = 0; k < 8; k++) {
                        double dd = Math.abs(ang - (HALF + k * 45.0));
                        diff = Math.min(diff, Math.min(dd, 360 - dd));
                    }
                    double fade = Math.min(1.0, (d - 7) / 4.0) * Math.min(1.0, (R - 2.0 - d) / 4.0);
                    if (diff < 0.9) blend(img, x, y, new int[]{104, 66, 30}, 0.34 * fade);
                    else if (diff < 2.0) blend(img, x, y, new int[]{126, 86, 44}, 0.13 * fade);
                }
            }
        }
        outline(img);
        return img;
    }

    // ---------------- block textures ----------------

    static void genBlockTextures() throws IOException {
        // The pie is 8px tall, so the side sheets only need the sampled band; the rest of the
        // sheet repeats the top colour so it still looks sane if anything samples it directly.
        // The top sheet carries no seal: on the ground the pie is cut into wedges, and the
        // renderer glazes every wedge with its own filling colour.
        ImageIO.write(drawWhole(true, false), "png", blockDir.resolve("mooncake_top.png").toFile());
        ImageIO.write(drawSide(), "png", blockDir.resolve("mooncake_side.png").toFile());
        ImageIO.write(drawInside(), "png", blockDir.resolve("mooncake_inside.png").toFile());
        ImageIO.write(drawBottom(), "png", blockDir.resolve("mooncake_bottom.png").toFile());
    }

    /**
     * Baked cross-section: crust cap, cream filling band, crust base.
     * Painted inside the {@link #SIDE_TOP}..S band, which is what the block models sample.
     */
    static BufferedImage drawSide() {
        return finish(paintSide());
    }

    static BufferedImage paintSide() {
        BufferedImage img = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(4242);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                double t = clamp((y - SIDE_TOP) / (double) (S - SIDE_TOP)); // 0 top .. 1 bottom
                int[] c;
                if (y < SIDE_TOP) {
                    c = new int[]{232, 176, 108};
                } else if (t < 0.20) {
                    c = mix(new int[]{240, 190, 126}, new int[]{226, 166, 96}, t / 0.20);
                } else if (t < 0.30) {
                    c = mix(new int[]{226, 166, 96}, new int[]{238, 220, 182}, (t - 0.20) / 0.10);
                } else if (t < 0.72) {
                    c = mix(new int[]{242, 226, 190}, new int[]{232, 210, 168}, (t - 0.30) / 0.42);
                } else if (t < 0.82) {
                    c = mix(new int[]{232, 210, 168}, new int[]{206, 148, 82}, (t - 0.72) / 0.10);
                } else {
                    c = mix(new int[]{206, 148, 82}, new int[]{168, 114, 58}, (t - 0.82) / 0.18);
                }
                img.setRGB(x, y, rgba(c));
            }
        }
        // crust sheen at the very top of the visible band, ambient shade at the bottom
        for (int x = 0; x < S; x++) {
            for (int y = SIDE_TOP; y < SIDE_TOP + 3; y++) blend(img, x, y, new int[]{255, 244, 216}, 0.22 - (y - SIDE_TOP) * 0.05);
            for (int y = S - 4; y < S; y++) blend(img, x, y, new int[]{92, 56, 26}, 0.10 + (y - (S - 4)) * 0.06);
        }
        // baked spots and air pockets in the filling band
        int bandTop = SIDE_TOP + (int) (0.30 * (S - SIDE_TOP));
        int bandBottom = SIDE_TOP + (int) (0.72 * (S - SIDE_TOP));
        for (int i = 0; i < 90; i++) {
            int x = rnd.nextInt(S);
            int y = bandTop + rnd.nextInt(Math.max(1, bandBottom - bandTop));
            blob(img, x, y, 0.7 + rnd.nextDouble() * 1.4, new int[]{214, 190, 146}, 0.35, 0);
        }
        for (int i = 0; i < 40; i++) {
            int x = rnd.nextInt(S);
            int y = SIDE_TOP + rnd.nextInt(S - SIDE_TOP);
            blob(img, x, y, 0.8 + rnd.nextDouble() * 1.6, new int[]{140, 92, 42}, 0.16, 0);
        }
        return img;
    }

    /** Cream cross-section where the pie has been cut open. */
    static BufferedImage drawInside() {
        return finish(paintInside());
    }

    static BufferedImage paintInside() {
        BufferedImage img = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(9911);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                double t = clamp((y - SIDE_TOP) / (double) (S - SIDE_TOP));
                int[] c = y < SIDE_TOP ? new int[]{244, 231, 202}
                        : mix(new int[]{246, 234, 206}, new int[]{232, 214, 180}, smoothstep(t));
                img.setRGB(x, y, rgba(c));
            }
        }
        for (int i = 0; i < 130; i++) {
            int x = rnd.nextInt(S);
            int y = SIDE_TOP + rnd.nextInt(S - SIDE_TOP);
            blob(img, x, y, 0.6 + rnd.nextDouble() * 1.5, new int[]{220, 198, 158}, 0.30, 0);
        }
        for (int i = 0; i < 26; i++) {
            int x = rnd.nextInt(S);
            int y = SIDE_TOP + rnd.nextInt(S - SIDE_TOP);
            blob(img, x, y, 0.6 + rnd.nextDouble() * 1.1, new int[]{200, 172, 126}, 0.22, 0);
        }
        return img;
    }

    /** Baked base of the pie (seen from below). */
    static BufferedImage drawBottom() {
        return finish(paintBottom());
    }

    static BufferedImage paintBottom() {
        BufferedImage img = new BufferedImage(S, S, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(5150);
        for (int y = 0; y < S; y++) {
            for (int x = 0; x < S; x++) {
                double dx = x + 0.5 - CX, dy = y + 0.5 - CY;
                double d = Math.hypot(dx, dy);
                if (d > R) continue;
                double t = clamp(d / R);
                int[] c = mix(new int[]{214, 154, 84}, new int[]{172, 118, 60}, smoothstep(t) * 0.9);
                if (d > R - 2.5) c = mix(c, new int[]{128, 82, 36}, 0.45);
                img.setRGB(x, y, rgba(c));
            }
        }
        for (int i = 0; i < 22; i++) {
            double[] p = discPoint(rnd, 3, R - 2);
            blob(img, p[0], p[1], 1.6 + rnd.nextDouble() * 3.4, new int[]{138, 88, 40}, 0.28, 0);
        }
        for (int i = 0; i < 14; i++) {
            double[] p = discPoint(rnd, 3, R - 2);
            blob(img, p[0], p[1], 1.2 + rnd.nextDouble() * 2.4, new int[]{236, 186, 118}, 0.22, 0);
        }
        outline(img);
        return img;
    }

    // ---------------- pack icons + preview sheet ----------------

    static void genPackIcons() throws IOException {
        BufferedImage big = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = big.createGraphics();
        g.setColor(new Color(58, 36, 22));
        g.fillRoundRect(0, 0, 128, 128, 22, 22);
        g.setColor(new Color(92, 58, 32));
        g.drawRoundRect(0, 0, 127, 127, 22, 22);
        g.drawImage(paintWhole(true, true), 4, 4, 120, 120, null);
        g.dispose();
        Files.createDirectories(root.resolve("fabric/src/main/resources"));
        Files.createDirectories(root.resolve("neoforge/src/main/resources"));
        ImageIO.write(big, "png", root.resolve("fabric/src/main/resources/pack.png").toFile());
        ImageIO.write(big, "png", root.resolve("neoforge/src/main/resources/pack.png").toFile());
        System.out.println("pack icons written");
    }

    /** Contact sheet so the textures can be reviewed without launching the game. */
    static void genPreview() throws IOException {
        int cell = 84, pad = 10;
        int cols = 8, rows = 6;
        BufferedImage sheet = new BufferedImage(cols * cell + pad * 2, rows * cell + pad * 2, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = sheet.createGraphics();
        g.setColor(new Color(34, 27, 22));
        g.fillRect(0, 0, sheet.getWidth(), sheet.getHeight());
        for (int r = 0; r < FLAVORS.length; r++) {
            for (int c = 0; c < STATES.length; c++) {
                g.setColor(new Color(96, 84, 70));
                g.fillRect(pad + c * cell + 3, pad + r * cell + 3, cell - 6, cell - 6);
                g.drawImage(drawSlice(FLAVORS[r], STATES[c]), pad + c * cell + 10, pad + r * cell + 10, 64, 64, null);
            }
        }
        BufferedImage[] extra = {drawWhole(true, true), drawWhole(true, false), drawSide(), drawInside(), drawBottom()};
        for (int i = 0; i < extra.length; i++) {
            g.setColor(new Color(96, 84, 70));
            g.fillRect(pad + i * cell + 3, pad + 5 * cell + 3, cell - 6, cell - 6);
            g.drawImage(extra[i], pad + i * cell + 10, pad + 5 * cell + 10, 64, 64, null);
        }
        g.dispose();
        Files.createDirectories(root.resolve("tools"));
        ImageIO.write(sheet, "png", root.resolve("tools/preview_grid.png").toFile());
        System.out.println("preview sheet written (tools/preview_grid.png)");
    }
}
