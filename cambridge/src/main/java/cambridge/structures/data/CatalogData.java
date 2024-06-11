package cambridge.structures.data;

import cambridge.enums.ItemType;
import cambridge.enums.ResourceType;
import cambridge.io.Serializable;
import cambridge.io.streams.MemoryInputStream;
import cambridge.io.streams.MemoryOutputStream;
import cambridge.resources.Archive;
import cambridge.resources.Archive.ArchiveEntry;
import cambridge.util.FileIO;
import cwlib.util.Bytes;

import java.io.File;
import java.util.ArrayList;
import java.util.Scanner;

public class CatalogData implements Serializable
{
    public ResourceType type;
    public int size;
    public ItemType itemType;
    public int dlcIndex;

    public transient int flags;

    public String archive;
    public String resource;


    @Override
    public void load(MemoryInputStream stream)
    {
        this.type = ResourceType.fromUID(stream.i32());
        this.size = stream.i32();
        this.itemType = ItemType.fromValue(stream.i32());
        this.dlcIndex = stream.i32();

        int stringOffset = stream.getOffset() + stream.i32();
        int archiveOffset = stream.getOffset() + stream.i32();

        flags = stream.u8(); // ???

        this.resource = stream.at(stringOffset).str(stream.u8());
        this.archive = stream.at(archiveOffset).str(stream.u8());

        stream.u8(); // Probably just padding
    }

    public static void main(String[] args)
    throws Exception
    {
        Scanner scanner = new Scanner(new File(args[0]));

        String line = scanner.nextLine().trim();
        if (line == null)
        {
            System.out.println("a nice message");
            return;
        }

        File memstick = null;
        ArrayList<Archive> archives = new ArrayList<>();
        while (line.startsWith(";"))
        {

            String path = line.split(";")[1].trim();

            if (new File(path).isDirectory())
            {
                memstick = new File(path);
            }
            else
            {
                archives.add(new Archive(line.split(";")[1].trim()));
            }

            line = scanner.nextLine().trim();
        }

        ArrayList<CatalogData> catalogDatas = new ArrayList<>();

        while (line != null)
        {

            if (!line.isEmpty() && !line.startsWith("#"))
            {
                ArchiveEntry entry = null;
                File source = null;
                for (Archive archive : archives)
                {
                    entry = archive.get(line);
                    if (entry != null)
                    {
                        source = archive.source;
                        break;
                    }
                }

                if (entry == null && memstick != null && line.contains("memstick_profile"))
                {
                    String name = new File(line).getName();
                    System.out.println(name);
                    File dataFile = new File(memstick, name);
                    if (dataFile.exists())
                    {
                        CatalogData data = new CatalogData();
                        data.resource = Archive.resolve(line).substring(1);
                        data.size = (int) dataFile.length();
                        if (data.resource.endsWith(".mip"))
                            data.type = ResourceType.TEXTURE;
                        else if (data.resource.endsWith(".level.biff") || data.resource.endsWith(
                            ".lbf"))
                            data.type = ResourceType.LEVEL_BIFF;
                        else if (data.resource.endsWith(".object.biff") || data.resource.endsWith(".obf"))
                            data.type = ResourceType.OBJECT_BIFF;
                        else if (data.resource.endsWith(".catalog.biff") || data.resource.endsWith(".cbf"))
                            data.type = ResourceType.CATALOG_BIFF;
                        catalogDatas.add(data);
                    }
                }

                if (entry != null)
                {
                    CatalogData data = new CatalogData();
                    data.resource = Archive.resolve(line).substring(1);
                    data.size = entry.getSize();


                    if (data.resource.endsWith(".anim"))
                        data.type = ResourceType.ANIMATION;
                    else if (data.resource.endsWith(".skeleton"))
                        data.type = ResourceType.SKELETON;
                    else if (data.resource.contains(".skin"))
                        data.type = ResourceType.SKIN;
                    else if (data.resource.endsWith(".model"))
                        data.type = ResourceType.MODEL;
                    else if (data.resource.endsWith(".mip"))
                        data.type = ResourceType.TEXTURE;
                    else if (data.resource.endsWith(".level.biff") || data.resource.endsWith(
                        ".lbf"))
                        data.type = ResourceType.LEVEL_BIFF;
                    else if (data.resource.endsWith(".object.biff") || data.resource.endsWith(
                        ".obf"))
                        data.type = ResourceType.OBJECT_BIFF;
                    else if (data.resource.endsWith(".materials.biff"))
                        data.type = ResourceType.MATERIAL_BIFF;
                    else if (data.resource.endsWith(".catalog.biff") || data.resource.endsWith(
                        ".cbf"))
                        data.type = ResourceType.CATALOG_BIFF;
                    else if (data.resource.endsWith(".background.biff"))
                        data.type = ResourceType.BACKGROUND_BIFF;
                    else if (data.resource.endsWith(".meta.biff"))
                    {
                        data.type = ResourceType.POPIT_METADATA;
                        data.itemType = ItemType.STICKER;
                    }
                    else if (data.resource.endsWith(".arc"))
                        data.type = ResourceType.ARCHIVE;


                    catalogDatas.add(data);
                }


            }

            if (scanner.hasNextLine())
                line = scanner.nextLine().trim();
            else
                line = null;
        }


        int sourceSize =
            catalogDatas.stream().mapToInt(element -> element.resource.length()).reduce(0,
                Integer::sum);

        MemoryOutputStream resourceStream = new MemoryOutputStream(0x1c * catalogDatas.size());
        MemoryOutputStream textStream = new MemoryOutputStream(sourceSize);
        MemoryOutputStream catalogStream = new MemoryOutputStream(catalogDatas.size() * 0x4);
        MemoryOutputStream domStream =
            new MemoryOutputStream(0xc + (0x18 * catalogDatas.size()) + 0x1c);

        domStream.str("ATG ", 4);
        domStream.i32(-1);
        domStream.i32(sourceSize - 0x10);

        for (CatalogData data : catalogDatas)
        {
            domStream.str("RSRC", 4);
            domStream.i32(-1);
            domStream.i32(0xC);
            domStream.str("DATA", 4);
            domStream.i32(resourceStream.getOffset());
            domStream.i32(0x1c);

            resourceStream.i32(data.type.getUID());
            resourceStream.i32(data.size);
            resourceStream.pad(0x8);


            catalogStream.i32(-((resourceStream.getLength() - resourceStream.getOffset()) + textStream.getLength() + catalogStream.getOffset()));

            resourceStream.i32((textStream.getOffset() + resourceStream.getLength()) - resourceStream.getOffset());

            resourceStream.pad(0x5);
            resourceStream.u8(data.resource.length());
            resourceStream.u16(0);

            textStream.str(data.resource, data.resource.length());
        }

        domStream.str("TEXT", 4);
        domStream.i32(resourceStream.getLength());
        domStream.i32(textStream.getLength());

        domStream.str("CAT1", 4);
        domStream.i32(resourceStream.getLength() + textStream.getLength());
        domStream.i32(catalogStream.getLength());

        domStream.i32(resourceStream.getLength() + textStream.getLength() + catalogStream.getLength());

        FileIO.write(args[1], Bytes.combine(
            resourceStream.getBuffer(),
            textStream.getBuffer(),
            catalogStream.getBuffer(),
            domStream.getBuffer()
        ));


    }
}