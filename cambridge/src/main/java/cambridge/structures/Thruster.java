package cambridge.structures;

import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.data.UID;

public class Thruster extends GameObject
{
    public float strength;
    public int active;
    public UID referenceUid;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.strength = stream.f32();
        this.active = stream.i32();
        this.referenceUid = stream.uid();
    }
}
