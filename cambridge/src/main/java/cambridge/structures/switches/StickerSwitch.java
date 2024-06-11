package cambridge.structures.switches;

import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.Switch;

public class StickerSwitch extends Switch
{
    public String sticker;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.sticker = stream.at(stream.getOffset() + stream.i32()).cstr();
    }
}
