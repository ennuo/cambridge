package cambridge.structures;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import cambridge.io.streams.MemoryInputStream.SeekMode;

import java.util.ArrayList;

public class Slots implements Serializable
{
    public static class FrontendSlot
    {
        public float x, y, z, w;
    }

    public ArrayList<FrontendSlot> slots = new ArrayList<>();

    @Override
    public void load(MemoryInputStream stream)
    {
        stream.seek(0x20, SeekMode.Relative);
        int count = stream.i32();
        stream.seek(0xc, SeekMode.Relative);

        for (int i = 0; i < count; ++i)
        {
            FrontendSlot slot = new FrontendSlot();
            stream.seek(0xc, SeekMode.Relative);
            slot.x = stream.f32();
            slot.y = stream.f32();
            slot.z = stream.f32();
            slot.w = stream.f32();
            stream.seek(0xc, SeekMode.Relative);

            slots.add(slot);
        }
    }


}
