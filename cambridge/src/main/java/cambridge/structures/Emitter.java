package cambridge.structures;

import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.data.UID;

public class Emitter extends GameObject
{
    public UID emittedObjectUID;
    public float linearVelocity;
    public float angularVelocity;
    public float frequency;
    public float lifetime;
    public int maxEmitted;
    public int maxEmittedAtOnce;
    public float sync;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);

        this.emittedObjectUID = stream.uid();
        stream.seek(0xC);
        this.linearVelocity = stream.f32();
        this.angularVelocity = stream.f32();
        this.frequency = stream.f32();
        this.lifetime = stream.f32();
        this.maxEmitted = stream.i32();
        this.maxEmittedAtOnce = stream.i32();
        this.sync = stream.f32();
    }

}
