package cambridge.io;

import cambridge.io.streams.MemoryInputStream;

/**
 * Interface that specifies that an object
 * can be serialized as a pure data binary
 * structure.
 */
public interface Serializable
{

    /**
     * (De)serializes a structure that implements Serializable.
     *
     * @param stream Serializer instance
     */
    void load(MemoryInputStream stream);

    default void load(MemoryInputStream stream, int size)
    {
        load(stream);
    }
}
