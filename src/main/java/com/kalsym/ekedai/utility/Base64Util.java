package com.kalsym.ekedai.utility;

import java.nio.ByteBuffer;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;

public class Base64Util {
    

    public static byte[] uuidToByte(String str){

        UUID uuid = UUID.fromString(str);

        ByteBuffer bb = ByteBuffer.wrap(new byte[16]);
        bb.putLong(uuid.getMostSignificantBits());
        bb.putLong(uuid.getLeastSignificantBits());
        return bb.array();

    }

    public static String byteToBase64(byte[] b) {

        return Base64.encodeBase64URLSafeString(b);
    }

    public static byte[] stringBase64ToByte(String str){

        return Base64.decodeBase64(str);

    }

    public static String byteOfBased64toUuid(  byte[] bytes) {
     
        ByteBuffer bb = ByteBuffer.wrap(bytes);
        UUID uuid = new UUID(bb.getLong(), bb.getLong());
        return uuid.toString();
    }


}
