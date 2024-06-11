package cambridge.structures.joints;

import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.Joint;

public class Spring extends Joint
{
    public float strength;
    public boolean stiff;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.strength = stream.f32();
        this.stiff = stream.i32() != 0;
    }
}
