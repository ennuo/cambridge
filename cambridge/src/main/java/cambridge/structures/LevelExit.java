package cambridge.structures;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.data.UID;

public class LevelExit implements Serializable
{
    public UID uid;
    
    @Override
    public void load(MemoryInputStream stream)
    {
        this.uid = stream.uid();
    }
}
