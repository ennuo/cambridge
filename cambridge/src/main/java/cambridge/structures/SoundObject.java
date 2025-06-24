package cambridge.structures;

import cambridge.io.streams.MemoryInputStream;

public class SoundObject extends GameObject
{
    public float radius;
    public int param;
    public int playMode;
    public String sfx;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.radius = stream.f32();
        stream.f32(); //unknown
        this.param = stream.i32();
        this.playMode = stream.i32();
        stream.f32(); //unknown
        this.sfx = stream.str(0x50);
    }
}
