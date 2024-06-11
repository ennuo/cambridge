package cambridge.structures.joints;

import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.Joint;

public class Rod extends Joint
{
    public boolean stiff;

    @Override
    public void load(MemoryInputStream stream)
    {
        super.load(stream);
        this.stiff = stream.i32() != 0;
    }
}
