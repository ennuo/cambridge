package cambridge.structures.joints.bolts;

import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.Joint;

public class SprungBolt extends Joint
{
    public float angle;
    public float tightness;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream, true);
        this.angle = stream.f32();
        this.tightness = stream.f32();
    }
}
