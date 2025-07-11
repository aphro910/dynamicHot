package com.kuma.tools.dynamicHotCompute.utils;

import org.xerial.snappy.Snappy;

import java.io.IOException;

public class CompressUtil {
    public static byte[] compress(String data) throws IOException {
        return Snappy.compress(data);
    }

    public static byte[] compress(byte[] data) throws IOException {
        return Snappy.compress(data);
    }

    public static String decompress(byte[] compressedData) throws IOException {
        return new String(Snappy.uncompress(compressedData));
    }

    public static byte[] decompressToByteArray(byte[] compressedData) throws IOException {
        return Snappy.uncompress(compressedData);
    }
}
