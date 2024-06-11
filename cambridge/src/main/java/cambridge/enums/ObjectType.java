package cambridge.enums;

import cambridge.io.ValueEnum;

public enum ObjectType implements ValueEnum<Integer>
{
    STATIC(0),
    DYNAMIC(1);

    private final int value;

    ObjectType(int value)
    {
        this.value = value;
    }

    public Integer getValue()
    {
        return this.value;
    }

    public static ObjectType fromValue(int value)
    {
        for (ObjectType type : ObjectType.values())
        {
            if (type.value == value)
                return type;
        }
        return null;
    }
}
