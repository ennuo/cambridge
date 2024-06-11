package cambridge.structures;

import cambridge.enums.ItemType;
import cambridge.enums.LanguageID;
import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;

import java.nio.charset.StandardCharsets;

public class ItemMetadata implements Serializable
{
    public int version;
    public int folderHash;
    public int categoryKey;
    public int themeKey;
    public ItemType type;
    public boolean isDLC;
    public int themeIndex;
    public int categoryIndex;
    public int dlcIndex;
    public int dlcArchiveIndex;
    public int resourceIndex;
    public int hiddenShapeFlags;
    public String[] nameTranslations;
    public String[] descTranslations;
    public String[] creatorTranslations;
    public String resource;

    @Override
    public void load(MemoryInputStream stream)
    {
        this.version = stream.i32();
        this.folderHash = stream.i32();
        this.categoryKey = stream.i32();
        this.themeKey = stream.i32();
        this.type = stream.enum32(ItemType.class);
        stream.i32();
        this.isDLC = stream.i32() == 1;
        this.themeIndex = stream.i32();
        this.categoryIndex = stream.i32();
        this.dlcIndex = stream.i32();
        this.dlcArchiveIndex = stream.i32();
        this.resourceIndex = stream.i32();
        this.hiddenShapeFlags = stream.i32();
        int nameTableStart = stream.i32();
        int descTableStart = stream.i32();
        int creatorTableStart = stream.i32();
        int resourceStart = stream.i32();

        this.nameTranslations = this.loadStringTable(stream.bytes(descTableStart - nameTableStart));
        this.descTranslations =
            this.loadStringTable(stream.bytes(creatorTableStart - descTableStart));
        this.creatorTranslations =
            this.loadStringTable(stream.bytes(resourceStart - creatorTableStart));
        this.resource = stream.cstr();
    }

    private String[] loadStringTable(byte[] table)
    {
        MemoryInputStream stream = new MemoryInputStream(table);
        int stringCount = LanguageID.MAX;
        stream.seek(stringCount * 0x2);
        String[] strings = new String[stringCount];
        for (int i = 0; i < stringCount; ++i)
        {
            int start = stream.getOffset();
            while (stream.u16() != 0) ;
            int end = stream.getOffset();
            strings[i] = new String(stream.getBuffer(), start, end - start,
                StandardCharsets.UTF_16LE);
        }
        return strings;
    }

}
