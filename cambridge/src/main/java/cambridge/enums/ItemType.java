package cambridge.enums;

import cambridge.io.ValueEnum;

public enum ItemType implements ValueEnum<Integer>
{
    NONE(0x0),
    BACKGROUND(0x1),
    STICKER(0x2),
    MATERIAL(0x5),
    OBJECT(0x6),
    SOUND(0x9),
    MUSIC(0xa),
    RESET_COSTUME(0xb),
    COSTUME_MATERIAL(0xd),
    COSTUME_HEAD(0xe),
    COSTUME_HAIR(0xf),
    COSTUME_EYES(0x10),
    COSTUME_GLASSES(0x11),
    COSTUME_MOUTH(0x12),
    COSTUME_MOUSTACHE(0x13),
    COSTUME_NECK(0x14),
    COSTUME_TORSO(0x15),
    COSTUME_WAIST(0x16),
    COSTUME_HANDS(0x17),
    COSTUME_LEGS(0x18),
    COSTUME_FEET(0x19);

    private final int value;

    ItemType(int value)
    {
        this.value = value;
    }

    public Integer getValue()
    {
        return this.value;
    }

    public static ItemType fromValue(int value)
    {
        for (ItemType type : ItemType.values())
        {
            if (type.value == value)
                return type;
        }
        return null;
    }
}
