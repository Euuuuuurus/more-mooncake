import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * Generates all mod resources (item/block models, blockstates, geometry table, lang
 * files, recipes) for the More Mooncake mod.
 * <p>
 * Textures are painted by {@code tools/TextureGen.java} (64x64) - run that first, then this tool.
 * <p>
 * Run:  java ResourceGen.java <projectRoot>
 */
public class ResourceGen {
    static final String MOD = "more_mooncake";
    static final String[] FLAVORS = {"wuren", "dousha", "suzi", "hongzao", "xianyadan"};
    static final String[] STATES = {"", "rusted", "weathered", "oxidized", "waxed", "waxed_rusted", "waxed_weathered", "waxed_oxidized"};
    static final String[] UNWAXED = {"", "rusted", "weathered", "oxidized"};
    static final String[] WAXED = {"waxed", "waxed_rusted", "waxed_weathered", "waxed_oxidized"};

    static Path root;

    public static void main(String[] args) throws Exception {
        System.setProperty("java.awt.headless", "true");
        root = Paths.get(args[0]).toAbsolutePath();
        // Textures, block sheets and pack icons come from tools/TextureGen.java.
        genModels();
        genWholeModel();
        genGeometry();
        genBlockModels();
        genLang();
        genRecipes();
        System.out.println("ResourceGen done -> " + root);
    }

    static String itemId(String flavor, String state) {
        return flavor + "_mooncake" + (state.isEmpty() ? "" : "_" + state);
    }

    static String fmt(double v) {
        if (v == Math.floor(v)) return String.valueOf((long) v);
        return String.valueOf(v);
    }

    // ---------------- textures ----------------

    static void genTextures() throws IOException {
        Path dir = root.resolve("common/src/main/resources/assets/" + MOD + "/textures/item");
        Files.createDirectories(dir);
        for (String flavor : FLAVORS) {
            for (String state : STATES) {
                String id = itemId(flavor, state);
                ImageIO.write(drawTexture(flavor, state), "png", dir.resolve(id + ".png").toFile());
            }
        }
        System.out.println("textures: " + (FLAVORS.length * STATES.length));
    }

    /** 16x16 slice texture: an 1/8 wedge pointing right, flavor filling at the center. */
    static BufferedImage drawTexture(String flavor, String state) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(itemId(flavor, state).hashCode());
        boolean waxed = state.startsWith("waxed");
        String base = waxed ? state.substring(5) : state;
        int[][] pal = palette(base);
        int[] crust = pal[0], edge = pal[1];

        int[][] patinaSpots = null;
        int[] patina = null;
        if (base.equals("weathered")) {
            patina = new int[]{111, 140, 78};
            patinaSpots = new int[][]{{4, 5}, {11, 4}, {13, 11}, {5, 13}};
        } else if (base.equals("oxidized")) {
            patina = new int[]{50, 112, 100};
            patinaSpots = new int[][]{{3, 4}, {12, 3}, {14, 12}, {5, 14}, {10, 11}, {7, 7}};
        }
        int[][] rustSpots = null;
        if (base.equals("rusted")) {
            rustSpots = new int[5][];
            for (int i = 0; i < rustSpots.length; i++) rustSpots[i] = new int[]{7 + rnd.nextInt(8), 5 + rnd.nextInt(10)};
        }
        int[][] suziSpots = null;
        if (flavor.equals("suzi")) {
            suziSpots = new int[4][];
            for (int i = 0; i < suziSpots.length; i++) suziSpots[i] = new int[]{8 + rnd.nextInt(5), 6 + rnd.nextInt(6)};
        }
        // seeds spread inside the wedge (pointing right from center 8,8)
        int[][] seeds = {{9, 8}, {10, 8}, {12, 8}, {11, 7}, {11, 9}};

        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double dx = x + 0.5 - 8, dy = y + 0.5 - 8;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > 7.5) continue;
                double angle = Math.toDegrees(Math.atan2(dy, dx));
                if (angle < -22.5 || angle > 22.5) continue;
                int[] col = crust;
                if (dist <= 3.6) {
                    switch (flavor) {
                        case "wuren" -> {
                            for (int[] s : seeds) {
                                if (x >= s[0] - 1 && x <= s[0] && y >= s[1] - 1 && y <= s[1]) {
                                    col = (x == s[0] - 1 || y == s[1] - 1) ? new int[]{91, 58, 30} : new int[]{240, 230, 206};
                                    break;
                                }
                            }
                        }
                        case "dousha" -> col = (x >= 9 && x <= 11 && y >= 7 && y <= 9) ? new int[]{156, 76, 76} : new int[]{122, 42, 42};
                        case "suzi" -> {
                            col = new int[]{74, 46, 99};
                            for (int[] sp : suziSpots) if (x == sp[0] && y == sp[1]) col = new int[]{107, 74, 143};
                        }
                        case "hongzao" -> col = (x >= 9 && x <= 10 && y >= 7 && y <= 8) ? new int[]{112, 31, 31} : new int[]{176, 58, 58};
                        case "xianyadan" -> col = dist <= 2.0 ? new int[]{255, 200, 110} : new int[]{240, 162, 58};
                    }
                }
                if (dist > 6.2) col = edge;
                if (rustSpots != null) {
                    for (int[] sp : rustSpots) if (x == sp[0] && y == sp[1]) col = new int[]{124, 58, 23};
                }
                if (patinaSpots != null) {
                    for (int[] p : patinaSpots) {
                        double pd = Math.sqrt((x + 0.5 - p[0]) * (x + 0.5 - p[0]) + (y + 0.5 - p[1]) * (y + 0.5 - p[1]));
                        if (pd <= (base.equals("oxidized") ? 2.0 : 1.7)) {
                            col = patina;
                            break;
                        }
                    }
                }
                if (waxed) {
                    if (x + y >= 9 && x + y <= 14) col = mix(col, new int[]{255, 255, 255}, 0.35);
                    if (x <= 4 && y <= 3) col = mix(col, new int[]{255, 255, 255}, 0.25);
                }
                // cut face shading on the two straight edges of the wedge
                if (Math.abs(Math.abs(angle) - 22.5) < 4.0) {
                    col = mix(col, new int[]{80, 50, 25}, 0.3);
                }
                img.setRGB(x, y, rgba(col));
            }
        }
        return img;
    }

    /** 16x16 whole mooncake (top view): shaded crust, 8 cut lines, red center seal. */
    static BufferedImage drawWhole(boolean withCutLines) {
        BufferedImage img = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        int[] crust = {224, 164, 94}, edge = {185, 127, 62};
        int[] stamp = {146, 44, 44}, stampEdge = {110, 30, 30};
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double dx = x + 0.5 - 8, dy = y + 0.5 - 8;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > 7.5) continue;
                int[] col = dist > 6.2 ? edge : crust;
                // shading: brighter top-left, darker bottom-right
                col = mix(col, new int[]{255, 255, 255}, Math.max(0.0, (14.0 - (x + y)) * 0.02));
                if (dist > 6.2) {
                    col = mix(col, new int[]{60, 35, 15}, 0.35);
                }
                if (withCutLines) {
                    double ang = Math.toDegrees(Math.atan2(dy, dx));
                    for (int k = 0; k < 8; k++) {
                        // wedge boundaries sit at 22.5 + 45k degrees, matching the pie geometry
                        double diff = Math.abs(ang - (22.5 + k * 45.0));
                        diff = Math.min(diff, 360 - diff);
                        if (diff < 2.5 && dist > 2.2) {
                            col = mix(col, new int[]{110, 72, 36}, 0.5);
                        }
                    }
                }
                // red seal in the center with a golden moon dot
                double sdist = Math.sqrt(dx * dx + dy * dy);
                if (sdist <= 2.6) {
                    col = sdist > 2.2 ? stampEdge : stamp;
                }
                if (sdist <= 1.2 && sdist > 0.4) {
                    col = new int[]{250, 214, 120};
                }
                img.setRGB(x, y, rgba(col));
            }
        }
        return img;
    }

    static void genWholeItemTexture() throws IOException {
        Path dir = root.resolve("common/src/main/resources/assets/" + MOD + "/textures/item");
        Files.createDirectories(dir);
        ImageIO.write(drawWhole(true), "png", dir.resolve("mooncake.png").toFile());
        System.out.println("whole item texture written");
    }

    static void genBlockTextures() throws IOException {
        Path dir = root.resolve("common/src/main/resources/assets/" + MOD + "/textures/block");
        Files.createDirectories(dir);
        // The pie geometry already separates the 8 wedges, so the block top needs no cut lines.
        ImageIO.write(drawWhole(false), "png", dir.resolve("mooncake_top.png").toFile());

        BufferedImage side = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int[] c;
                if (y <= 2) c = mix(new int[]{240, 190, 126}, new int[]{224, 164, 94}, y / 2.0);
                else if (y <= 13) c = new int[]{224, 164, 94};
                else c = new int[]{150, 100, 52};
                side.setRGB(x, y, rgba(c));
            }
        }
        ImageIO.write(side, "png", dir.resolve("mooncake_side.png").toFile());

        BufferedImage bottom = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                double dist = Math.sqrt((x + 0.5 - 8) * (x + 0.5 - 8) + (y + 0.5 - 8) * (y + 0.5 - 8));
                int[] c = dist > 7.5 ? new int[]{150, 100, 52} : new int[]{201, 138, 69};
                bottom.setRGB(x, y, rgba(c));
            }
        }
        ImageIO.write(bottom, "png", dir.resolve("mooncake_bottom.png").toFile());

        BufferedImage inside = new BufferedImage(16, 16, BufferedImage.TYPE_INT_ARGB);
        Random rnd = new Random(42);
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int[] c = new int[]{242, 227, 194};
                if (rnd.nextInt(12) == 0) c = new int[]{224, 200, 160};
                inside.setRGB(x, y, rgba(c));
            }
        }
        ImageIO.write(inside, "png", dir.resolve("mooncake_inside.png").toFile());
        System.out.println("block textures written");
    }

    static int[][] palette(String base) {
        switch (base) {
            case "rusted": return new int[][]{{194, 112, 59}, {143, 77, 36}};
            case "weathered": return new int[][]{{169, 166, 90}, {124, 122, 60}};
            case "oxidized": return new int[][]{{95, 175, 158}, {62, 133, 120}};
            default: return new int[][]{{224, 164, 94}, {185, 127, 62}};
        }
    }

    static int[] mix(int[] a, int[] b, double t) {
        return new int[]{(int) (a[0] + (b[0] - a[0]) * t), (int) (a[1] + (b[1] - a[1]) * t), (int) (a[2] + (b[2] - a[2]) * t)};
    }

    static int rgba(int[] c) {
        return 0xFF000000 | (c[0] << 16) | (c[1] << 8) | c[2];
    }

    // ---------------- models ----------------

    static void genModels() throws IOException {
        Path dir = root.resolve("common/src/main/resources/assets/" + MOD + "/models/item");
        Files.createDirectories(dir);
        int n = 0;
        for (String flavor : FLAVORS) {
            for (String state : STATES) {
                String id = itemId(flavor, state);
                String json = "{\n"
                        + "  \"parent\": \"minecraft:item/generated\",\n"
                        + "  \"textures\": {\n"
                        + "    \"layer0\": \"" + MOD + ":item/" + id + "\"\n"
                        + "  }\n"
                        + "}\n";
                Files.write(dir.resolve(id + ".json"), json.getBytes(StandardCharsets.UTF_8));
                n++;
            }
        }
        System.out.println("models: " + n);
    }

    static void genWholeModel() throws IOException {
        Path dir = root.resolve("common/src/main/resources/assets/" + MOD + "/models/item");
        Files.createDirectories(dir);
        String json = "{\n"
                + "  \"parent\": \"minecraft:item/generated\",\n"
                + "  \"textures\": {\n"
                + "    \"layer0\": \"" + MOD + ":item/mooncake\"\n"
                + "  }\n"
                + "}\n";
        Files.write(dir.resolve("mooncake.json"), json.getBytes(StandardCharsets.UTF_8));
        System.out.println("whole item model written");
    }

    // ---------------- mooncake pie geometry ----------------
    // Wedge k is centred at 225 + 45k degrees (0 deg = +x = east, 90 deg = +z = south), so wedge 0
    // is the top-left (north-west) piece and the wedges run clockwise:
    // top-left, top, top-right, right, bottom-right, bottom, bottom-left, left.
    // That is exactly the crafting-ring order, so slice slot k is always wedge k, and the mooncake
    // is eaten piece by piece in that order. Every wedge is approximated by axis aligned boxes
    // (a staircase) so that the block models, the collision shapes and the renderer all agree.

    static final double PIE_CX = 8.0;
    static final double PIE_CZ = 8.0;
    static final double PIE_R = 7.0;
    static final double PIE_TOP = 8.0;
    static final double PIE_BAND = 1.0;
    static final double PIE_HALF = Math.toRadians(22.5);
    static final String[] WEDGE_NAMES = {"top-left", "top", "top-right", "right",
            "bottom-right", "bottom", "bottom-left", "left"};

    static double wedgeAngle(int k) {
        return Math.toRadians(225.0 + 45.0 * k);
    }

    static boolean inWedge(double px, double pz, double angle) {
        double dx = px - PIE_CX, dz = pz - PIE_CZ;
        if (dx * dx + dz * dz > PIE_R * PIE_R) return false;
        double diff = Math.atan2(dz, dx) - angle;
        while (diff > Math.PI) diff -= 2 * Math.PI;
        while (diff < -Math.PI) diff += 2 * Math.PI;
        return Math.abs(diff) <= PIE_HALF;
    }

    static double snap(double v) {
        return Math.round(v * 4.0) / 4.0;
    }

    /** Axis aligned boxes {x0, y0, z0, x1, y1, z1} in pixel units approximating one wedge. */
    static List<double[]> wedgeBoxes(int k) {
        double angle = wedgeAngle(k);
        double top = PIE_TOP - 0.02 * k; // per wedge offset, neighbouring tops must not z-fight
        boolean alongX = Math.abs(Math.cos(angle)) >= Math.abs(Math.sin(angle));
        double lo = Double.MAX_VALUE, hi = -Double.MAX_VALUE;
        for (int i = 0; i <= 180; i++) {
            double th = angle - PIE_HALF + 2 * PIE_HALF * i / 180.0;
            double v = alongX ? PIE_CX + PIE_R * Math.cos(th) : PIE_CZ + PIE_R * Math.sin(th);
            lo = Math.min(lo, v);
            hi = Math.max(hi, v);
        }
        // the sector is convex, so its extremes lie on the outer arc or in the centre point
        double centre = alongX ? PIE_CX : PIE_CZ;
        lo = Math.min(lo, centre);
        hi = Math.max(hi, centre);
        lo = snap(lo);
        hi = snap(hi);
        List<double[]> boxes = new ArrayList<>();
        for (double t = lo; t < hi - 1e-6; t += PIE_BAND) {
            double t1 = Math.min(t + PIE_BAND, hi);
            double oLo = Double.MAX_VALUE, oHi = -Double.MAX_VALUE;
            for (int edge = 0; edge < 2; edge++) {
                double u = edge == 0 ? t : t1;
                for (double v = 0.0; v <= 16.0; v += 0.125) {
                    double px = alongX ? u : v;
                    double pz = alongX ? v : u;
                    if (!inWedge(px, pz, angle)) continue;
                    oLo = Math.min(oLo, v);
                    oHi = Math.max(oHi, v);
                }
            }
            if (oLo > oHi) continue;
            if (alongX) {
                boxes.add(new double[]{t, 0.0, snap(oLo), t1, top, snap(oHi)});
            } else {
                boxes.add(new double[]{snap(oLo), 0.0, t, snap(oHi), top, t1});
            }
        }
        return boxes;
    }

    /** True when this face of the box looks into a wedge that has already been eaten. */
    static boolean faceIsCut(double[] b, int bites, double nx, double nz) {
        double px = (b[0] + b[3]) / 2.0 + nx * 0.3;
        double pz = (b[2] + b[5]) / 2.0 + nz * 0.3;
        for (int k = 0; k < bites; k++) {
            if (inWedge(px, pz, wedgeAngle(k))) return true;
        }
        return false;
    }

    /**
     * One model element. Every face carries an explicit uv derived from the box position, so the
     * many staircase boxes of a wedge line up into one continuous texture across the whole pie
     * instead of each little box repeating the entire sheet:
     * <ul>
     *   <li>up/down cover the 0..16 top view of the pie.</li>
     *   <li>the four sides map world x/z and y directly - the pie is 8px tall, so they sample the
     *       lower half (uv v 8..16) of the 64x64 side sheets, exactly where TextureGen paints
     *       the cross-section.</li>
     * </ul>
     */
    static String boxElement(double[] b, int bites) {
        double x0 = b[0], y0 = b[1], z0 = b[2], x1 = b[3], y1 = b[4], z1 = b[5];
        String north = faceIsCut(b, bites, 0, -1) ? "#inside" : "#side";
        String south = faceIsCut(b, bites, 0, 1) ? "#inside" : "#side";
        String west = faceIsCut(b, bites, -1, 0) ? "#inside" : "#side";
        String east = faceIsCut(b, bites, 1, 0) ? "#inside" : "#side";
        return "    { \"from\": [" + fmt(x0) + ", " + fmt(y0) + ", " + fmt(z0) + "], "
                + "\"to\": [" + fmt(x1) + ", " + fmt(y1) + ", " + fmt(z1) + "],\n"
                + "      \"faces\": {\n"
                + face("down", "#bottom", x0, 16 - z1, x1, 16 - z0) + ",\n"
                + face("up", "#top", x0, z0, x1, z1) + ",\n"
                + face("north", north, x0, 16 - y1, x1, 16 - y0) + ",\n"
                + face("south", south, 16 - x1, 16 - y1, 16 - x0, 16 - y0) + ",\n"
                + face("west", west, z0, 16 - y1, z1, 16 - y0) + ",\n"
                + face("east", east, 16 - z1, 16 - y1, 16 - z0, 16 - y0) + "\n"
                + "      }\n    }";
    }

    /** A single model face with an explicit uv rectangle (uv units are 0..16, v grows downwards). */
    static String face(String name, String texture, double u1, double v1, double u2, double v2) {
        return "        \"" + name + "\": { \"texture\": \"" + texture + "\", \"uv\": ["
                + fmt(u1) + ", " + fmt(v1) + ", " + fmt(u2) + ", " + fmt(v2) + "] }";
    }

    /** 8 pie models: model n keeps wedges n..7, so the mooncake is eaten piece by piece. */
    static void genBlockModels() throws IOException {
        Path models = root.resolve("common/src/main/resources/assets/" + MOD + "/models/block");
        Path states = root.resolve("common/src/main/resources/assets/" + MOD + "/blockstates");
        Files.createDirectories(models);
        Files.createDirectories(states);
        StringBuilder sb = new StringBuilder("{\n  \"variants\": {\n");
        for (int bites = 0; bites < 8; bites++) {
            StringBuilder elements = new StringBuilder();
            for (int k = bites; k < 8; k++) {
                for (double[] b : wedgeBoxes(k)) {
                    if (elements.length() > 0) elements.append(",\n");
                    elements.append(boxElement(b, bites));
                }
            }
            String json = "{\n"
                    + "  \"textures\": {\n"
                    + "    \"particle\": \"" + MOD + ":block/mooncake_side\",\n"
                    + "    \"bottom\": \"" + MOD + ":block/mooncake_bottom\",\n"
                    + "    \"top\": \"" + MOD + ":block/mooncake_top\",\n"
                    + "    \"side\": \"" + MOD + ":block/mooncake_side\",\n"
                    + "    \"inside\": \"" + MOD + ":block/mooncake_inside\"\n"
                    + "  },\n"
                    + "  \"elements\": [\n" + elements + "\n  ]\n}\n";
            Files.write(models.resolve("mooncake_bite" + bites + ".json"), json.getBytes(StandardCharsets.UTF_8));
            sb.append("    \"bites=").append(bites).append("\": { \"model\": \"").append(MOD).append(":block/mooncake_bite").append(bites).append("\" }");
            if (bites < 7) sb.append(",");
            sb.append("\n");
        }
        sb.append("  }\n}\n");
        Files.write(states.resolve("mooncake_block.json"), sb.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("block models: 8 pie models + blockstate");
    }

    /** Emits the Java geometry table shared by the block shapes and the block entity renderer. */
    static void genGeometry() throws IOException {
        Path file = root.resolve("common/src/main/java/com/moremooncake/mooncake/block/MooncakeGeometry.java");
        Files.createDirectories(file.getParent());
        StringBuilder sb = new StringBuilder();
        sb.append("package com.moremooncake.mooncake.block;\n\n");
        sb.append("/**\n");
        sb.append(" * GENERATED by tools/ResourceGen.java - do not edit by hand.\n");
        sb.append(" * <p>\n");
        sb.append(" * The grand mooncake is a pie of 8 wedges. Wedge 0 is the top-left (north-west) piece and\n");
        sb.append(" * the wedges run clockwise (top-left, top, top-right, right, bottom-right, bottom,\n");
        sb.append(" * bottom-left, left), which is exactly the crafting-ring order - so slice slot k is wedge k\n");
        sb.append(" * and the mooncake is eaten piece by piece in that order.\n");
        sb.append(" * <p>\n");
        sb.append(" * Each wedge is approximated by axis aligned boxes so that the block models, the collision\n");
        sb.append(" * shapes and the block entity renderer all use the very same geometry.\n");
        sb.append(" */\n");
        sb.append("public final class MooncakeGeometry {\n");
        sb.append("    /** Boxes of every wedge as {x0, y0, z0, x1, y1, z1} in 1/16 block units. */\n");
        sb.append("    public static final double[][][] WEDGE_BOXES = {\n");
        for (int k = 0; k < 8; k++) {
            sb.append("            { // wedge ").append(k).append(" - ").append(WEDGE_NAMES[k]).append("\n");
            for (double[] b : wedgeBoxes(k)) {
                sb.append("                    {")
                        .append(num(b[0])).append(", ").append(num(b[1])).append(", ").append(num(b[2])).append(", ")
                        .append(num(b[3])).append(", ").append(num(b[4])).append(", ").append(num(b[5]))
                        .append("},\n");
            }
            sb.append("            },\n");
        }
        sb.append("    };\n\n    private MooncakeGeometry() {\n    }\n}\n");
        Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
        System.out.println("geometry table written (MooncakeGeometry.java)");
    }

    static String num(double v) {
        return String.format(java.util.Locale.ROOT, "%.2f", v);
    }

    // ---------------- lang ----------------

    static void genLang() throws IOException {
        Path dir = root.resolve("common/src/main/resources/assets/" + MOD + "/lang");
        Files.createDirectories(dir);
        String[] flavorEn = {"Five-Kernel", "Red-Bean", "Perilla-Seed", "Red-Date", "Salted-Egg-Yolk"};
        String[] flavorZh = {"五仁", "豆沙", "苏子", "红枣", "咸蛋黄"};
        String[] stateEnPre = {"", "Rusted ", "Weathered ", "Oxidized ", "Waxed ", "Waxed Rusted ", "Waxed Weathered ", "Waxed Oxidized "};
        String[] stateZhPre = {"", "锈蚀的", "斑驳的", "氧化的", "涂蜡的", "涂蜡的锈蚀的", "涂蜡的斑驳的", "涂蜡的氧化的"};

        Map<String, String> en = new LinkedHashMap<>();
        Map<String, String> zh = new LinkedHashMap<>();
        en.put("itemGroup." + MOD, "More Mooncake");
        zh.put("itemGroup." + MOD, "更多月饼");
        for (int i = 0; i < FLAVORS.length; i++) {
            for (int j = 0; j < STATES.length; j++) {
                String id = itemId(FLAVORS[i], STATES[j]);
                en.put("item." + MOD + "." + id, stateEnPre[j] + flavorEn[i] + " Mooncake Slice");
                zh.put("item." + MOD + "." + id, stateZhPre[j] + flavorZh[i] + "月饼块");
            }
        }
        en.put("item." + MOD + ".mooncake", "Grand Mooncake");
        zh.put("item." + MOD + ".mooncake", "大月饼");
        en.put("item." + MOD + ".mooncake_full", "Grand Mooncake: %s");
        zh.put("item." + MOD + ".mooncake_full", "大月饼：%s");
        en.put("block." + MOD + ".mooncake_block", "Grand Mooncake");
        zh.put("block." + MOD + ".mooncake_block", "大月饼");

        en.put("tooltip." + MOD + ".festival", "Happy Mid-Autumn Festival!");
        zh.put("tooltip." + MOD + ".festival", "中秋快乐！");
        en.put("tooltip." + MOD + ".slice_hint", "A slice: craft 8 slices + 1 egg into a Grand Mooncake");
        zh.put("tooltip." + MOD + ".slice_hint", "一块月饼：工作台中间放鸡蛋、周围放 8 块可拼成大月饼");
        en.put("tooltip." + MOD + ".waxed", "Waxed: effects last twice as long, will not oxidize");
        zh.put("tooltip." + MOD + ".waxed", "已涂蜡：效果持续时间翻倍，不会再氧化");
        en.put("tooltip." + MOD + ".oxidized_side_effect", "Warning: oxidized mooncake has side effects!");
        zh.put("tooltip." + MOD + ".oxidized_side_effect", "警告：氧化月饼食用后有副作用！");
        en.put("tooltip." + MOD + ".grand_place", "Place it, then right-click to eat slice by slice - each bite applies that slice's effect");
        zh.put("tooltip." + MOD + ".grand_place", "右键地面放置；放置后右键食用，每口触发对应小块的效果（吃完不返还小块）");
        en.put("tooltip." + MOD + ".grand_cut", "Right-click it with an axe to cut it open and get the remaining slices back");
        zh.put("tooltip." + MOD + ".grand_cut", "用斧头右键切开：返还剩下的小块");
        // JEI category titles
        en.put("jei." + MOD + ".assembly", "Assemble a Grand Mooncake");
        zh.put("jei." + MOD + ".assembly", "拼装大月饼（8 块 + 鸡蛋）");
        en.put("jei." + MOD + ".scrape", "Scrape Wax & Rust");
        zh.put("jei." + MOD + ".scrape", "刮蜡与除锈（斧头不消耗）");

        writeJson(en, dir.resolve("en_us.json"));
        writeJson(zh, dir.resolve("zh_cn.json"));
        System.out.println("lang entries: " + en.size() + " / " + zh.size());
    }

    static void writeJson(Map<String, String> map, Path file) throws IOException {
        StringBuilder sb = new StringBuilder("{\n");
        int i = 0;
        for (Map.Entry<String, String> e : map.entrySet()) {
            sb.append("  \"").append(esc(e.getKey())).append("\": \"").append(esc(e.getValue())).append("\"");
            if (++i < map.size()) sb.append(",");
            sb.append("\n");
        }
        sb.append("}\n");
        Files.write(file, sb.toString().getBytes(StandardCharsets.UTF_8));
    }

    static String esc(String s) {
        return s.replace("\\", "\\\\").replace("\"", "\\\"");
    }

    // ---------------- recipes ----------------

    static void genRecipes() throws IOException {
        // NOTE: since 1.21 the data pack directory is the singular "recipe"
        // (it was "recipes" up to 1.20.x). Using the old name makes the game
        // silently ignore every recipe in it.
        Path dir = root.resolve("common/src/main/resources/data/" + MOD + "/recipe");
        Files.createDirectories(dir);
        // Wipe the directory first so recipes that no longer exist are not left behind
        // and loaded stale by the game.
        try (var stream = Files.list(dir)) {
            for (Path p : stream.filter(p -> p.getFileName().toString().endsWith(".json")).toList()) {
                Files.deleteIfExists(p);
            }
        }
        int n = 0;

        // 1) Survival access to the slices: bake a fresh slice from wheat plus a
        //    flavour ingredient. Oxidised/waxed variants come from the recipes below.
        String[][] sliceIngredient = {
                {"wuren", "minecraft:pumpkin_seeds"},
                {"dousha", "minecraft:cocoa_beans"},
                {"suzi", "minecraft:beetroot"},
                {"hongzao", "minecraft:sweet_berries"},
                {"xianyadan", "minecraft:egg"},
        };
        for (String[] step : sliceIngredient) {
            writeShapeless(dir.resolve("slice_" + step[0] + ".json"),
                    List.of("{\"item\": \"minecraft:wheat\"}", "{\"item\": \"" + step[1] + "\"}"),
                    itemId(step[0], ""), 1, "more_mooncake:slice");
            n++;
        }

        // 2) Oxidation / preservation chain per flavour.
        for (String flavor : FLAVORS) {
            // wax: fresh/rusted/weathered/oxidized + honeycomb -> waxed variant
            for (int i = 0; i < 4; i++) {
                String base = itemId(flavor, UNWAXED[i]);
                String waxedId = itemId(flavor, WAXED[i]);
                writeShapeless(dir.resolve("wax_" + base + ".json"),
                        List.of("{\"item\": \"" + MOD + ":" + base + "\"}", "{\"item\": \"minecraft:honeycomb\"}"),
                        waxedId, 1, "more_mooncake:wax");
                n++;
            }
            // oxidize forward: copper-family items step the oxidation tier up
            String[][] oxidizeSteps = {
                    {"", "rusted", "minecraft:iron_nugget"},
                    {"rusted", "weathered", "minecraft:copper_ingot"},
                    {"weathered", "oxidized", "minecraft:copper_block"},
            };
            for (String[] step : oxidizeSteps) {
                writeShapeless(dir.resolve("oxidize_" + itemId(flavor, step[0]) + ".json"),
                        List.of("{\"item\": \"" + MOD + ":" + itemId(flavor, step[0]) + "\"}", "{\"item\": \"" + step[2] + "\"}"),
                        itemId(flavor, step[1]), 1, "more_mooncake:oxidize");
                n++;
            }
        }
        // Scraping (wax off / rust off) is one custom tool recipe for every slice:
        // the axe keeps 1 durability instead of being consumed. No data recipes needed.
        String scrape = "{\n"
                + "  \"type\": \"" + MOD + ":mooncake_scrape\",\n"
                + "  \"category\": \"misc\"\n"
                + "}\n";
        Files.write(dir.resolve("mooncake_scrape.json"), scrape.getBytes(StandardCharsets.UTF_8));
        n++;

        // 3) Pure grand mooncakes: 8 identical slices around an egg. Ordinary shaped
        //    recipes so they appear in the recipe book; the slice list travels as
        //    custom data, exactly like the custom assembly recipe.
        for (String flavor : FLAVORS) {
            for (String state : STATES) {
                String slice = itemId(flavor, state);
                List<String> slices = new ArrayList<>();
                for (int k = 0; k < 8; k++) {
                    slices.add(MOD + ":" + slice);
                }
                writeGrand(dir.resolve("grand_" + slice + ".json"), slice, slices);
                n++;
            }
        }

        // 4) Custom assembly: any 8 slices around an egg (assorted or mixed states).
        String assembly = "{\n"
                + "  \"type\": \"" + MOD + ":mooncake_assembly\",\n"
                + "  \"category\": \"misc\"\n"
                + "}\n";
        Files.write(dir.resolve("mooncake_assembly.json"), assembly.getBytes(StandardCharsets.UTF_8));
        n++;
        System.out.println("recipes: " + n);
    }

    /** Shapeless recipe with an optional recipe-book group. */
    static void writeShapeless(Path file, List<String> ingredientObjects, String resultId, int count, String group) throws IOException {
        String json = "{\n"
                + "  \"type\": \"minecraft:crafting_shapeless\",\n"
                + "  \"category\": \"misc\"";
        if (group != null) {
            json += ",\n  \"group\": \"" + group + "\"";
        }
        json += ",\n  \"ingredients\": [\n"
                + "    " + String.join(",\n    ", ingredientObjects) + "\n"
                + "  ],\n"
                + "  \"result\": {\n"
                + "    \"id\": \"" + MOD + ":" + resultId + "\",\n"
                + "    \"count\": " + count + "\n"
                + "  }\n"
                + "}\n";
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    /** Shaped 8-slices-around-an-egg recipe for a pure grand mooncake. */
    static void writeGrand(Path file, String sliceId, List<String> slices8) throws IOException {
        StringBuilder slicesJson = new StringBuilder();
        for (int i = 0; i < slices8.size(); i++) {
            if (i > 0) slicesJson.append(", ");
            slicesJson.append("\"").append(slices8.get(i)).append("\"");
        }
        String json = "{\n"
                + "  \"type\": \"minecraft:crafting_shaped\",\n"
                + "  \"group\": \"more_mooncake:grand\",\n"
                + "  \"category\": \"misc\",\n"
                + "  \"pattern\": [\"xxx\", \"xex\", \"xxx\"],\n"
                + "  \"key\": {\n"
                + "    \"x\": { \"item\": \"" + MOD + ":" + sliceId + "\" },\n"
                + "    \"e\": { \"item\": \"minecraft:egg\" }\n"
                + "  },\n"
                + "  \"result\": {\n"
                + "    \"id\": \"" + MOD + ":mooncake\",\n"
                + "    \"count\": 1,\n"
                + "    \"components\": {\n"
                + "      \"minecraft:custom_data\": {\n"
                + "        \"Slices\": [ " + slicesJson + " ]\n"
                + "      }\n"
                + "    }\n"
                + "  }\n"
                + "}\n";
        Files.write(file, json.getBytes(StandardCharsets.UTF_8));
    }

    // ---------------- mod icons ----------------

    static void genIcon() throws IOException {
        BufferedImage img = new BufferedImage(128, 128, BufferedImage.TYPE_INT_ARGB);
        int[] crust = {224, 164, 94}, edge = {185, 127, 62};
        int[][] seeds = {{64, 58}, {64, 70}, {58, 64}, {70, 64}, {64, 64}};
        for (int y = 0; y < 128; y++) {
            for (int x = 0; x < 128; x++) {
                double dx = x + 0.5 - 64, dy = y + 0.5 - 64;
                double dist = Math.sqrt(dx * dx + dy * dy);
                if (dist > 56) continue;
                int[] col = dist > 46 ? edge : crust;
                if (x >= 52 && x <= 75 && y >= 52 && y <= 75) {
                    col = (x == 52 || x == 75 || y == 52 || y == 75) ? new int[]{110, 30, 30} : new int[]{146, 44, 44};
                    for (int[] s : seeds) {
                        if (x >= s[0] - 3 && x <= s[0] + 2 && y >= s[1] - 3 && y <= s[1] + 2) {
                            col = (x == s[0] - 3 || x == s[0] + 2 || y == s[1] - 3 || y == s[1] + 2)
                                    ? new int[]{91, 58, 30} : new int[]{240, 230, 206};
                            break;
                        }
                    }
                }
                if (x + y >= 96 && x + y <= 128) col = mix(col, new int[]{255, 255, 255}, 0.25);
                img.setRGB(x, y, rgba(col));
            }
        }
        Files.createDirectories(root.resolve("fabric/src/main/resources"));
        Files.createDirectories(root.resolve("neoforge/src/main/resources"));
        ImageIO.write(img, "png", root.resolve("fabric/src/main/resources/pack.png").toFile());
        ImageIO.write(img, "png", root.resolve("neoforge/src/main/resources/pack.png").toFile());
        System.out.println("icons written");
    }
}
