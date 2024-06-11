package cambridge.resources;

import cambridge.io.streams.MemoryInputStream;

import java.nio.charset.StandardCharsets;

public class Group
{
    public String name;
    public int uid;
    public String[] translations;

    public Group(String name, byte[] data)
    {
        this.name = name;
        MemoryInputStream stream = new MemoryInputStream(data);
        stream.i32();
        this.uid = stream.i32();
        stream.f32();
        int stringCount = 0x11;
        stream.seek(stringCount * 0x2);
        this.translations = new String[stringCount];
        for (int i = 0; i < stringCount; ++i)
        {
            int start = stream.getOffset();
            while (stream.u16() != 0) ;
            int end = stream.getOffset();
            this.translations[i] = new String(stream.getBuffer(), start, end - start,
                StandardCharsets.UTF_16LE);
        }
    }
}
