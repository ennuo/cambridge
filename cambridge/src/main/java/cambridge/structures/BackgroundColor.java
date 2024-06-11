package cambridge.structures;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import org.joml.Vector3f;

public class BackgroundColor implements Serializable
{
    public Vector3f rgb;

    @Override
    public void load(MemoryInputStream stream)
    {
        this.rgb = new Vector3f(
            (((float) stream.i32()) / (float) 0xff),
            (((float) stream.i32()) / (float) 0xff),
            (((float) stream.i32()) / (float) 0xff)
        );
    }
}
