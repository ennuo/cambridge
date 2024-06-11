package cambridge.resources.model;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class VertexData
{
    public float[] weights = new float[8];
    public int[] joints = new int[4];
    public Vector3f normal;
    public Vector2f uv;
    public Vector4f color;
    public Vector3f position;

    public VertexData()
    {
        this.weights[0] = 1.0f;
    }
}
