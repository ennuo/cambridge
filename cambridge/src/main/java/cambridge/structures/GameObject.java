package cambridge.structures;

import cambridge.enums.LethalType;
import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.data.UID;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class GameObject implements Serializable
{
    public UID uid;
    public int world;
    // 0x4 bytes
    public int visibility;
    public RenderMesh mesh;
    public String model; // 0x50
    public Vector3f position;
    public Vector2f positionalVelocity;
    public float angularVelocity;
    public float angle;
    public LethalType lethalType = LethalType.NONE;

    @Override
    public void load(MemoryInputStream stream)
    {
        this.uid = stream.uid();
        this.world = stream.i32();
        this.visibility = stream.i32();

        int meshOffset = stream.i32();
        if (meshOffset != 0)
        {
            this.mesh = new RenderMesh();
            this.mesh.load(stream.at(stream.getOffset() + meshOffset - 4));
        }

        this.model = stream.str(0x50);
        this.position = stream.v3();
        this.positionalVelocity = stream.v2();
        this.angularVelocity = stream.f32();
        this.angle = stream.f32(); // radians
        this.lethalType = stream.enum32(LethalType.class);
    }
}
