package cambridge.structures;

import cambridge.io.streams.MemoryInputStream;

public class Thruster extends GameObject
{
    public float strength;
    public int active;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.strength = stream.f32();
        this.active = stream.i32();
    }
}
