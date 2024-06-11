package cambridge.structures;

import cambridge.io.streams.MemoryInputStream;
import org.joml.Vector2f;
import org.joml.Vector3f;

public class CameraGadget extends GameObject
{
    public float trackPlayer = 1.0f;
    public Vector2f pitchAngle;
    public Vector3f targetPosition;
    public Vector3f triggerPosition;
    public Vector3f triggerBox;
    public float zoomDistance;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.trackPlayer = stream.f32();
        this.pitchAngle = stream.v2();
        this.targetPosition = stream.v3();

        this.triggerPosition = stream.v3();
        this.triggerBox = stream.v3();

        this.zoomDistance = stream.f32();
    }
}
