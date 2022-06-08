package com.weeswares.iok.health.helpers;

public class HexString {
    private final static char[] HEX_ARRAY = "0123456789ABCDEF".toCharArray();

    private HexString() {
        // Utility class.
    }

    public static char[] bytesToHex(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];

        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }
        return hexChars;
    }

    public static String bytesToHexString(byte[] bytes) {
        char[] hexChars = new char[bytes.length * 2];

        for (int j = 0; j < bytes.length; j++) {
            int v = bytes[j] & 0xFF;
            hexChars[j * 2] = HEX_ARRAY[v >>> 4];
            hexChars[j * 2 + 1] = HEX_ARRAY[v & 0x0F];
        }

        return new String(hexChars);
    }

    public static byte[] hexToBytes(String hexRepresentation) {
        if (hexRepresentation.length() % 2 == 1) {
            throw new IllegalArgumentException("hexToBytes requires an even-length String parameter");
        }

        int len = hexRepresentation.length();
        byte[] data = new byte[len / 2];

        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hexRepresentation.charAt(i), 16) << 4)
                    + Character.digit(hexRepresentation.charAt(i + 1), 16));
        }

        return data;
    }

    public static void calculateWeight(byte[] bytes) {
        int integerVal = byteArrayToShortLE(bytes, 1);
        System.out.println("Integer = " + integerVal);
        System.out.println("Actual weight =" + (integerVal * 0.005));
    }

    public static void calculateTemperature(byte[] bytes) {
        int integerVal = byteArrayToShortLE(bytes, 1);
        System.out.println("byte[] size is " + bytes.length);
        for (byte b : bytes) {
            System.out.println("Byte:" + (int) b);
        }
//        System.out.println("printl");
        System.out.println("Integer = " + integerVal);
        System.out.println("Actual temperature =" + (integerVal * 0.01));
    }

    public static short byteArrayToShortLE(final byte[] b, final int offset) {
        short value = 0;
        for (int i = 0; i < 2; i++) {
            value |= (b[i + offset] & 0x000000FF) << (i * 8);
        }

        return value;
    }

    public static int byteArrayToIntLE(final byte[] b, final int offset) {
        int value = 0;

        for (int i = 0; i < 4; i++) {
            value |= ((int) b[i + offset] & 0x000000FF) << (i * 8);
        }

        return value;
    }
}
