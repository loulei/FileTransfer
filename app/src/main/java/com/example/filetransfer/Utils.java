package com.example.filetransfer;

import android.content.Intent;

public class Utils {

    public static int byteArrayToInt(byte[] b) {
        return   b[3] & 0xFF |
                (b[2] & 0xFF) << 8 |
                (b[1] & 0xFF) << 16 |
                (b[0] & 0xFF) << 24;
    }
    public static byte[] intToByteArray(int a) {
        return new byte[] {
                (byte) ((a >> 24) & 0xFF),
                (byte) ((a >> 16) & 0xFF),
                (byte) ((a >> 8) & 0xFF),
                (byte) (a & 0xFF)
        };
    }

    public static long byteArrayToLong(byte[] b) {
        return b[0] & 0xFF | (b[1] & 0xFF) << 8 | (b[2] & 0xFF) << 16 | (b[3] & 0xFF) << 24 | (b[4] & 0xFF) << 32 | (b[5] & 0xFF) << 40 | (b[6] & 0xFF) << 48 | (b[7] & 0xFF) << 56;
    }

    public static byte[] longToByteArray(long a) {
        return new byte[] {(byte) (a & 0xFF), (byte) ((a >> 8) & 0xFF), (byte) ((a >> 16) & 0xFF),  (byte) ((a >> 24) & 0xFF), (byte) ((a >> 32) & 0xFF), (byte) ((a >> 40) & 0xFF), (byte) ((a >> 48) & 0xFF), (byte) ((a >> 56) & 0xFF)};
    }

    public static String bytesToHexString(byte[] b) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < b.length; i++) {
            String hex = Integer.toHexString(b[i] & 0xFF);
            if (hex.length() == 1) {
                builder.append("0");
            }
            builder.append(hex);
        }
        return builder.toString();
    }

    public static byte[] hexStringToBytes(String hex) {
        byte[] bs = new byte[hex.length() / 2];
        for (int i = 0; i < hex.length() / 2; i++) {
            bs[i] = (byte) (Integer.parseInt(hex.substring(i*2, i*2 + 2), 16) & 0xFF);
        }
        return bs;
    }
}
