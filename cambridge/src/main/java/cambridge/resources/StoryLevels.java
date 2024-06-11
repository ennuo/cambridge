package cambridge.resources;

import cambridge.io.streams.MemoryInputStream;
import cambridge.structures.data.StoryLevel;
import cambridge.structures.data.StoryLevel.LevelType;

import java.util.ArrayList;

public class StoryLevels
{
    public ArrayList<StoryLevel> levels = new ArrayList<>();

    public StoryLevels(byte[] slvData, byte[] dlvData)
    {
        if (slvData == null || dlvData == null)
            throw new NullPointerException("No data provided to StoryLevels constructor!");

        MemoryInputStream slv = new MemoryInputStream(slvData);
        MemoryInputStream dlv = new MemoryInputStream(dlvData);
        while (slv.getOffset() < slv.getLength())
        {
            StoryLevel level = new StoryLevel();
            level.load(slv);
            this.levels.add(level);


            int flags = dlv.u16();
            dlv.u16();
            level.nextLevelIndex = dlv.u16();
            dlv.seek(0x1c - 0x6);

            if (level.type == LevelType.CREATOR)
            {
                System.out.println(level.root + " : " + level.nextLevelIndex);
            }
        }
    }
}
