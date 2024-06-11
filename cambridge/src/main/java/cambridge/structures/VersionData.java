package cambridge.structures;

import java.util.ArrayList;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;

public class VersionData implements Serializable 
{
    public ArrayList<Integer> gameObjectUidOffsets = new ArrayList<>();

    @Override
    public void load(MemoryInputStream stream, int size)
    {
        int end = stream.getOffset() + size;
        while (stream.getOffset() < end)
        {
            int value = stream.i32();
            gameObjectUidOffsets.add(stream.getOffset() - 4 + value);
        }
    }

    @Override
    public void load(MemoryInputStream stream)
    {
        throw new RuntimeException("not this one nope");
    }
    
}
