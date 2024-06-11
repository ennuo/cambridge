package cambridge.structures;

import cambridge.io.streams.MemoryInputStream;

public class Music extends GameObject
{
    public float start;
    public float radius;
    public float volume;
    public String path;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.start = stream.f32();
        this.radius = stream.f32();
        stream.i32();
        this.volume = stream.f32();
        this.path = stream.str(0x50);
    }
}
