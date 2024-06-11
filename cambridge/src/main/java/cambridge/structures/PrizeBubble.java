package cambridge.structures;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.data.UID;
import org.joml.Vector3f;

public class PrizeBubble implements Serializable
{
    public UID uid;
    public Vector3f position;
    public String icon;
    public String prize;

    @Override
    public void load(MemoryInputStream stream)
    {
        this.uid = stream.uid();
        stream.seek(0x8);
        this.position = stream.v3();
        stream.seek(0xC);
        this.icon = stream.at(stream.getOffset() + stream.i32()).cstr();
        this.prize = stream.at(stream.getOffset() + stream.i32()).cstr();
    }

}
