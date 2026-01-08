package cambridge.structures;

import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.data.UID;

public class CreatureNavigator extends GameObject
{
    public UID parent;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.parent = stream.uid();
    }
}
