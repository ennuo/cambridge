package cambridge.structures.switches;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.data.UID;

public class LeverSwitch implements Serializable
{
    public int behavior;
    public boolean inverted;
    public UID uid;
    public UID baseUid;
    public UID handleUid;
    public UID myobjectUid;
    public float activation;
    public UID[] targets;

    @Override
    public void load(MemoryInputStream stream)
    {
        this.uid = stream.uid();
        stream.bytes(0x14);
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

        this.baseUid = stream.uid(); // switch base object uid
        this.handleUid = stream.uid(); // handle uid
        this.myobjectUid = stream.uid(); // my object uid
        stream.i32(); // unknown
    }
}
