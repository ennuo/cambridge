package cambridge.structures;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;

public class Decal implements Serializable
{
    public String texture;
    public float u, v;
    public float scale;
    public float angle;
    public boolean flipped;

    @Override
    public void load(MemoryInputStream stream)
    {
        this.texture = stream.at(stream.getOffset() + stream.i32()).cstr();
        this.u = stream.f32();
        this.v = stream.f32();
        this.scale = stream.f32();
        this.angle = stream.f32();
        this.flipped = stream.i32() != 0;
    }
}
