package cambridge.resources.model;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import org.joml.Vector2f;

public class Skin implements Serializable
{

    public static class SkinVertexData
    {
        private final int vertexType;
        private final int numVerts;
        private final int numBones;
        private final int[] bones;

        public SkinVertexData(int vertexType, int numVerts)
        {
            this.vertexType = vertexType;
            this.numVerts = numVerts;
            this.numBones = 0;
            this.bones = new int[] { -1, -1 - 1, -1 };
        }

        public SkinVertexData(MemoryInputStream stream)
        {
            stream.i32();
            stream.i32();
            this.vertexType = stream.i32();
            this.numVerts = stream.i32();
            this.numBones = stream.i32();
            this.bones = new int[] {
                stream.i32(),
                stream.i32(),
                stream.i32(),
                stream.i32()
            };

            for (int i = this.numBones; i < 4; ++i)
                this.bones[i] = -1;
        }

        public int getVertexType()
        {
            return this.vertexType;
        }

        public int getNumVerts()
        {
            return this.numVerts;
        }

        public int getNumBones()
        {
            return this.numBones;
        }

        public int[] getBones()
        {
            return this.bones;
        }
    }

    public Skin(byte[] data)
    {
        this.load(new MemoryInputStream(data));
    }

    private SkinVertexData[] descriptors;
    private Vector2f textureOffset;
    private Vector2f textureScale;

    @Override
    public void load(MemoryInputStream stream)
    {
        this.descriptors = new SkinVertexData[stream.i32()];
        for (int i = 0; i < this.descriptors.length; ++i)
            this.descriptors[i] = new SkinVertexData(stream);
        this.textureOffset = stream.v2();
        this.textureScale = stream.v2();
    }

    public SkinVertexData[] getVertexDescriptors()
    {
        return this.descriptors;
    }

    public Vector2f getTextureOffset()
    {
        return this.textureOffset;
    }

    public Vector2f getTextureScale()
    {
        return this.textureScale;
    }
}
