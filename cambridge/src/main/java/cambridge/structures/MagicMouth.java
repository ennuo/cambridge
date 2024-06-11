package cambridge.structures;

import cambridge.io.streams.MemoryInputStream;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.HashMap;

public class MagicMouth extends GameObject
{
    public float radius;
    public boolean cutscene;
    public Vector2f pitchAngle;
    public Vector3f targetBox;

    public String sfx;

    public HashMap<String, String> translations = new HashMap<>();

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);

        this.radius = stream.f32();
        this.cutscene = stream.i32() != 0;
        this.pitchAngle = stream.v2();
        this.targetBox = stream.v3();
        stream.f32();

        int translationCount = stream.i32();
        this.sfx = stream.str(0x50);

        for (int i = 0; i < translationCount; ++i)
        {
            this.translations.put(
                stream.at(stream.getOffset() + stream.i32()).cstr(),
                stream.at(stream.getOffset() + stream.i32()).cwstr()
            );
        }
    }
}
