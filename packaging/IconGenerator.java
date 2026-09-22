package packaging;

import java.awt.*;
import java.awt.geom.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

public class IconGenerator {
    public static void main(String[] args) throws Exception {
        int size = 1024;
        BufferedImage image = new BufferedImage(size, size, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g2 = image.createGraphics();

        g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);

        // Background squircle / rounded rect
        RoundRectangle2D background = new RoundRectangle2D.Float(64, 64, size - 128, size - 128, 220, 220);
        
        // Gradient fill: Deep Slate to Cyber Blue
        GradientPaint bgGradient = new GradientPaint(
            100, 100, new Color(15, 23, 42),
            900, 900, new Color(30, 41, 59)
        );
        g2.setPaint(bgGradient);
        g2.fill(background);

        // Outer glow border
        g2.setStroke(new BasicStroke(16f));
        g2.setPaint(new GradientPaint(
            100, 100, new Color(56, 189, 248, 200),
            900, 900, new Color(168, 85, 247, 180)
        ));
        g2.draw(background);

        // Security Shield shape in center
        Path2D shield = new Path2D.Float();
        shield.moveTo(512, 220);
        shield.curveTo(620, 220, 720, 260, 750, 310);
        shield.curveTo(750, 560, 640, 720, 512, 800);
        shield.curveTo(384, 720, 274, 560, 274, 310);
        shield.curveTo(304, 260, 404, 220, 512, 220);
        shield.closePath();

        GradientPaint shieldGradient = new GradientPaint(
            300, 300, new Color(14, 165, 233, 220),
            700, 700, new Color(99, 102, 241, 240)
        );
        g2.setPaint(shieldGradient);
        g2.fill(shield);

        g2.setStroke(new BasicStroke(12f));
        g2.setColor(new Color(224, 242, 254));
        g2.draw(shield);

        // USB Trident & Checkmark Symbol in Center
        // Center checkmark / sanitization spark
        Path2D check = new Path2D.Float();
        check.moveTo(420, 520);
        check.lineTo(480, 580);
        check.lineTo(610, 430);

        g2.setStroke(new BasicStroke(28f, BasicStroke.CAP_ROUND, BasicStroke.JOIN_ROUND));
        g2.setColor(new Color(240, 253, 250));
        g2.draw(check);

        // USB Connector icon at bottom center of shield
        g2.setColor(new Color(255, 255, 255, 230));
        g2.fillRoundRect(472, 630, 80, 70, 16, 16);
        g2.setColor(new Color(15, 23, 42));
        g2.fillRect(487, 650, 16, 25);
        g2.fillRect(521, 650, 16, 25);

        g2.dispose();

        File outDir = new File(args.length > 0 ? args[0] : "packaging/assets");
        outDir.mkdirs();
        File outFile = new File(outDir, "USBSanitizer.png");
        ImageIO.write(image, "PNG", outFile);
        System.out.println("Generated icon: " + outFile.getAbsolutePath());
    }
}
