package cambridge.structures;

import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.data.UID;

public class MagicEye extends GameObject
{
    public UID parent;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.parent = stream.uid();
    }
}
