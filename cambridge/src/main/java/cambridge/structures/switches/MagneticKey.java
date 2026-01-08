package cambridge.structures.switches;

import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.GameObject;
import cambridge.structures.data.UID;

public class MagneticKey extends GameObject
{
    public int colorIndex;
    public UID parent;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.colorIndex = stream.i32();
        this.parent = stream.uid();
    }
}
