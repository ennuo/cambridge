package cambridge.structures;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class BackgroundLight implements Serializable
{
    public int lightType; // 0 = directional, 1 = point
    public int lightComponents; // 0 = diffuse, 1 = diffuse + specular, 2 = powered diffuse +
    // specular
    public Vector4f diffuseColor;
    public Vector4f ambientColor;
    public Vector4f specularColor;
    public Vector3f lightVector;
    public Vector3f lightDirection;
    public Vector3f lightAttentuation;
    public float lightConvergence;
    public float lightCutoff;
    public boolean enabled;

    @Override
    public void load(MemoryInputStream stream)
    {
        this.ambientColor = stream.argb();
        this.diffuseColor = stream.argb();
        this.specularColor = stream.argb();
        this.lightType = stream.i32();
        this.lightComponents = stream.i32();
        this.lightVector = stream.v3();
        this.lightDirection = stream.v3();
        this.lightAttentuation = stream.v3();
        this.lightConvergence = stream.f32();
        this.lightCutoff = stream.f32();
        this.enabled = stream.i32() != 0;
    }
}
