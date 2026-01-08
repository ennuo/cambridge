package cambridge.structures;

import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.data.UID;

public class Jetpack extends GameObject
{
    public float tetherLength;
    public UID parent;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.tetherLength = stream.f32();
        this.parent = stream.uid();
    }
}
