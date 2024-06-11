package cambridge.structures;

import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.data.UID;

public class Switch extends GameObject
{
    public int behavior;
    public boolean inverted;
    public float activation;
    public UID[] targets;
    public float radius;
    public int colorIndex;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.behavior = stream.i32();
        // 0 = on/off
        // 1 = direction
        // 2 = one-shot
        this.inverted = stream.i32() != 0;
        boolean unk = stream.i32() != 0;
        this.activation = stream.f32();
        int targetCount = stream.i32();
        this.targets = new UID[targetCount];
        for (int i = 0; i < targetCount; ++i)
            this.targets[i] = stream.uid();
        stream.bytes((0x64 - targetCount) * 0x4);
        stream.i32(); // unk 
        this.radius = stream.f32(); // x50
        this.colorIndex = stream.i32();
        stream.uid(); // uid of switch base?
        stream.i32(); // unk
    }
}
