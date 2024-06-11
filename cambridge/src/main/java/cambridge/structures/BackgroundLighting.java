package cambridge.structures;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import org.joml.Vector4f;

public class BackgroundLighting implements Serializable
{
    // type, 0, = directiona;
    public Vector4f globalAmbientColor;
    public BackgroundLight[] lights = new BackgroundLight[4];
    public Vector4f fogColor;
    public float fogNear, fogFar;
    public boolean fogEnabled;

    @Override
    public void load(MemoryInputStream stream)
    {
        stream.seek(0x10);
        this.globalAmbientColor = stream.argb();
        for (int i = 0; i < 4; ++i)
        {
            BackgroundLight light = new BackgroundLight();
            light.load(stream);
            this.lights[i] = light;
        }
        this.fogColor = new Vector4f(
            ((float) stream.u32()) / ((float) 0xFFFFFFFFL),
            ((float) stream.u32()) / ((float) 0xFFFFFFFFL),
            ((float) stream.u32()) / ((float) 0xFFFFFFFFL),
            1.0f
        );
        this.fogNear = stream.f32();
        this.fogFar = stream.f32();
        this.fogEnabled = stream.i32() != 0;
    }
}
