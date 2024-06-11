package cambridge.resources.model;

import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import cambridge.io.streams.MemoryInputStream.SeekMode;

import java.awt.image.BufferedImage;

public class Texture implements Serializable
{
    private static int unswizzle(int offset, int log2w)
    {
        if (log2w <= 4) return offset;

        int wMask = (1 << log2w) - 1;

        int mx = offset & 0xf;
        int by = offset & (~7 << log2w);
        int bx = offset & wMask & ~0xf;
        int my = offset & (7 << log2w);
        return by | (bx << 3) | (my >> (log2w - 4)) | mx;
    }

    private boolean alpha;
    private BufferedImage image;

    public Texture(byte[] data)
    {
        this.load(new MemoryInputStream(data));
    }

    public BufferedImage getImage()
    {
        return this.image;
    }

    public boolean isAlpha()
    {
        return this.alpha;
    }

    @Override
    public void load(MemoryInputStream stream)
    {
        int clutOffset = stream.i32();
        int width = stream.i32();
        int height = stream.i32();
        int bpp = stream.u8();
        int numBlocks = stream.u8();
        int texMode = stream.u8();
        this.alpha = stream.u8() == 1;
        int dataOffset = stream.i32();

        stream.seek(clutOffset - stream.getOffset(), SeekMode.Relative);
        int[] clut = new int[(dataOffset - clutOffset) / 4];
        for (int i = 0; i < clut.length; ++i)
        {
            clut[i] =
                (stream.u8() << 16) |
                (stream.u8() << 8) |
                stream.u8() |
                (stream.u8() << 24);
        }

        byte[] texData = stream.bytes(stream.getLength() - stream.getOffset());
        this.image = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);

        int log2w = (int) (Math.log((width * bpp) >> 3) / Math.log(2));
        int offset = 0;
        if (bpp == 4)
        {
            for (int y = height - 1; y >= 0; --y)
            {
                for (int x = 0; x < width; x += 2)
                {
                    int index = texData[unswizzle(offset++, log2w)] & 0xff;
                    this.image.setRGB(x, y, clut[index & 0xf]);
                    this.image.setRGB(x + 1, y, clut[index >> 4]);
                }
            }
        }
        else if (bpp == 8)
        {
            for (int y = height - 1; y >= 0; --y)
                for (int x = 0; x < width; ++x)
                    this.image.setRGB(x, y, clut[texData[unswizzle(offset++, log2w)] & 0xff]);
        }
        else throw new RuntimeException("Unhandled BPP!");
    }
}
