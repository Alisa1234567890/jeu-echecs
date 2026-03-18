package org.tools;

import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.TranscoderOutput;
import org.apache.batik.transcoder.image.ImageTranscoder;
import org.apache.batik.transcoder.image.PNGTranscoder;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

import javax.imageio.ImageIO;

public class SvgToPngConverter {

    /**
     * Converts SVG from InputStream into a BufferedImage of the requested size using Batik.
     * Returns null if Batik is not available or conversion fails.
     */
    public static BufferedImage loadSvgAsImage(InputStream svgStream, float width, float height) {
        try {
            PNGTranscoder transcoder = new PNGTranscoder();
            if (width > 0) {
                transcoder.addTranscodingHint(ImageTranscoder.KEY_WIDTH, width);
            }
            if (height > 0) {
                transcoder.addTranscodingHint(ImageTranscoder.KEY_HEIGHT, height);
            }

            TranscoderInput input = new TranscoderInput(svgStream);
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            TranscoderOutput output = new TranscoderOutput(baos);
            transcoder.transcode(input, output);

            byte[] pngBytes = baos.toByteArray();
            if (pngBytes.length == 0) {
                System.err.println("SvgToPngConverter: Transcoding produced empty output");
                return null;
            }
            ByteArrayInputStream bais = new ByteArrayInputStream(pngBytes);
            BufferedImage result = ImageIO.read(bais);
            if (result != null) {
                System.out.println("SvgToPngConverter: Successfully converted SVG to BufferedImage (" + result.getWidth() + "x" + result.getHeight() + ")");
            }
            return result;

        } catch (Exception e) {
            System.err.println("SvgToPngConverter: Error converting SVG: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}



