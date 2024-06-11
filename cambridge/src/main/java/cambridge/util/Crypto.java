package cambridge.util;

import cwlib.types.data.GUID;
import org.anarres.lzo.LzoDecompressor1x;
import org.anarres.lzo.lzo_uintp;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.zip.CRC32;

public class Crypto
{
    private static final byte[] XOR_DATA = FileIO.getResourceFile("/keys");

    public static byte[] md5(byte[] data)
    {
        try
        {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(data);
            return md.digest();
        }
        catch (Exception ex) { return null; }
    }

    public static GUID makeGUID(String string)
    {
        CRC32 crc = new CRC32();
        crc.update(string.getBytes(StandardCharsets.US_ASCII));
        return new GUID(crc.getValue() | 0x80000000L);
    }

    public static int crc32(String string)
    {
        string = string.toLowerCase();
        CRC32 crc = new CRC32();
        crc.update(string.getBytes(StandardCharsets.US_ASCII));
        return (int) (~crc.getValue() >>> 0);
    }

    public static byte[] xor(byte[] data)
    {
        return Crypto.xor(data, 0, data.length);
    }

    public static byte[] xor(byte[] data, int offset, int size)
    {
        for (int i = offset, j = 0; j < size; ++i, ++j)
            data[i] ^= Crypto.XOR_DATA[j];
        return data;
    }

    public static byte[] decompress(byte[] data, int size)
    {
        byte[] stream = new byte[size];
        lzo_uintp p = new lzo_uintp(size);
        int res = new LzoDecompressor1x().decompress(
            data,
            0,
            data.length,
            stream,
            0,
            p
        );
        return Arrays.copyOfRange(stream, 0, p.value);
    }
}
