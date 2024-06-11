package cambridge.craftworld;

import cwlib.enums.ResourceType;
import cwlib.types.data.ResourceDescriptor;
import cwlib.util.GsonUtils;

import java.io.File;
import java.lang.reflect.Type;
import java.util.HashMap;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;

import cambridge.io.MaterialLookupTableSerializer;
import cambridge.util.Crypto;
import cambridge.util.FileIO;

public class MaterialLookupData
{
    public static HashMap<Integer, MaterialLookupData> MLUT = new HashMap<>();
    public static HashMap<Integer, String> CACHED_MATERIAL_UID_MAP = new HashMap<>();

    public static final int DEFAULT_MATERIAL_UID = 0xef01e39c;

    static
    {
        // Parse a CSV of all known materials in retail for JSON serialization.
        String csv = FileIO.getResourceFileAsString("/lists/materials.csv");
        for (String line : csv.split("\n"))
        {
            line = line.trim();
            if (line.startsWith("#") || line.isEmpty()) continue;

            String[] columns = line.split(",");

            int uid = Integer.parseUnsignedInt(columns[0].trim());
            String path = columns[1].trim();

            CACHED_MATERIAL_UID_MAP.put(uid, path);
        }

        // Load the MLUT from file
        File configFilePath = new File(FileIO.JAR_DIRECTORY, "mlut.json");
        if (configFilePath.exists())
        {
            Type type = new TypeToken<HashMap<Integer, MaterialLookupData>>() { }.getType();
            Gson gson = 
                new GsonBuilder()
                    .setPrettyPrinting()
                    .registerTypeAdapter(type, new MaterialLookupTableSerializer())
                    .create();

            String json = new String(FileIO.read(configFilePath.getAbsolutePath()));

            MLUT = gson.fromJson(json, type);
        }
        else loadDefaultMaterialLookupTable();
    }
    
    private static void loadDefaultMaterialLookupTable()
    {
        // Cardboard
        MLUT.put(0xef01e39c, new MaterialLookupData(10724, 10803, 10813));

        // Wood
        MLUT.put(0xd3ec228b, new MaterialLookupData(10717, 10811, 10790));

        // Sponge
        MLUT.put(0xc30e5657, new MaterialLookupData(10719, 10797, 19415));

        // Metal
        MLUT.put(0x4afec1b, new MaterialLookupData(10716, 10810));

        // Polystyrene
        MLUT.put(0x92eef9bf, new MaterialLookupData(10718, 10798, 10790));

        // Glass
        MLUT.put(0xaadab9f5, new MaterialLookupData(10725, 10805, 10790));

        // Pink Floaty
        MLUT.put(0x9f972dd, new MaterialLookupData(21166, 22476, 19415));

        // Peach Floaty
        MLUT.put(0x7ed83093, new MaterialLookupData(21165, 51424, 19415));

        // Dissolve
        MLUT.put(0x31b4eb0e, new MaterialLookupData(22011, 27500));

        // Stone / Rock
        MLUT.put(0xbb31663d, new MaterialLookupData(26602, 26637, 10790));

        // Damask
        MLUT.put(0x364d5c24, new MaterialLookupData(10719, 16670, 10823));
    }

    public ResourceDescriptor physicsResource;
    public ResourceDescriptor staticPhysicsResource;
    public ResourceDescriptor renderMaterial;
    public ResourceDescriptor bevel;
    public float bevelSize = 10.0f;

    public MaterialLookupData()
    {
        this.physicsResource = null;
        this.renderMaterial = null;
        this.bevel = null;
    }

    public MaterialLookupData(long physics, long render, long bevel)
    {
        if (physics != -1)
            this.physicsResource = new ResourceDescriptor(physics, ResourceType.MATERIAL);
        if (render != -1)
            this.renderMaterial = new ResourceDescriptor(render, ResourceType.GFX_MATERIAL);
        if (bevel != -1)
            this.bevel = new ResourceDescriptor(bevel, ResourceType.BEVEL);
        else
            this.bevel = null;
    }
    
    public MaterialLookupData(long physics, long render)
    {
        this(physics, render, -1);
    }
}
