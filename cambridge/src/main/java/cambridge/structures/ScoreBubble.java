package cambridge.structures;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.data.UID;
import org.joml.Vector3f;

public class ScoreBubble implements Serializable
{
    public UID uid;
    public Vector3f position;

    @Override
    public void load(MemoryInputStream stream)
    {
        this.uid = stream.uid();
        stream.seek(0x8);
        this.position = stream.v3();
    }

}
