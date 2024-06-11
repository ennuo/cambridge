package cambridge.structures;

import org.joml.Vector3f;

import cambridge.io.streams.MemoryInputStream;

public class MeshEntity extends GameObject 
{
    public Vector3f modelPositionOffset;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        modelPositionOffset = stream.v3();
    }
}
