package cambridge.enums;

import cambridge.util.Crypto;

public enum ResourceType
{
    ANIMATION,
    SKELETON,
    SKIN,
    MESH,
    MODEL,
    TEXTURE,
    FONT,
    SFX_CROSSFADE,
    SFX_BANK,
    LEVEL_BIFF,
    OBJECT_BIFF,
    MATERIAL_BIFF,
    CATALOG_BIFF,
    BACKGROUND_BIFF,
    GUI_BIFF,
    AUDIO_BIFF,
    MUSIC_BIFF,
    FRONTEND_BIFF,
    FRONTEND_RES,
    LAMS,
    POPIT_GROUP,
    POPIT_METADATA,
    COSTUME_DATA,
    COSTUME_MATERIAL,
    BINARY_FILE,
    ARCHIVE;

    private final int uid;

    ResourceType()
    {
        this.uid = Crypto.crc32(this.name());
    }

    public Integer getUID()
    {
        return this.uid;
    }

    public static ResourceType fromUID(int uid)
    {
        for (ResourceType type : ResourceType.values())
        {
            if (type.uid == uid)
                return type;
        }
        return null;
    }
}
