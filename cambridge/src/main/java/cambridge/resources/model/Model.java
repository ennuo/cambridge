package cambridge.resources.model;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import cambridge.util.FileIO;
import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Model implements Serializable
{
    public String[] textures;
    public Mesh[] meshes;
    public Material[] materials;
    public String[] skeletons;
    public Matrix4f[] bones;
    public String[] skins;

    public Vector3f COM;

    public Model(byte[] data)
    {
        this.load(new MemoryInputStream(data));
    }
    
    @Override
    public void load(MemoryInputStream stream)
    {
        this.textures = new String[stream.i32()];
        for (int i = 0; i < this.textures.length; ++i)
            this.textures[i] = stream.cstr();

        this.meshes = new Mesh[stream.i32()];

        Vector3f min = new Vector3f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY,
            Float.POSITIVE_INFINITY);
        Vector3f max = new Vector3f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY,
            Float.NEGATIVE_INFINITY);

        for (int i = 0; i < this.meshes.length; ++i)
        {
            Mesh mesh = new Mesh();
            mesh.load(stream);
            this.meshes[i] = mesh;

            Vector3f v = mesh.getMaxVert();
            if (v.x > max.x) max.x = v.x;
            if (v.y > max.y) max.y = v.y;
            if (v.z > max.z) max.z = v.z;
            v = mesh.getMinVert();
            if (v.x < min.x) min.x = v.x;
            if (v.y < min.y) min.y = v.y;
            if (v.z < min.z) min.z = v.z;
        }

        this.COM = max.add(min).div(2.0f);

        this.materials = new Material[stream.i32()];
        for (int i = 0; i < this.materials.length; ++i)
        {
            Material material = new Material();
            material.load(stream);
            this.materials[i] = material;
        }

        this.skeletons = new String[stream.i32()];
        for (int i = 0; i < this.skeletons.length; ++i)
            this.skeletons[i] = stream.cstr();

        this.bones = new Matrix4f[stream.i32()];
        for (int i = 0; i < this.bones.length; ++i)
            this.bones[i] = stream.m44();

        this.skins = new String[stream.i32()];
        for (int i = 0; i < this.skins.length; ++i)
            this.skins[i] = stream.cstr();
    }
}
