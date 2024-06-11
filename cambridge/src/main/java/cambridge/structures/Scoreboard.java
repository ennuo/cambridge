package cambridge.structures;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.data.UID;

public class Scoreboard implements Serializable
{
    public UID uid;

    public String[] levelCompleteIcons;
    public String[] levelCompleteRewards;

    public String[] levelAcedIcons;
    public String[] levelAcedRewards;

    public String[] levelCollectedIcons;
    public String[] levelCollectedRewards;

    @Override
    public void load(MemoryInputStream stream)
    {
        this.uid = stream.uid();
        stream.u32();
        stream.u32();

        this.levelCompleteIcons = new String[] { stream.refstr(), stream.refstr(),
            stream.refstr() };
        this.levelCompleteRewards = new String[] { stream.refstr(), stream.refstr(),
            stream.refstr() };

        this.levelAcedIcons = new String[] { stream.refstr(), stream.refstr(), stream.refstr() };
        this.levelAcedRewards = new String[] { stream.refstr(), stream.refstr(), stream.refstr() };

        this.levelCollectedIcons = new String[] { stream.refstr(), stream.refstr(),
            stream.refstr() };
        this.levelCollectedRewards = new String[] { stream.refstr(), stream.refstr(),
            stream.refstr() };
    }
}
