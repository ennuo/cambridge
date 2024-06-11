package cambridge.structures.joints;

import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.Joint;

public class Piston extends Joint
{
    public float minLength;
    public boolean stiff;
    public float strength;
    public float time;
    public float pause;
    public float sync;
    public int flipperMotion;
    public boolean backwards;
    public float currentLength;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.minLength = stream.f32();
        this.stiff = stream.i32() != 0;
        this.strength = stream.f32();
        this.time = stream.f32();
        this.pause = stream.f32();
        this.sync = stream.f32();
        this.flipperMotion = stream.i32();
        // 0 = none
        // 1 = in
        // 2 = out
        this.backwards = stream.i32() != 0;

        stream.i32(); // gets set to 1 when flipper is not 0?

        stream.bytes(0x8); // unknown

        this.currentLength = stream.f32();

        stream.bytes(0x4);

    }
}
