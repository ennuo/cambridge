package cambridge.enums;

import cambridge.io.ValueEnum;

public enum LethalType implements ValueEnum<Integer>
{
    NONE(0),
    ELECTRICITY(1),
    FIRE(2),
    GAS(3),
    SPIKE(4),
    CRUSH(5);

    private final int value;

    LethalType(int value)
    {
        this.value = value;
    }

    public Integer getValue()
    {
        return this.value;
    }

    public static LethalType fromValue(int value)
    {
        for (LethalType type : LethalType.values())
        {
            if (type.value == value)
                return type;
        }
        return null;
    }
}
