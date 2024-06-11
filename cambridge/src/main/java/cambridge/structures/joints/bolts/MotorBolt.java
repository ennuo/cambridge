package cambridge.structures.joints.bolts;

import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.Joint;

public class MotorBolt extends Joint
{
    public float tightness;
    public float speed;
    public int direction;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream, true);
        this.tightness = stream.f32();
        this.speed = stream.f32();
        this.direction = stream.i32();
    }
}
