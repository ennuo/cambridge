package cambridge.structures;

import cambridge.io.streams.MemoryInputStream;

public class Jetpack extends GameObject
{
    public float tetherLength;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.tetherLength = stream.f32();
    }
}
