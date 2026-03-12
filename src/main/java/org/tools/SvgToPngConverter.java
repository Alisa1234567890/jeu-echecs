package org.tools;

import java.awt.image.BufferedImage;
import java.io.InputStream;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;

public class SvgToPngConverter {

    /**
     * Tries to convert SVG from InputStream into a BufferedImage of given width/height using Batik via reflection.
     * Returns null if Batik is not available or conversion fails.
     */
    public static BufferedImage loadSvgAsImage(InputStream svgStream, float width, float height) {
        try {
            // Load classes via reflection
            Class<?> transcoderClass = Class.forName("org.apache.batik.transcoder.image.ImageTranscoder");
            Class<?> transcoderInputClass = Class.forName("org.apache.batik.transcoder.TranscoderInput");
            Class<?> transcoderOutputClass = Class.forName("org.apache.batik.transcoder.TranscoderOutput");
            Class<?> transcoderExceptionClass = Class.forName("org.apache.batik.transcoder.TranscoderException");

            // Create an anonymous subclass of ImageTranscoder via runtime proxy isn't trivial; instead use Batik's PNGTranscoder if available
            Class<?> pngTranscoderClass = null;
            try {
                pngTranscoderClass = Class.forName("org.apache.batik.transcoder.image.PNGTranscoder");
            } catch (ClassNotFoundException ex) {
                System.err.println("SvgToPngConverter: PNGTranscoder not found: " + ex.getMessage());
            }

            Object transcoder;
            if (pngTranscoderClass != null) {
                transcoder = pngTranscoderClass.getDeclaredConstructor().newInstance();
            } else {
                System.err.println("SvgToPngConverter: Cannot create transcoder - PNGTranscoder class not available");
                return null;
            }

            // set hints for width/height
            try {
                Class<?> imageTranscoderClass = Class.forName("org.apache.batik.transcoder.image.ImageTranscoder");
                java.lang.reflect.Field keyWidth = imageTranscoderClass.getField("KEY_WIDTH");
                java.lang.reflect.Field keyHeight = imageTranscoderClass.getField("KEY_HEIGHT");
                Object K_WIDTH = keyWidth.get(null);
                Object K_HEIGHT = keyHeight.get(null);
                Method addHint = transcoder.getClass().getMethod("addTranscodingHint", Object.class, Object.class);
                if (width > 0) addHint.invoke(transcoder, K_WIDTH, (int)width);
                if (height > 0) addHint.invoke(transcoder, K_HEIGHT, (int)height);
                System.out.println("SvgToPngConverter: Set width/height hints to " + (int)width + "x" + (int)height);
            } catch (Exception nsf) {
                System.err.println("SvgToPngConverter: Warning - could not set width/height hints: " + nsf.getMessage());
            }

            // prepare TranscoderInput from InputStream
            Constructor<?> inputCtor = transcoderInputClass.getConstructor(InputStream.class);
            Object input = inputCtor.newInstance(svgStream);

            // use a ByteArrayOutputStream via TranscoderOutput to capture image bytes, but we want a BufferedImage directly.
            // Simpler approach: use PNGTranscoder to write to a java.io.ByteArrayOutputStream and read back as BufferedImage via ImageIO
            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            Constructor<?> outputCtor = transcoderOutputClass.getConstructor(java.io.OutputStream.class);
            Object output = outputCtor.newInstance(baos);

            Method transcodeMethod = transcoder.getClass().getMethod("transcode", transcoderInputClass, transcoderOutputClass);
            transcodeMethod.invoke(transcoder, input, output);

            byte[] pngBytes = baos.toByteArray();
            if (pngBytes.length == 0) {
                System.err.println("SvgToPngConverter: Transcoding produced empty output");
                return null;
            }
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(pngBytes);
            BufferedImage result = javax.imageio.ImageIO.read(bais);
            if (result != null) {
                System.out.println("SvgToPngConverter: Successfully converted SVG to BufferedImage (" + result.getWidth() + "x" + result.getHeight() + ")");
            }
            return result;

        } catch (ClassNotFoundException e) {
            System.err.println("SvgToPngConverter: Batik not available - ClassNotFoundException: " + e.getMessage());
            return null;
        } catch (Exception e) {
            System.err.println("SvgToPngConverter: Error converting SVG: " + e.getClass().getSimpleName() + ": " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }
}



