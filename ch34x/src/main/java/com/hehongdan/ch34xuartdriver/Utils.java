package com.hehongdan.ch34xuartdriver;

import java.util.Locale;

/**
 * 工具类
 *
 * @author yujing  2022年2月11日17:31:45
 */
public class Utils {
    /**
     * byte数组转hexString
     */
    public static String bytesToHexString(byte[] bArray) {
        return bytesToHexString(bArray,"");
    }

    /**
     * byte数组转hexString
     * @param bArray byte数组
     * @param separate 每位之间用个符号隔开
     * @return hexString
     */
    public static String bytesToHexString(byte[] bArray,String separate) {
        StringBuilder sb = new StringBuilder(bArray.length);
        String sTemp;
        for (byte aBArray : bArray) {
            sTemp = Integer.toHexString(0xFF & aBArray);
            if (sTemp.length() < 2)
                sb.append(0);
            sb.append(sTemp.toUpperCase(Locale.US)+separate);
        }
        return sb.toString();
    }

    /**
     * hexString转byte数组
     */
    public static byte[] hexStringToByte(String hex) {
        if (hex != null) {
            hex = hex.replaceAll(" ", "")
                    .replaceAll("\n", "")
                    .replaceAll("\r", "")
                    .replaceAll("\t", "")
                    .toUpperCase(Locale.US);
        } else {
            return new byte[0];
        }
        int len = (hex.length() / 2);
        byte[] result = new byte[len];
        char[] aChar = hex.toCharArray();
        for (int i = 0; i < len; i++) {
            int pos = i * 2;
            result[i] = (byte) (toByte(aChar[pos]) << 4 | toByte(aChar[pos + 1]));
        }
        return result;
    }

    @SuppressWarnings("SpellCheckingInspection")
    private static byte toByte(char c) {
        return (byte) "0123456789ABCDEF".indexOf(c);
    }
}
