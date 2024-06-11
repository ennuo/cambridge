package cambridge.resources;

import cambridge.util.Bytes;
import cambridge.util.Crypto;
import cambridge.util.FileIO;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class Archive
{
    public static Map<String, String> PATHS = new GsonBuilder().create().fromJson(
        FileIO.getResourceFileAsString("/paths.json"),
        new TypeToken<Map<String, String>>() { }.getType()
    );

    public File source;

    public static class ArchiveEntry
    {
        /**
         * Hash of file name
         */
        private int name;

        /**
         * Hash of path in archive
         */
        private final int path;

        /**
         * Offset of data in archive
         */
        private final int offset;

        /**
         * Size of data in archive
         */
        private final int size;

        public ArchiveEntry(int path, int offset, int size)
        {
            this.path = path;
            this.offset = offset;
            this.size = size;
        }

        public void setName(int name)
        {
            this.name = name;
        }

        public int getName()
        {
            return this.name;
        }

        public int getPath()
        {
            return this.path;
        }

        public int getOffset()
        {
            return this.offset;
        }

        public int getSize()
        {
            return this.size;
        }
    }

    private int version = 1;
    private HashMap<Integer, ArchiveEntry> entryLookup = new HashMap<>();
    private final HashMap<ArchiveEntry, byte[]> dataLookup = new HashMap<>();

    public Archive(String path)
    {
        this(new File(path));
    }

    public Archive(File path)
    {
        this.source = path;

        byte[] archive = FileIO.read(path.getAbsolutePath());

        Crypto.xor(archive, 0, 0xC);

        byte[] header = Arrays.copyOfRange(archive, 0, 0xC);
        int archiveNameHash = Bytes.toIntegerLE(header, 0x0);
        this.version = Bytes.toIntegerLE(header, 0x4);
        int count = Bytes.toIntegerLE(header, 0x8);

        if (archiveNameHash != Crypto.crc32(path.getName()))
            System.out.println("Filename CRC32 hash mismatch! Continuing anyway, but the game " +
                               "will fail to load this archive!");

        Crypto.xor(archive, 0xC, 0x10 + (count * 0xC));

        byte[] headerMd5 = Arrays.copyOfRange(archive, 0xC, 0x1c);
        if (!Arrays.equals(headerMd5, Crypto.md5(header)))
            System.out.println("Header MD5 hash mismatch! Continuing anyway, but the game will " +
                               "fail to load this archive!");

        byte[] table = Arrays.copyOfRange(archive, 0x1c, 0x1c + (count * 0xC));
        this.entryLookup = new HashMap<>(count);
        for (int offset = 0; offset < table.length; offset += 0xC)
        {
            int uid = Bytes.toIntegerLE(table, offset);
            ArchiveEntry entry = new ArchiveEntry(
                uid,
                Bytes.toIntegerLE(table, offset + 0x4),
                Bytes.toIntegerLE(table, offset + 0x8)
            );

            this.entryLookup.put(uid, entry);

            byte[] data = Arrays.copyOfRange(archive, entry.getOffset(),
                entry.getOffset() + entry.getSize());
            String magic = new String(Arrays.copyOfRange(data, 0x0, data.length >= 4 ? 4 :
                data.length), StandardCharsets.US_ASCII);
            if (magic.equals("RIFF") || magic.equals("~SCE"))
            {
                this.dataLookup.put(entry, data);
                continue;
            }

            Crypto.xor(data);

            byte[] info = Arrays.copyOfRange(data, data.length - 0x19, data.length);
            data = Arrays.copyOfRange(data, 0x0, data.length - 0x19);

            int realSize = Bytes.toIntegerLE(info, 0x0);
            boolean isCompressed = info[0x4] == 1;
            entry.name = Bytes.toIntegerLE(info, 0x5);
            byte[] md5 = Arrays.copyOfRange(info, 0x9, 0x19);

            if (isCompressed)
                data = Crypto.decompress(data, realSize);

            this.dataLookup.put(entry, data);
        }
    }

    public static String resolve(String path)
    {
        String archivePath = path.replaceAll("\\\\", "/");
        if (!archivePath.startsWith("/"))
            archivePath = "/" + archivePath;
        return archivePath.toLowerCase();
    }

    public byte[] extract(String path)
    {
        path = path.replaceAll("\\\\", "/");
        if (!path.startsWith("/"))
            path = "/" + path;
        path = path.toLowerCase();
        ArchiveEntry entry = this.entryLookup.get(Crypto.crc32(path));
        if (entry == null) return null;
        return this.dataLookup.get(entry);
    }

    public ArchiveEntry get(String path)
    {
        path = path.replaceAll("\\\\", "/");
        if (!path.startsWith("/"))
            path = "/" + path;
        path = path.toLowerCase();
        return this.entryLookup.get(Crypto.crc32(path));
    }
}
