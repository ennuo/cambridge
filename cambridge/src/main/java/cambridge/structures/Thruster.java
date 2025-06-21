package cambridge.structures;

import cambridge.io.streams.MemoryInputStream;

public class Thruster extends GameObject
{
    public float strength;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.strength = stream.f32();
    }
}
