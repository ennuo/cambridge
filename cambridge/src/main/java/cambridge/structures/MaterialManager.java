package cambridge.structures;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.data.UID;
import org.joml.Vector3f;

/**
 * s32 UID
 * f32 traction
 * f32 slidingFriction (makes it harder to push)
 * f32 ???
 * f32 acceleration(?) (1.0 = stay in place, 1.5 = float up)
 * f32 density/weight?
 * grabbable: bool
 * pad x3
 * f32 rollingFriction?
 */

/* MaterialPack */

public class MaterialManager implements Serializable
{

    public static class RenderMaterial implements Serializable
    {
        public UID uid;
        public UID physicsMaterial;
        public String texture;
        public String bevelTexture;
        public int bevelType;
        public float bevelSize;
        public Vector3f color;
        public Vector3f tint;

        @Override
        public void load(MemoryInputStream stream)
        {
            stream.i32();
            this.uid = stream.uid();
            this.physicsMaterial = stream.uid();
            this.texture = stream.at(stream.getOffset() + stream.i32()).cstr();
            this.bevelTexture = stream.at(stream.getOffset() + stream.i32()).cstr();

            stream.seek(0x4);
            
            this.bevelType = stream.i32();
            this.bevelSize = stream.f32();

            stream.seek(0x14);
            this.color = stream.v3();
            this.tint = stream.v3();
            stream.seek(0x10);
        }
    }

    public RenderMaterial[] materials;

    @Override
    public void load(MemoryInputStream stream)
    {
        int c1 = stream.i32();
        int c2 = stream.i32();

        MemoryInputStream o1 = stream.at(stream.getOffset() + stream.i32());
        MemoryInputStream o2 = stream.at(stream.getOffset() + stream.i32());

        this.materials = new RenderMaterial[c2];
        for (int i = 0; i < c2; ++i)
        {
            RenderMaterial material = new RenderMaterial();
            material.load(o2);
            this.materials[i] = material;
        }
    }
}
