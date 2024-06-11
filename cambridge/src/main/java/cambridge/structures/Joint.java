package cambridge.structures;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.data.UID;
import org.joml.Vector3f;

public class Joint implements Serializable
{
    public static boolean USE_V013 = false;

    public UID uid;
    public int visibility;
    public UID a, b;
    public Vector3f contactA, contactB;

    public Vector3f aDirection = new Vector3f();
    public Vector3f bDirection = new Vector3f();

    public float length;


    public boolean isBolt;

    @Override
    public void load(MemoryInputStream stream)
    {
        this.load(stream, false);
    }

    public void load(MemoryInputStream stream, boolean isBolt)
    {
        this.isBolt = isBolt;

        this.uid = stream.uid();
        stream.seek(0x4);
        this.visibility = stream.i32();
        this.a = stream.uid();
        this.b = stream.uid();
        this.contactA = stream.v3();

        if (USE_V013)
        {
            this.contactB = stream.v3();
            if (!isBolt)
            {
                this.aDirection = stream.v3();
                this.bDirection = stream.v3();

                // System.out.println(String.format("A: %.02f, %.02f, %.02f", aDirection.x,
                // aDirection.y, aDirection.z));
                // System.out.println(String.format("B: %.02f, %.02f, %.02f", bDirection.x,
                // bDirection.y, bDirection.z));
                // System.out.println("A: " + Math.atan2(aDirection.y, aDirection.x));
                // System.out.println("B: " + Math.atan2(bDirection.y, bDirection.x));
            }
            this.length = stream.f32();
            return;
        }

        if (isBolt) this.contactB = this.contactA;
        else
        {
            this.contactB = stream.v3();
            this.aDirection = stream.v3();
            this.bDirection = stream.v3();
            this.length = stream.f32();
        }
    }
}
