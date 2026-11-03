package com.masquerade817;

import javax.imageio.ImageIO;
import java.awt.Color;
import java.awt.GradientPaint;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.Polygon;
import java.awt.RenderingHints;
import java.awt.geom.RoundRectangle2D;
import java.awt.image.BufferedImage;

public class AppIconUtil {

    private static final String ICON_RESOURCE_PATH = "/icon.png";
    private static Image cachedIcon = null;

    /**
     * Retrieves the application icon. If the resource is not found,
     * dynamically renders a crisp modern desktop wallpaper icon.
     */
    public static synchronized Image getAppIcon(int size) {
        if (cachedIcon != null && cachedIcon.getWidth(null) == size) {
            return cachedIcon;
        }

        try (var in = AppIconUtil.class.getResourceAsStream(ICON_RESOURCE_PATH)) {
            if (in != null) {
                var img = ImageIO.read(in);
                if (img != null) {
                    cachedIcon = img.getScaledInstance(size, size, Image.SCALE_SMOOTH);
                    return cachedIcon;
                }
            }
        } catch (Exception ignored) {
        }

        cachedIcon = generateMonitorIcon(size);
        return cachedIcon;
    }

    /**
     * Programmatically generates a high-quality monitor with wallpaper illustration.
     */
    public static BufferedImage generateMonitorIcon(int size) {
        var image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = image.createGraphics();

        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);

        float s = size / 32.0f;

        // Monitor Bezel Outer
        g.setColor(new Color(30, 35, 45));
        g.fill(new RoundRectangle2D.Float(2 * s, 2 * s, 28 * s, 19 * s, 4 * s, 4 * s));

        // Screen area (sky gradient)
        int screenX = Math.round(4 * s);
        int screenY = Math.round(4 * s);
        int screenW = Math.round(24 * s);
        int screenH = Math.round(15 * s);

        var skyGradient = new GradientPaint(
                screenX, screenY, new Color(41, 128, 185),
                screenX, screenY + screenH, new Color(109, 213, 250)
        );
        g.setPaint(skyGradient);
        g.fillRect(screenX, screenY, screenW, screenH);

        // Sun
        g.setColor(new Color(255, 230, 100, 230));
        g.fillOval(Math.round(21 * s), Math.round(6 * s), Math.round(4 * s), Math.round(4 * s));

        // Mountains on screen
        var m1 = new Polygon();
        m1.addPoint(Math.round(4 * s), Math.round(19 * s));
        m1.addPoint(Math.round(13 * s), Math.round(10 * s));
        m1.addPoint(Math.round(21 * s), Math.round(19 * s));
        g.setColor(new Color(39, 174, 96));
        g.fillPolygon(m1);

        var m2 = new Polygon();
        m2.addPoint(Math.round(14 * s), Math.round(19 * s));
        m2.addPoint(Math.round(22 * s), Math.round(12 * s));
        m2.addPoint(Math.round(28 * s), Math.round(19 * s));
        g.setColor(new Color(46, 204, 113));
        g.fillPolygon(m2);

        // Monitor Stand Neck
        g.setColor(new Color(52, 73, 94));
        g.fillRect(Math.round(14 * s), Math.round(21 * s), Math.round(4 * s), Math.round(5 * s));

        // Monitor Stand Base
        g.setColor(new Color(44, 62, 80));
        g.fill(new RoundRectangle2D.Float(10 * s, 26 * s, 12 * s, 3 * s, 2 * s, 2 * s));

        g.dispose();
        return image;
    }
}
