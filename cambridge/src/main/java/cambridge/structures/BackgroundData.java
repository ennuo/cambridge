package cambridge.structures;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;

public class BackgroundData implements Serializable
{
    public String backgroundModel;
    public String brickModel;
    public float brickRepeatX;

    @Override
    public void load(MemoryInputStream stream)
    {
        this.backgroundModel = stream.str(0x50);
        stream.seek(0x10);
        this.brickModel = stream.str(0x50);
        this.brickRepeatX = stream.f32();
    }


}
