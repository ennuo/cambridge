package cambridge.resources;

import cambridge.io.streams.MemoryInputStream;
import cambridge.io.streams.MemoryInputStream.SeekMode;

import java.util.ArrayList;

public class Translations
{
    public ArrayList<String> strings = new ArrayList<>();

    public Translations(byte[] data)
    {
        MemoryInputStream stream = new MemoryInputStream(data);
        int uid = stream.i32();
        int dataOffset = stream.i32();
        stream.seek(0x4, SeekMode.Begin);
        while (stream.getOffset() != dataOffset)
        {
            int offset = stream.getOffset();
            String string = stream.at(stream.i32()).cwstr();
            this.strings.add(string);
        }
    }

    public String get(int index)
    {
        if (strings.size() + 1 < index) return "";
        return strings.get(index);
    }
}
