package cambridge.enums;

import cambridge.io.ValueEnum;

public enum MeshType implements ValueEnum<Integer>
{
    MESH(0),
    GENERATED_MESH(1);

    private final int value;

    MeshType(int value)
    {
        this.value = value;
    }

    public Integer getValue()
    {
        return this.value;
    }

    public static MeshType fromValue(int value)
    {
        for (MeshType type : MeshType.values())
        {
            if (type.value == value)
                return type;
        }
        return null;
    }
}
