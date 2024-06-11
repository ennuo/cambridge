package cambridge.structures;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.data.UID;

public class World implements Serializable
{
    public UID uid;
    public String background;

    @Override
    public void load(MemoryInputStream stream)
    {
        this.uid = stream.uid();
        this.background = stream.str(0x50);
    }
}
