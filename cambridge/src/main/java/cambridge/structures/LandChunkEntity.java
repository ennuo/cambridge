package cambridge.structures;

import cambridge.io.streams.MemoryInputStream;

public class LandChunkEntity extends GameObject
{
    public Decal[] decals;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.decals = new Decal[stream.i32()];
        for (int i = 0; i < this.decals.length; ++i)
        {
            Decal decal = new Decal();
            decal.load(stream);
            this.decals[i] = decal;
        }
    }
}
