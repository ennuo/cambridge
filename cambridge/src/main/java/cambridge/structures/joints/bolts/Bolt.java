package cambridge.structures.joints.bolts;

import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.Joint;

public class Bolt extends Joint
{
    public float tightness;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream, true);
        this.tightness = stream.f32();
    }
}
