package cambridge.structures;

import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.data.UID;

public class CreaturePiece extends GameObject
{
    public UID referenceUid;
    
    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.referenceUid = stream.uid();
    }
}
