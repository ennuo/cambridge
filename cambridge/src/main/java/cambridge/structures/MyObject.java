package cambridge.structures;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.data.UID;

public class MyObject implements Serializable
{
    public UID uid;
    public int world; 
    
    
    // might actually be world?
    // CE5693A4 = world
    // 

    public UID[] objects;

    @Override
    public void load(MemoryInputStream stream)
    {
        this.uid = stream.uid();
        this.world = stream.i32();
        stream.seek(0x8);
        this.objects = new UID[stream.i32()];
        for (int i = 0; i < this.objects.length; ++i)
            this.objects[i] = stream.uid();
    }
}
