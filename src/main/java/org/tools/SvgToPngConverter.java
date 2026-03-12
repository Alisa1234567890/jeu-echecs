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
        if (svgStream == null) return null;
        try {
            // Load classes via reflection
            Class<?> transcoderClass = Class.forName("org.apache.batik.transcoder.image.ImageTranscoder");
            Class<?> transcoderInputClass = Class.forName("org.apache.batik.transcoder.TranscoderInput");
            Class<?> transcoderOutputClass = Class.forName("org.apache.batik.transcoder.TranscoderOutput");

            // Use Batik's PNGTranscoder
            Class<?> pngTranscoderClass = Class.forName("org.apache.batik.transcoder.image.PNGTranscoder");
            Object transcoder = pngTranscoderClass.getDeclaredConstructor().newInstance();

            // set hints for width/height if available
            try {
                Class<?> imageTranscoderClass = Class.forName("org.apache.batik.transcoder.image.ImageTranscoder");
                java.lang.reflect.Field keyWidth = imageTranscoderClass.getField("KEY_WIDTH");
                java.lang.reflect.Field keyHeight = imageTranscoderClass.getField("KEY_HEIGHT");
                Object K_WIDTH = keyWidth.get(null);
                Object K_HEIGHT = keyHeight.get(null);
                Method addHint = transcoder.getClass().getMethod("addTranscodingHint", Object.class, Object.class);
                if (width > 0) addHint.invoke(transcoder, K_WIDTH, width);
                if (height > 0) addHint.invoke(transcoder, K_HEIGHT, height);
            } catch (NoSuchFieldException nsf) {
                // ignore if keys not available
            }

            // prepare TranscoderInput from InputStream
            Constructor<?> inputCtor = transcoderInputClass.getConstructor(InputStream.class);
            Object input = inputCtor.newInstance(svgStream);

            java.io.ByteArrayOutputStream baos = new java.io.ByteArrayOutputStream();
            Constructor<?> outputCtor = transcoderOutputClass.getConstructor(java.io.OutputStream.class);
            Object output = outputCtor.newInstance(baos);

            Method transcodeMethod = transcoder.getClass().getMethod("transcode", transcoderInputClass, transcoderOutputClass);
            transcodeMethod.invoke(transcoder, input, output);

            byte[] pngBytes = baos.toByteArray();
            java.io.ByteArrayInputStream bais = new java.io.ByteArrayInputStream(pngBytes);
            BufferedImage img = javax.imageio.ImageIO.read(bais);
            try { bais.close(); } catch (Exception ignored) {}
            try { baos.close(); } catch (Exception ignored) {}
            return img;

        } catch (ClassNotFoundException e) {
            System.err.println("SvgToPngConverter: Batik not found on classpath: " + e.getMessage());
            e.printStackTrace();
            return null;
        } catch (Exception e) {
            System.err.println("SvgToPngConverter: conversion failed: " + e.getMessage());
            e.printStackTrace();
            return null;
        } finally {
            try { svgStream.close(); } catch (Exception ignored) {}
        }
    }
}
