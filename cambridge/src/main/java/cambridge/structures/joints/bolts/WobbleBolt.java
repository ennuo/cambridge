package cambridge.structures.joints.bolts;

import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.Joint;

public class WobbleBolt extends Joint
{
    public float rotation;
    public float angle;
    public float tightness;

    public float time;
    public float pause;
    public float sync;

    public int flipperMotion;
    public boolean backwards;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream, true);

        this.rotation = stream.f32();
        this.angle = stream.f32();
        this.tightness = stream.f32();

        this.time = stream.f32();
        this.pause = stream.f32();
        this.sync = stream.f32();

        this.flipperMotion = stream.i32();
        // 0 = none
        // 1 = in
        // 2 = out
        this.backwards = stream.i32() != 0;
    }
}
