package packaging;

import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.image.BufferedImage;
import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import javax.imageio.ImageIO;

public class IconToIco {
    public static void main(String[] args) throws Exception {
        File pngFile = new File("packaging/assets/USBSanitizer.png");
        BufferedImage src = ImageIO.read(pngFile);

        int[] sizes = {16, 32, 48, 64, 128, 256};
        List<byte[]> pngBytesList = new ArrayList<>();

        for (int sz : sizes) {
            BufferedImage scaled = new BufferedImage(sz, sz, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g2 = scaled.createGraphics();
            g2.drawImage(src.getScaledInstance(sz, sz, Image.SCALE_SMOOTH), 0, 0, null);
            g2.dispose();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            ImageIO.write(scaled, "PNG", baos);
            pngBytesList.add(baos.toByteArray());
        }

        File icoFile = new File("packaging/assets/USBSanitizer.ico");
        try (FileOutputStream fos = new FileOutputStream(icoFile)) {
            // ICONDIR header: 6 bytes (Reserved=0, Type=1 for ICO, Count=sizes.length)
            ByteBuffer dir = ByteBuffer.allocate(6).order(ByteOrder.LITTLE_ENDIAN);
            dir.putShort((short) 0);
            dir.putShort((short) 1);
            dir.putShort((short) sizes.length);
            fos.write(dir.array());

            int offset = 6 + (sizes.length * 16);

            for (int i = 0; i < sizes.length; i++) {
                int sz = sizes[i];
                byte[] data = pngBytesList.get(i);

                ByteBuffer entry = ByteBuffer.allocate(16).order(ByteOrder.LITTLE_ENDIAN);
                entry.put((byte) (sz >= 256 ? 0 : sz)); // Width
                entry.put((byte) (sz >= 256 ? 0 : sz)); // Height
                entry.put((byte) 0); // Color count
                entry.put((byte) 0); // Reserved
                entry.putShort((short) 1); // Color planes
                entry.putShort((short) 32); // Bits per pixel
                entry.putInt(data.length); // Image size in bytes
                entry.putInt(offset); // Image offset

                fos.write(entry.array());
                offset += data.length;
            }

            for (byte[] data : pngBytesList) {
                fos.write(data);
            }
        }

        System.out.println("Generated ICO: " + icoFile.getAbsolutePath());
    }
}
