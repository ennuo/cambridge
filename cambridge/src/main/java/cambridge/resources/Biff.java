package cambridge.resources;

import cambridge.enums.ElementType;
import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import cambridge.util.Bytes;
import cambridge.util.FileIO;

import java.util.ArrayList;
import java.util.Arrays;

public class Biff
{
    public static class Element
    {
        private transient byte[] source;
        public String id;
        private int offset;
        private int size;

        private final ArrayList<Element> children = new ArrayList<>();

        public static Element at(byte[] biff, int offset)
        {
            Element element = new Element();

            element.source = biff;
            element.id = new String(biff, offset, 4);
            element.offset = Bytes.toIntegerLE(biff, offset + 4);
            element.size = Bytes.toIntegerLE(biff, offset + 8);

            if (element.offset != -1) return element;

            int start = offset + 0xc;
            int end = start + element.size;
            offset = start;

            while (offset < end)
            {
                Element child = Element.at(biff, offset);
                if (child.offset == -1)
                    offset += child.size;
                element.children.add(child);
                offset += 0xc;
            }

            return element;
        }

        public ElementType getType()
        {
            return ElementType.get(this.id);
        }

        public ArrayList<Element> getChildren()
        {
            return this.children;
        }

        public byte[] getData()
        {
            return Arrays.copyOfRange(this.source, this.offset, this.offset + this.size);
        }

        public int getOffset()
        {
            return offset;
        }

        @SuppressWarnings("unchecked")
        public <T extends Serializable> T load()
        {
            ElementType type = this.getType();
            if (type == ElementType.INVALID) return null;
            Class<T> clazz = (Class<T>) type.getSerializable();
            if (clazz == null) return null;
            T structure = null;
            try { structure = clazz.getDeclaredConstructor().newInstance(); }
            catch (Exception ex) { return null; }
            structure.load(new MemoryInputStream(this.source).at(this.offset), this.size);
            return structure;
        }
    }

    public ArrayList<Element> elements = new ArrayList<>();

    public Biff(String path)
    {
        this.load(FileIO.read(path));
    }

    public Biff(byte[] data)
    {
        this.load(data);
    }

    private ArrayList<Element> getAll(ElementType type, ArrayList<Element> elements,
                                      ArrayList<Element> children)
    {
        for (Element element : children)
        {
            if (type == null || element.getType().equals(type))
                elements.add(element);
            this.getAll(type, elements, element.getChildren());
        }
        return elements;
    }

    public <T extends Serializable> T get(ElementType type)
    {
        ArrayList<Element> elements = this.getAll(type);
        if (elements.size() == 0) return null;
        return elements.get(0).load();
    }

    public ArrayList<Element> getAll(ElementType type)
    {
        return this.getAll(type, new ArrayList<>(), this.elements);
    }

    private void load(byte[] biff)
    {
        if (biff == null)
            throw new NullPointerException("No data provided to Biff::load");

        int table = Bytes.toIntegerLE(biff, biff.length - 4);
        this.elements = Element.at(biff, table).getChildren();
    }
}
