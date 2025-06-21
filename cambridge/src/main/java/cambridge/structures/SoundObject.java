package cambridge.structures;

import cambridge.io.streams.MemoryInputStream;

public class SoundObject extends GameObject
{
    public float param;
    public String sfx;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.param = stream.f32();
        this.sfx = stream.str(0x50);
    }
}
