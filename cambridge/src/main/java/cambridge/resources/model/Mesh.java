package cambridge.resources.model;

import cambridge.enums.VertexDecl;
import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import cambridge.io.streams.MemoryInputStream.SeekMode;
import cambridge.resources.model.Skin.SkinVertexData;
import org.joml.Vector2f;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collections;

public class Mesh implements Serializable
{
    private String name;

    private int primitiveType;
    private int vertexType;
    private int vertexCount;
    private int morphCount;

    private Vector3f minVert;
    private Vector3f maxVert;

    private transient int[] indices;

    private int[] morphIDs;
    private int[] morphIndices;

    private Vector2f textureOffset;
    private Vector2f textureScale;

    private transient byte[] buffer;

    public String getName()
    {
        return this.name;
    }

    public int getVertexCount()
    {
        return this.vertexCount;
    }

    public int getMorphCount()
    {
        return this.morphCount;
    }

    public Vector3f getMinVert()
    {
        return this.minVert;
    }

    public Vector3f getMaxVert()
    {
        return this.maxVert;
    }

    public int[] getIndices()
    {
        return this.indices;
    }

    public Vector2f getTextureOffset()
    {
        return this.textureOffset;
    }

    public Vector2f getTextureScale()
    {
        return this.textureScale;
    }

    @Override
    public void load(MemoryInputStream stream)
    {
        this.primitiveType = stream.i32();
        this.vertexType = stream.i32();
        this.vertexCount = stream.i32();
        this.morphCount = stream.i32();
        this.minVert = stream.v3();
        this.maxVert = stream.v3();
        stream.i8();
        byte[] buffer = stream.bytearray();
        this.name = stream.cstr();

        this.morphIDs = new int[this.morphCount];
        this.morphIndices = new int[this.morphCount];
        for (int i = 0; i < this.morphCount; ++i)
        {
            this.morphIDs[i] = stream.i32();
            this.morphIndices[i] = stream.i32();
        }

        stream = new MemoryInputStream(buffer);
        stream.seek(0xf, SeekMode.Relative);
        this.textureOffset = stream.v2();
        this.textureScale = stream.v2();
        stream.seek(0x1, SeekMode.Relative);

        ArrayList<Integer> indices = new ArrayList<>();
        int lastIndex = 0;
        while (true)
        {
            int indexCount = stream.u16();
            int cullingBehavior = stream.u16();
            if (indexCount == 0) break;
            if (this.primitiveType == VertexDecl.GU_TRIANGLES)
            {
                for (int i = lastIndex; i < lastIndex + indexCount; ++i)
                    indices.add(i);
            }
            else if (this.primitiveType == VertexDecl.GU_TRIANGLE_STRIP)
            {
                for (int i = lastIndex, j = 0; i < lastIndex + (indexCount - 2); ++i, ++j)
                {
                    if ((j & 1) != 0) Collections.addAll(indices, i, i + 2, i + 1);
                    else Collections.addAll(indices, i, i + 1, i + 2);
                }
            }
            lastIndex += indexCount;
        }

        this.indices = indices.stream().mapToInt(Integer::valueOf).toArray();

        this.buffer = stream.bytes(stream.getLength() - stream.getOffset());
    }

    public VertexData[][] getVertexData(Skin skin, boolean bakeTextureTransforms)
    {
        int numVertsPerVertex = this.morphCount + 1;

        VertexData[][] streams = new VertexData[numVertsPerVertex][this.vertexCount];
        MemoryInputStream stream = new MemoryInputStream(this.buffer);

        SkinVertexData[] descriptors;
        if (skin == null)
        {
            if (this.vertexType == VertexDecl.GU_WEIGHT_32BITF)
                throw new RuntimeException("No skins supplied to mesh that is skinned!");
            descriptors = new SkinVertexData[] { new SkinVertexData(this.vertexType,
                this.vertexCount) };
        }
        else descriptors = skin.getVertexDescriptors();

        int lastVertexOffset = 0;
        for (SkinVertexData info : descriptors)
        {
            int vertexType = info.getVertexType();
            int weightCount = VertexDecl.getWeightCount(vertexType);
            for (int i = 0; i < info.getNumVerts(); ++i)
            {
                for (int j = 0; j < numVertsPerVertex; ++j)
                {
                    VertexData data = new VertexData();
                    data.joints = info.getBones();

                    switch (vertexType & VertexDecl.GU_WEIGHT_BITS)
                    {
                        case VertexDecl.GU_WEIGHT_8BIT:
                        {
                            for (int w = 0; w < weightCount; ++w)
                                data.weights[w] = ((float) stream.i8()) / 0x7f;
                            break;
                        }
                        case 0:
                            break;
                        default:
                            throw new RuntimeException("Unhandled weight type!");
                    }

                    switch (vertexType & VertexDecl.GU_TEXTURE_BITS)
                    {
                        case VertexDecl.GU_TEXTURE_8BIT:
                        {
                            data.uv = new Vector2f(
                                ((float) stream.i8()) / 0x7f,
                                ((float) stream.i8()) / 0x7f
                            );
                            break;
                        }
                        case VertexDecl.GU_TEXTURE_16BIT:
                        {
                            stream.align(2);
                            data.uv = new Vector2f(
                                ((float) stream.i16()) / 0x7fff,
                                ((float) stream.i16()) / 0x7fff
                            );
                            break;
                        }
                        case VertexDecl.GU_TEXTURE_32BITF:
                        {
                            stream.align(4);
                            data.uv = new Vector2f(stream.f32(), stream.f32());
                            break;
                        }
                    }

                    // Not bothering to parse the colors for now.
                    switch (vertexType & VertexDecl.GU_COLOR_BITS)
                    {
                        case VertexDecl.GU_COLOR_5650:
                        case VertexDecl.GU_COLOR_5551:
                        case VertexDecl.GU_COLOR_4444:
                            stream.align(2);
                            stream.u16();
                            break;
                        case VertexDecl.GU_COLOR_8888:
                            stream.align(4);
                            stream.u32();
                            break;
                    }

                    switch (vertexType & VertexDecl.GU_NORMAL_BITS)
                    {
                        case VertexDecl.GU_NORMAL_8BIT:
                        {
                            data.normal = new Vector3f(
                                ((float) stream.i8()) / 0x7f,
                                ((float) stream.i8()) / 0x7f,
                                ((float) stream.i8()) / 0x7f
                            );
                            break;
                        }
                        case VertexDecl.GU_NORMAL_16BIT:
                        {
                            stream.align(2);
                            data.normal = new Vector3f(
                                ((float) stream.u16()) / 0xffff,
                                ((float) stream.u16()) / 0xffff,
                                ((float) stream.u16()) / 0xffff
                            );
                            break;
                        }
                        case VertexDecl.GU_NORMAL_32BITF:
                        {
                            stream.align(4);
                            data.normal = new Vector3f(
                                stream.f32(),
                                stream.f32(),
                                stream.f32()
                            );
                            break;
                        }
                    }

                    switch (vertexType & VertexDecl.GU_VERTEX_BITS)
                    {
                        case VertexDecl.GU_VERTEX_8BIT:
                        {
                            data.position = new Vector3f(
                                ((float) stream.i8()) / 0x7f,
                                ((float) stream.i8()) / 0x7f,
                                ((float) stream.i8()) / 0x7f
                            );
                            break;
                        }
                        case VertexDecl.GU_VERTEX_16BIT:
                        {
                            stream.align(2);
                            data.position = new Vector3f(
                                ((float) stream.i16()) / 0x7fff,
                                ((float) stream.i16()) / 0x7fff,
                                ((float) stream.i16()) / 0x7fff
                            );
                            break;
                        }
                        case VertexDecl.GU_VERTEX_32BITF:
                        {
                            stream.align(4);
                            data.position = new Vector3f(
                                stream.f32(),
                                stream.f32(),
                                stream.f32()
                            );
                            break;
                        }
                    }

                    streams[j][lastVertexOffset + i] = data;
                }
            }

            if (bakeTextureTransforms && ((vertexType & VertexDecl.GU_TEXTURE_BITS) != 0))
            {
                for (int i = lastVertexOffset; i < lastVertexOffset + info.getNumVerts(); ++i)
                {
                    for (int j = 0; j < numVertsPerVertex; ++j)
                    {
                        VertexData data = streams[j][i];
                        if (skin != null)
                        {
                            data.uv.mul(skin.getTextureScale());
                            data.uv.add(skin.getTextureOffset());
                        }
                        else
                        {
                            data.uv.mul(this.textureScale);
                            data.uv.add(this.textureOffset);
                        }
                    }
                }
            }

            lastVertexOffset += info.getNumVerts();
        }


        return streams;
    }
}