package cambridge.resources.model;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;

public class Material implements Serializable
{
    public int textureIndex;
    public float specularCoefficient;
    public int diffuseColor;
    public int emissiveColor;
    public int specularColor;

    @Override
    public void load(MemoryInputStream stream)
    {
        stream.i32();
        this.textureIndex = stream.i32();
        stream.i32();
        stream.i32();

        this.specularCoefficient = stream.f32();
        stream.i32(); // some color
        this.diffuseColor = stream.i32();
        this.emissiveColor = stream.i32();
        this.specularColor = stream.i32();
    }

    public int getHash()
    {
        int hash = 7;
        hash = 31 * hash + diffuseColor;
        hash = 31 * hash + specularColor;
        hash = 31 * hash + Float.floatToIntBits(specularCoefficient);
        return hash;
    }
}
