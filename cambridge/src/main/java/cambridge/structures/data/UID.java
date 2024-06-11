package cambridge.structures.data;

import cambridge.util.Bytes;
import cambridge.util.Crypto;

public class UID
{
    private final int hash;
    private final String tag;

    public UID(int hash)
    {
        this.hash = hash;
        this.tag = "#" + Bytes.toHex(hash).toUpperCase();
    }

    public UID(String tag)
    {
        this.tag = tag;
        if (this.tag.startsWith("#"))
            this.hash = Bytes.toIntegerBE(Bytes.fromHex(this.tag.substring(1)));
        else
            this.hash = Crypto.crc32(tag);
    }

    public String getTag()
    {
        return this.tag;
    }

    public int getHash()
    {
        return this.hash;
    }

    @Override
    public boolean equals(Object other)
    {
        if (other == this) return true;

        if (other instanceof UID)
            return ((UID) other).hash == this.hash;

        if (other instanceof Integer)
            return other.equals(this.hash);

        if (other instanceof String)
            return other.equals(this.tag);

        return false;
    }

    @Override
    public int hashCode()
    {
        return this.hash;
    }

    @Override
    public String toString()
    {
        return this.tag;
    }
}
