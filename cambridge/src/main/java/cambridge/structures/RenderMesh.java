package cambridge.structures;

import cambridge.enums.MeshType;
import cambridge.enums.ObjectType;
import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import org.joml.Vector3f;

public class RenderMesh implements Serializable
{
    public MeshType meshType = MeshType.MESH;
    public ObjectType objectType = ObjectType.STATIC;
    public int materialUID;
    public int oldMaterialUID;
    public float massDepth;
    public Vector3f[] vertices;

    public int v0;
    public int v1;
    public int v2;

    @Override
    public void load(MemoryInputStream stream)
    {
        this.meshType = stream.enum32(MeshType.class);
        this.objectType = stream.enum32(ObjectType.class);

        this.materialUID = stream.i32();
        this.oldMaterialUID = stream.i32();

        v0 = stream.i32();
        v1 = stream.i32();

        this.massDepth = stream.f32();

        v2 = stream.i32();

        int vertexCount = stream.i32();
        MemoryInputStream vertexBuffer = stream.at(stream.getOffset() + stream.i32());
        this.vertices = new Vector3f[vertexCount];
        for (int i = 0; i < vertexCount; ++i)
        {
            this.vertices[i] = vertexBuffer.v3();
            vertexBuffer.f32();
        }
    }

}
