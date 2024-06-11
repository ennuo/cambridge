package cambridge.structures.switches;

import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.GameObject;

public class MagneticKey extends GameObject
{
    public int colorIndex;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.colorIndex = stream.i32();
    }
}
