package cambridge.structures.data;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import cambridge.io.streams.MemoryInputStream.SeekMode;

// slv = FE21[]
// dlv = ???[same size as slv] (each struct is 0x1c bytes)
// + 0x0 - ushort - Flags(?)
// 0x2 = visible
// 0x4 = locked
// 0x8 = also locked(?)
// + 0x2 - ushort - ???
// + 0x4 - ushort - NextLevelIndex

public class StoryLevel implements Serializable
{

    public enum LevelType
    {
        DEVELOPER,
        USER,
        LOCAL,

        STORY,
        CREATOR,
        MINIGAME,

        UNKNOWN
    }

    public int id;
    public LevelType type;
    public int title, description, creator;
    public int frontendIndex;
    public int nextLevelIndex;
    public String root;

    @Override
    public void load(MemoryInputStream stream)
    {
        stream.i32(); // FE21
        stream.i32(); // ? 
        this.id = stream.i32(); // slotNumber
        this.type = LevelType.values()[stream.i32()];

        this.title = stream.i32();
        this.description = stream.i32();
        this.creator = stream.i32();

        stream.i32(); // padding?

        stream.str(0x40); // probably?

        this.frontendIndex = stream.i32();

        stream.seek(0x10, SeekMode.Relative); // padding?

        this.root = stream.str(0x40);

        stream.seek(0x2, SeekMode.Relative); // ?
    }
}
