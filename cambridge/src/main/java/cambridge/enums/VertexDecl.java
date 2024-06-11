package cambridge.enums;

public class VertexDecl
{
    public static int GU_VERTICES(int n)
    {
        return ((((n) - 1) & 7) << 18);
    }

    public static int GU_WEIGHTS(int n)
    {
        return ((((n) - 1) & 7) << 14);
    }

    public static final int GU_TEXTURE_8BIT = (1 << 0);
    public static final int GU_TEXTURE_16BIT = (2 << 0);
    public static final int GU_TEXTURE_32BITF = (3 << 0);
    public static final int GU_TEXTURE_BITS = (3 << 0);

    public static final int GU_COLOR_5650 = (4 << 2);
    public static final int GU_COLOR_5551 = (5 << 2);
    public static final int GU_COLOR_4444 = (6 << 2);
    public static final int GU_COLOR_8888 = (7 << 2);
    public static final int GU_COLOR_BITS = (7 << 2);

    public static final int GU_NORMAL_8BIT = (1 << 5);
    public static final int GU_NORMAL_16BIT = (2 << 5);
    public static final int GU_NORMAL_32BITF = (3 << 5);
    public static final int GU_NORMAL_BITS = (3 << 5);

    public static final int GU_VERTEX_8BIT = (1 << 7);
    public static final int GU_VERTEX_16BIT = (2 << 7);
    public static final int GU_VERTEX_32BITF = (3 << 7);
    public static final int GU_VERTEX_BITS = (3 << 7);

    public static final int GU_WEIGHT_8BIT = (1 << 9);
    public static final int GU_WEIGHT_16BIT = (2 << 9);
    public static final int GU_WEIGHT_32BITF = (3 << 9);
    public static final int GU_WEIGHT_BITS = (3 << 9);

    public static final int GU_INDEX_8BIT = (1 << 11);
    public static final int GU_INDEX_16BIT = (2 << 11);
    public static final int GU_INDEX_BITS = (3 << 11);

    public static final int GU_WEIGHTS_BITS = GU_WEIGHTS(8);
    public static final int GU_VERTICES_BITS = GU_VERTICES(8);

    public static final int GU_TRANSFORM_3D = (0 << 23);
    public static final int GU_TRANSFORM_2D = (1 << 23);
    public static final int GU_TRANSFORM_BITS = (1 << 23);

    public static final int GU_POINTS = 0;
    public static final int GU_LINES = 1;
    public static final int GU_LINE_STRIP = 2;
    public static final int GU_TRIANGLES = 3;
    public static final int GU_TRIANGLE_STRIP = 4;
    public static final int GU_TRIANGLE_FAN = 5;
    public static final int GU_SPRITES = 6;

    public static int getWeightCount(int type)
    {
        type = type & VertexDecl.GU_WEIGHTS_BITS;
        for (int i = 1; i <= 8; ++i)
        {
            if (type == VertexDecl.GU_WEIGHTS(i))
                return i;
        }
        return 0;
    }
}
