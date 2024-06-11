package cambridge.structures;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.data.UID;

public class Glue implements Serializable
{
    public UID[] objects;

    @Override
    public void load(MemoryInputStream stream)
    {
        this.objects = new UID[stream.i32()];
        for (int i = 0; i < this.objects.length; ++i)
            this.objects[i] = new UID(stream.at(stream.getOffset() + stream.i32()).cstr());
    }

}
