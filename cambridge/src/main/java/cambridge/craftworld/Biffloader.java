package cambridge.craftworld;

import cambridge.enums.ElementType;
import cambridge.enums.ItemType;
import cambridge.enums.LanguageID;
import cambridge.resources.*;
import cambridge.resources.Archive.ArchiveEntry;
import cambridge.resources.Biff.Element;
import cambridge.resources.model.Material;
import cambridge.resources.model.Model;
import cambridge.structures.*;
import cambridge.structures.MaterialManager.RenderMaterial;
import cambridge.structures.Slots.FrontendSlot;
import cambridge.structures.data.StoryLevel;
import cambridge.structures.data.StoryLevel.LevelType;
import cambridge.structures.data.UID;
import cambridge.util.Bytes;
import cambridge.util.Crypto;
import cambridge.util.FileIO;
import cwlib.CwlibConfiguration;
import cwlib.enums.*;
import cwlib.io.GatherVariables;
import cwlib.io.Resource;
import cwlib.resources.*;
import cwlib.singleton.ResourceSystem;
import cwlib.singleton.ResourceSystem.ResourceLogLevel;
import cwlib.structs.dlc.DLCFile;
import cwlib.structs.slot.Pack;
import cwlib.structs.slot.Slot;
import cwlib.structs.slot.SlotID;
import cwlib.structs.things.Thing;
import cwlib.structs.things.components.EggLink;
import cwlib.structs.things.parts.*;
import cwlib.types.SerializedResource;
import cwlib.types.archives.FileArchive;
import cwlib.types.archives.SaveArchive;
import cwlib.types.data.GUID;
import cwlib.types.data.NetworkPlayerID;
import cwlib.types.data.ResourceDescriptor;
import cwlib.types.data.Revision;
import cwlib.types.data.SHA1;
import cwlib.types.databases.FileDB;
import cwlib.types.databases.FileDBRow;
import cwlib.util.Colors;
import cwlib.util.GsonUtils;
import cwlib.util.gfx.CgAssembler;
import cwlib.util.gfx.GfxAssembler;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaterniond;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import com.google.gson.GsonBuilder;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.regex.Pattern;

import javax.print.attribute.standard.Compression;

public class Biffloader
{
    /**
     * The target game revision used in conversion.
     */
    public static final Revision GAME_REVISION = 
        new Revision(Branch.LEERDAMMER.getHead(), Branch.LEERDAMMER.getID(), Revisions.LD_LAMS_KEYS);

    /**
     * The default author for all PSP assets.
     */
    public static final String DEFAULT_AUTHOR = "SCEE_Cambridge";

    /**
     * The default author network player ID for all PSP assets.
     */
    public static final NetworkPlayerID DEFAULT_AUTHOR_ID = new NetworkPlayerID(DEFAULT_AUTHOR);

    /**
     * The content ID for the Turbo! DLC Pack.
     */
    public static final String TURBO_CONTENT_ID = "LBPPDLCSONYPK001";

    /**
     * The theme category used for Turbo! asssets.
     */
    public static final String TURBO_THEME_ID = "suburban";

    private static final long E_DOWNLOADABLES_KEY = 61688;
    private static final long E_CONTENT_PACKS_KEY = 66087;
    private static final long E_KEYS_TRANS_KEY = 69253;
    private static final long E_LEVEL_EXIT_SCRIPT_KEY = 4255486038L;
    private static final long E_CAMBRIDGE_WORLD_SCRIPT_KEY = 2762666099L;

    private final String looseDirectory;
    private final FileDB gameDB;
    private final ArrayList<FileArchive> gameCaches = new ArrayList<>();
    private final FileArchive pspCache;
    private final FileDB pspDB;
    private final Archive[] pspArchives;

    private final HashMap<PS3Asset, RPlan> gameAssets = new HashMap<>();
    private final ArrayList<TranslationUnit> translationData = new ArrayList<>();
    private final RTranslationTable keyCache;

    private final RDLC downloadables;
    private final RPacks contentPacks;

    private final HashMap<Integer, Group> popitGroups = new HashMap<>();

    public Biffloader(String looseDirectory, String pspCache, String pspDatabase, String[] pspArchives)
    {
        File gameDatabaseFile = new File(looseDirectory, "output/blurayguids.map");
        try
        {
            if (!gameDatabaseFile.exists())
            {
                System.err.println("Can't construct Biffloader instance since " + gameDatabaseFile.getAbsolutePath() + " does not exist!");
                System.exit(1);
            }

            this.gameDB = new FileDB(gameDatabaseFile);
        }
        catch (Exception ex)
        {
            System.err.println("An error occurred while parsing FileDB from " + gameDatabaseFile.getAbsolutePath());
            throw ex;
        }

        if (!looseDirectory.endsWith("/"))
            looseDirectory += "/";
        this.looseDirectory = looseDirectory;

        // Search for all FARC files in the USRDIR folder so we can actually load resources from the database
        File[] cacheFiles = new File(looseDirectory).listFiles((dir, name) -> name.toLowerCase().endsWith(".farc"));
        for (File cache : cacheFiles)
        {
            try
            {
                gameCaches.add(new FileArchive(cache));
            }
            catch (Exception ex)
            {
                System.err.println("An error occurred while reading cache from " + cache.getAbsolutePath());
                throw ex;
            }
        }

        // If a patch file exists, put it atop of our blurayguids.map
        File patchDatabaseFile = new File(looseDirectory, "output/brg_patch.map");
        if (patchDatabaseFile.exists())
        {
            try
            {
                FileDB patchDB = new FileDB(patchDatabaseFile);
                gameDB.patch(patchDB);
            }
            catch (Exception ex)
            {
                System.err.println("An error occurred while parsing patch FileDB from " + patchDatabaseFile.getAbsolutePath());
                throw ex;
            }
        }

        File pspDatabaseFile = new File(pspDatabase);
        if (pspDatabaseFile.exists())
        {
            try
            {
                pspDB = new FileDB(pspDatabaseFile);
            }
            catch (Exception ex)
            {
                System.err.println("An error occurred while parsing existing PSP FileDB from " + pspDatabaseFile.getAbsolutePath());
                throw ex;
            }
        }
        else pspDB = new FileDB(0x100);

        File pspCacheFile = new File(pspCache);
        
        // If the cache doesn't exist, just write an empty one at the location
        if (!pspCacheFile.exists())
        {
            FileIO.write(pspCache, new byte[] { 0x00, 0x00, 0x00, 0x00, 0x46, 0x41, 0x52, 0x43 });
        }

        try
        {
            this.pspCache = new FileArchive(pspCacheFile);
        }
        catch (Exception ex)
        {
            System.err.println("An error occurred while reading cache from " + pspCacheFile.getAbsolutePath());
            throw ex;
        }

        // Sneak psp cache into the game cache array
        gameCaches.add(this.pspCache);

        this.pspArchives = new Archive[pspArchives.length];
        for (int i = 0; i < pspArchives.length; ++i)
            this.pspArchives[i] = new Archive(pspArchives[i]);

        
        for (PS3Asset asset : PS3Asset.values())
            this.gameAssets.put(asset, this.loadPS3Resource(asset.getGUID(), RPlan.class));

        // Attempt to load all language data from the game for all regions
        for (int i = 0; i < LanguageID.MAX; ++i)
            registerTranslationUnit(i, LanguageID.NAMES[i]);
        registerTranslationUnit(LanguageID.SPANISH, "us spanish");
        registerTranslationUnit(LanguageID.FRENCH, "us french");

        byte[] keyTransData = extractPS3Data(E_KEYS_TRANS_KEY);
        if (keyTransData != null)
            this.keyCache = new RTranslationTable(keyTransData);
        else this.keyCache = new RTranslationTable();

        // Try to load DLC data
        byte[] downloadableData = extractPS3Data(E_DOWNLOADABLES_KEY);
        if (downloadableData != null)
        {
            SerializedResource resource = new SerializedResource(downloadableData);
            if (resource.getSerializationType().equals(SerializationType.TEXT))
            {
                GatherVariables gatherVariables = GatherVariables.getEmptyDownloadableContentGatherable();
                GatherVariables.Load(resource.getStream().getBuffer(), gatherVariables);
                downloadables = (RDLC) gatherVariables.getObjectForReflection();
            }
            else downloadables = resource.loadResource(RDLC.class);
        }
        else downloadables = new RDLC();

        byte[] contentPackData = extractPS3Data(E_CONTENT_PACKS_KEY);
        if (contentPackData != null)
            contentPacks = new SerializedResource(contentPackData).loadResource(RPacks.class);
        else
            contentPacks = new RPacks();

        // Lock the Turbo! DLC
        ArrayList<GUID> guids = new ArrayList<>();
        for (String path : Archive.PATHS.values())
        {
            path = Archive.resolve(path);
            if (path.endsWith(".meta.biff") && path.contains(TURBO_THEME_ID))
            {
                GUID guid = Crypto.makeGUID(("gamedata/psp" + path).replace(".meta.biff", ".plan"));
                guids.add(guid);
                downloadables.addGUID(guid, DLCFileFlags.NONE);
            }
        }

        guids.sort((a, z) -> Long.compareUnsigned(a.getValue(), z.getValue()));
        DLCFile file = downloadables.getOrCreateFile(TURBO_CONTENT_ID);
        file.file = "petrolheadscreatorpack.edat";
        file.guids = guids;
        file.flags = DLCFileFlags.AUTO_ADD_DISABLED;

        // Make sure the level exit script exists
        createEntryFromData("gamedata/scripts/level_exit.ff", E_LEVEL_EXIT_SCRIPT_KEY, FileIO.getResourceFile("/scripts/level_exit.ff"));
        // createEntryFromData("gamedata/scripts/cambridgeworld.ff", E_CAMBRIDGE_WORLD_SCRIPT_KEY, FileIO.getResourceFile("/scripts/cambridgeworld.ff"));

        for (String path : Archive.PATHS.values())
        {
            if (!path.endsWith(".grp")) continue;
            byte[] data = this.getPSPResource(path);
            if (data != null)
            {
                String name = new File(path).getName().split("\\.")[0].toUpperCase();
                Group group = new Group(name, data);
                this.popitGroups.put(group.uid, group);
            }
        }

        this.preload();
        System.out.println("\n\n\n\n=== FINISHED PRE-LOADING RESOURCES ===\n\n\n\n");
    }

    private void registerTranslationUnit(int languageId, String ps3Name)
    {
        FileDBRow row = gameDB.get(String.format("gamedata/languages/%s.trans", ps3Name));
        if (row == null) return;
        byte[] ps3RowData = extractPS3Data(row.getSHA1());
        if (ps3RowData == null) return;

        TranslationUnit unit = new TranslationUnit();
        unit.id = languageId;
        unit.guid = row.getGUID();
        unit.name = ps3Name;
        unit.ps3TranslationData = new RTranslationTable(ps3RowData);
        unit.pspTranslationData = new Translations(getPSPResource(String.format("lams/%s/global" +
                                                                                ".lng",
            LanguageID.NAMES[languageId])));

        translationData.add(unit);
    }

    public Group getPopitGroup(int uid)
    {
        return this.popitGroups.get(uid);
    }

    public long addTranslationTag(String tag, int index)
    {
        long key = RTranslationTable.makeLamsKeyID(tag);
        keyCache.add(key, tag);
        for (TranslationUnit unit : translationData)
            unit.ps3TranslationData.add(key, unit.pspTranslationData.get(index));
        return key;
    }

    public long addTranslationTag(int languageId, String tag, String value)
    {
        long key = RTranslationTable.makeLamsKeyID(tag);
        keyCache.add(key, tag);
        for (TranslationUnit unit : translationData)
        {
            if (unit.id == languageId)
            {
                unit.ps3TranslationData.add(key, value);
            }
        }

        return key;
    }

    public byte[] getPSPResource(String path)
    {
        for (Archive archive : this.pspArchives)
        {
            byte[] data = archive.extract(path);
            if (data != null)
                return data;
        }
        return null;
    }

    public ArchiveEntry getPSPIndex(String path)
    {
        for (Archive archive : this.pspArchives)
        {
            ArchiveEntry data = archive.get(path);
            if (data != null)
                return data;
        }
        return null;
    }

    public byte[] extractPS3Data(SHA1 sha1)
    {
        for (FileArchive cache : gameCaches)
        {
            byte[] data = cache.extract(sha1);
            if (data != null)
                return data;
        }

        return null;
    }

    public byte[] extractPS3Data(long guid)
    {
        FileDBRow row = gameDB.get(guid);
        if (row == null)
            row = pspDB.get(guid);
        if (row == null)
            return null;

        byte[] data = extractPS3Data(row.getSHA1());
        if (data != null) return data;

        File looseFilePath = new File(looseDirectory, row.getPath());
        if (looseFilePath.exists())
            return FileIO.read(looseFilePath.getAbsolutePath());

        return null;
    }

    public <T extends Resource> T loadPS3Resource(long guid, Class<T> clazz)
    {
        byte[] data = extractPS3Data(guid);
        if (data == null) return null;
        SerializedResource resource = new SerializedResource(data);
        return resource.loadResource(clazz);
    }

    public Thing[] getGameAsset(LoadContext context, PS3Asset asset)
    {
        if (asset == null) return null;
        Thing[] things = this.gameAssets.get(asset).getThings();
        for (Thing thing : things)
        {
            if (thing == null) continue;
            context.reassignThing(thing);
            thing.setPart(Part.REF, null);
            thing.setPart(Part.METADATA, null);
        }
        return things;
    }

    public FileDBRow createZeroedEntry(String path, long guid)
    {
        FileDBRow row = this.pspDB.get(guid);
        if (row != null) row.setDetails((byte[]) null);
        else row = this.pspDB.newFileDBRow(path, guid);
        return row;
    }

    public void createEntryFromData(String path, long guid, byte[] data)
    {
        FileDBRow row = this.createZeroedEntry(path, guid);
        row.setDetails(data);
        this.pspCache.add(data);
    }

    private void preload()
    {
        LoadContext context = new LoadContext(this, null);

        // Diffuse + Alpha = g9842 (teeth_01.gmat)
        // Diffuse + Alpha + DiffuseMultipliedSpecular = g65946 (cart_wheel.gmat)
        // Diffuse = g29847 (butterfly.gmat)

        SerializedResource resource = new SerializedResource(extractPS3Data(65946));
        RGfxMaterial defaultMaterial = resource.loadResource(RGfxMaterial.class);

        // Preload all material data for shapes
        for (String path : Archive.PATHS.values())
        {
            if (!path.endsWith(".materials.biff")) continue;
            byte[] biffData = this.getPSPResource(path);

            if (biffData == null) continue;
            Biff biff = new Biff(biffData);
            MaterialManager manager = biff.getAll(ElementType.MATERIAL_MANAGER).get(0).load();

            for (RenderMaterial material : manager.materials)
            {
                MaterialLookupData data = MaterialLookupData.MLUT.get(material.uid.getHash());
                if (data == null)
                {
                    data = new MaterialLookupData();
                    MaterialLookupData.MLUT.put(material.uid.getHash(), data);
                }


                long physicsMaterialKey;
                long staticPhysicsMaterialKey = 11987;

                String category = material.texture.split("\\\\")[1];
                switch (category)
                {
                    case "Cardboard":
                        physicsMaterialKey = 66927; break;
                    case "Metal": 
                        physicsMaterialKey = 10716; break;
                    case "Sponge":
                        physicsMaterialKey = 10719; break;
                    case "Stone":
                        physicsMaterialKey = 26602; break;
                    case "Wood":
                        physicsMaterialKey = 10717; break;
                    case "Polystyrene": 
                        physicsMaterialKey = 10718; break;
                    case "Glass":
                        physicsMaterialKey = 10725; break;
                    case "PeachBubble":
                        physicsMaterialKey = 21165; break;
                    case "PinkBubble":
                        physicsMaterialKey = 21166; break;
                    case "Balloon":
                        physicsMaterialKey = 12744; break;
                    case "Dissolve":
                        physicsMaterialKey = 22011; break;
                    case "Rubber":
                        physicsMaterialKey = 16362; break;
                    default:
                        physicsMaterialKey = 3520; break;
                }

                if (data.physicsResource == null)
                    data.physicsResource = new ResourceDescriptor(physicsMaterialKey, ResourceType.MATERIAL);

                if (data.staticPhysicsResource == null)
                    data.staticPhysicsResource = new ResourceDescriptor(staticPhysicsMaterialKey, ResourceType.MATERIAL);
                
                if (data.renderMaterial == null)
                {
                    path = "gamedata/psp/gmats/" + material.texture.replaceAll("\\\\", "/").toLowerCase().replace(".mip", ".gmat");
                    GUID guid = Crypto.makeGUID(path);
                    if (this.pspDB.get(guid) == null)
                    {
                        defaultMaterial.textures[0] = ResourceConverter.getTexture(context,
                            material.texture);
                        this.createEntryFromData(path, guid.getValue(),
                            SerializedResource.compress(defaultMaterial.build(resource.getRevision(),
                                resource.getCompressionFlags())));
                    }

                    data.renderMaterial = new ResourceDescriptor(guid, ResourceType.GFX_MATERIAL);
                }

                if (data.bevel == null)
                {
                    if (material.bevelTexture != null && !material.bevelTexture.isEmpty())
                        data.bevel = ResourceConverter.getBevel(context, material.bevelTexture);
                }
            }
        }

        // Pre-load all inventory items
        // Mainly for sticker switches, but good to have them all
        for (String path : Archive.PATHS.values())
        {
            if (path.endsWith(".model"))
            {
                try
                {
                    ResourceConverter.getMesh(context, path, null);
                }
                catch (Exception ex)
                {
                    System.out.println("Failed to load : " + path);
                }
                continue;
            }

            if (!path.endsWith(".meta.biff")) continue;

            ResourceConverter.getItem(context, path);
        }

    }

    public Thing[] loadDeveloperObject(String path)
    {
        // Emitted objects should be wrapped in another group
        
        // Positions for emitters is from the body root of the primary Thing in the group.
        // Gluing objects together always parents the body root, rather than the direct Thing
        // When grouping an existing group, the group will get grouped instead of the object.

        LoadContext context = this.loadDeveloperAsset(path);
        if (1 < context.things.size())
        {
            Thing root = context.things.get(0);
            while (root.parent != null)
                root = root.parent;
            context.things.remove(root);
            context.things.add(0, root);
        }

        Thing[] things = context.things.toArray(Thing[]::new);
        return things;
    }

    public RLevel getLevel(String path)
    {
        return getLevel(this.loadDeveloperAsset(path));
    }

    public RLevel getLevel(LoadContext context)
    {
        RLevel level = new SerializedResource(FileIO.getResourceFile("/templates/level.bin")).loadResource(RLevel.class);
        level.playerRecord.getPlayerIDs()[0] = DEFAULT_AUTHOR_ID;
        
        // level.worldThing.<PScript>getPart(Part.SCRIPT).instance.script = new ResourceDescriptor(E_CAMBRIDGE_WORLD_SCRIPT_KEY, ResourceType.SCRIPT);
        PWorld world = level.worldThing.getPart(Part.WORLD);

        String[] uri = context.biff.<World>get(ElementType.WORLD).background.split("/");
        String background = "gui/meta/" + uri[1] + "/" + uri[0] + "/" + uri[1] + "_background.plan";

        ResourceDescriptor backdropPlanDescriptor = null;
        FileDBRow backdropFileRow = this.pspDB.get(background);
        if (backdropFileRow == null)
        {
            ItemMetadata metadata = new ItemMetadata();
            metadata.version = 8;
            metadata.categoryKey = -1893332556;
            metadata.themeKey = -1092079999;
            metadata.type = ItemType.BACKGROUND;
            metadata.resource = ((World) context.biff.get(ElementType.WORLD)).background;
            metadata.nameTranslations = new String[LanguageID.MAX];
            metadata.descTranslations = new String[LanguageID.MAX];
            metadata.descTranslations = new String[LanguageID.MAX];
            Arrays.fill(metadata.nameTranslations, uri[1]);
            Arrays.fill(metadata.descTranslations, "");
            Arrays.fill(metadata.creatorTranslations, DEFAULT_AUTHOR);

            backdropPlanDescriptor = ResourceConverter.getItem(context, background.replace(".plan", ".meta" + metadata));
        }
        else backdropPlanDescriptor = new ResourceDescriptor(backdropFileRow.getGUID(), ResourceType.PLAN);

        // Re-assign all existing UIDs so there's no conflicts
        for (Thing thing : world.things)
        {
            if (thing == null || thing == level.worldThing) continue;
            context.reassignThing(thing);
        }

        for (Thing thing : context.things)
            world.things.add(thing);
        
        world.things.removeIf(t -> t == null);
        world.things.sort((a, z) -> a.UID - z.UID);

        world.backdrop.<PRef>getPart(Part.REF).plan = backdropPlanDescriptor;
        world.isPaused = true;
        world.thingUIDCounter = context.lastUID;

        Scoreboard scoreboard = context.biff.get(ElementType.SCOREBOARD);
        if (scoreboard != null)
        {
            for (int i = 0; i < 3; ++i)
            {
                world.completeRewards[i] = this.getEggLink(context, scoreboard.levelCompleteRewards[i]);
                world.collectRewards[i] = this.getEggLink(context, scoreboard.levelCollectedRewards[i]);
                world.aceRewards[i] = this.getEggLink(context, scoreboard.levelAcedRewards[i]);
            }
        }

        return level;
    }

    private EggLink getEggLink(LoadContext context, String string)
    {
        if (string == null) return new EggLink();
        ResourceDescriptor descriptor = ResourceConverter.getItem(context, string);
        if (descriptor == null) return new EggLink();
        return new EggLink(descriptor);
    }

    private LoadContext loadDeveloperAsset(String path)
    {
        Biff biff = new Biff(this.getPSPResource(path));
        LoadContext context = new LoadContext(this, biff);

        context.developerEmitterPath = path;
        if (path.endsWith(".level.biff"))
        {
            context.isDeveloperLevel = true;
            context.developerLevelTag = path.split("\\\\")[2].split("[.]")[0].toUpperCase();
        }

        this.load(context);
        return context;
    }

    private void load(LoadContext context)
    {
        // First pass to add all objects to collection
        for (Element element : context.elements)
        {
            Joint.USE_V013 = context.biff.getAll(ElementType.V013).size() != 0;
            switch (element.getType())
            {
                case LANDSCAPE:
                    PartConverter.addLandscape(context, element.load());
                    break;
                case SCORE_BUBBLE:
                    PartConverter.addScoreBubble(context, element.load());
                    break;
                case PRIZE_BUBBLE:
                    PartConverter.addPrizeBubble(context, element.load());
                    break;
                case JETPACK:
                    PartConverter.addJetpack(context, element.load());
                    break;
                case MAGIC_MOUTH:
                    PartConverter.addMagicMouth(context, element.load());
                    break;
                case MAGNETIC_KEY:
                    PartConverter.addMagneticKey(context, element.load());
                    break;
                case CAMERA_ZONE:
                    PartConverter.addCameraZone(context, element.load());
                    break;
                case ENTRANCE:
                    PartConverter.addCheckpoint(context, element.load(), true);
                    break;
                case CHECKPOINT:
                    PartConverter.addCheckpoint(context, element.load(), false);
                    break;
                case MESH:
                    PartConverter.addMesh(context, element.load());
                    break;
                case EMITTER:
                    PartConverter.addEmitter(context, element.load());
                    break;
                case MUSIC:
                    PartConverter.addMusic(context, element.load());
                    break;
                case PISTON:
                case STRING:
                case ROD:
                case ELASTIC:
                case WINCH:
                case SPRING:
                case BOLT:
                case MOTOR_BOLT:
                case WOBBLE_BOLT:
                case SPRUNG_BOLT:
                    PartConverter.addJoint(context, element);
                    break;
                case STICKER_SWITCH:
                case PROXIMITY_SWITCH:
                case GRAB_SWITCH:
                case TAG_SENSOR:
                    PartConverter.addSwitch(context, element);
                    break;
                case OBJECT:
                    PartConverter.addGroup(context, element.load());
                    break;

                case LEVEL_EXIT:
                {
                    LevelExit exit = element.load();
                    Thing thing = context.getEmptyThing();
                    thing.setPart(Part.POS, new PPos());
                    thing.setPart(Part.SCRIPT, new PScript(new ResourceDescriptor(E_LEVEL_EXIT_SCRIPT_KEY, ResourceType.SCRIPT)));
                    context.things.add(thing);
                    context.lookup.put(exit.uid, thing);
                    break;
                }

                default:
                    break;
            }
        }
        
        // Second pass to attach all switches
        PartConverter.attachSwitchTargets(context);
        // Third pass to glue all objects together
        PartConverter.addGlue(context);

        // Setup emitters
        for (Element element : context.biff.getAll(ElementType.EMITTER))
        {
            Emitter emitter = element.load();
            Thing thing = context.lookup.get(emitter.uid);
            if (thing == null) continue;

            PEmitter part = thing.getPart(Part.EMITTER);
            part.parentThing = thing.parent;

            ArrayList<Thing> emitReferenceThings = context.getObjectForEmitter(emitter.emittedObjectUID);
            if (emitReferenceThings == null || emitReferenceThings.size() == 0) continue;
            
            Thing item = emitReferenceThings.get(0);
            if (item == null) continue;

            Matrix4f parentWorldPos = new Matrix4f().identity();
            if (thing.parent != null && thing.parent.hasPart(Part.POS))
                parentWorldPos = thing.parent.<PPos>getPart(Part.POS).worldPosition;

            Matrix4f worldPos = thing.<PPos>getPart(Part.POS).worldPosition;
            Matrix4f itemWorldPos = new Matrix4f().identity().translate(item.<PPos>getPart(Part.POS).worldPosition.getTranslation(new Vector3f()));

            Vector3f translation = worldPos.getTranslation(new Vector3f());
            Vector3f parentTranslation = parentWorldPos.getTranslation(new Vector3f());
            Vector3f itemTranslation = itemWorldPos.getTranslation(new Vector3f());

            Matrix4f parentItemOffsetMatrix = parentWorldPos.invert(new Matrix4f()).mul(itemWorldPos);

            part.worldOffset = new Vector4f(itemTranslation.sub(translation, new Vector3f()), 0.0f);
            part.worldRotation = 0.0f;


            part.parentRelativeOffset = part.worldOffset.mul(parentWorldPos.invert(new Matrix4f()), new Vector4f());
            part.parentRelativeRotation = (float) Math.toDegrees(part.worldRotation - GetWorldAngle(parentWorldPos));

            part.worldZ = GetAllowedZ(translation.z);

            part.emitBackZ = translation.z;
            part.emitFrontZ = translation.z;
            if (item.hasPart(Part.SHAPE))
            {
                PShape shape = item.getPart(Part.SHAPE);
                part.emitBackZ -= shape.thickness;
                part.emitFrontZ += shape.thickness;
            }
            part.zOffset = itemTranslation.z - part.worldZ;

            part.plan = ResourceConverter.getItem(context, emitter.emittedObjectUID, emitReferenceThings.toArray(Thing[]::new));
        }

        // 3463342888 = limbo? (-1000000000)
        // 3461780388 = world? (-900000000)

        // INGM = ??? = has one u32
        // WRLD, has a UID, and a char[0x50] for background path
        // BKGO = Background lighting
        // MATS = UID, char[0x50] for material path
        // ACT = Contains some Sackboy object
        // V013 = Version tag

        // MYOB
        // Object instance
        // u32 uid
        // f32 lifetime
        // 4 byte - unknown
        // 4 byte - unknown
        // u32 part_count
        // u32[] part_uids
    }

    private void loadStory()
    {
        LoadContext context = new LoadContext(this, null);
        ArrayList<StoryLevel> levels = new StoryLevels(
            this.getPSPResource("myprofile/story.slv"),
            this.getPSPResource("myprofile/story.dlv")
        ).levels;

        Biff frontend = new Biff(this.getPSPResource("frontend/configuration.frontend.biff"));
        Slots locations = frontend.get(ElementType.SLOT);

        ArrayList<Slot> slots = new ArrayList<>();
        ArrayList<Slot> groups = new ArrayList<>();
        for (StoryLevel level : levels)
        {
            if (level.frontendIndex == 0xffff) continue;

            boolean isTurboSlot = level.root.toLowerCase().contains(TURBO_THEME_ID);

            FrontendSlot loc = locations.slots.get(level.frontendIndex);

            // Convert the PSP slot locations to a format that works in LBP1
            float
                x = loc.x,
                y = loc.y,
                z = loc.z;

            // The origin of the Earth mesh on PSP is the top of the mesh,
            // not the center, so correct for the offset.
            y += 0.72;

            // First convert the coordinates into spherical coordinates
            if (x == 0) x = Math.ulp(1.0f); // Avoid divide by zero error

            double radius = Math.sqrt((x * x) + (y * y) + (z * z));
            double azimuth = Math.atan(z / x);

            if (x < 0.0f) azimuth += Math.PI;
            double elevation = Math.asin(y / radius);

            // Do some correction for the difference in how the Earths
            // are modeled between games, these are just eyeballed estimates.
            radius = 590.63; // ~ the radius of the ps3 earth
            azimuth -= Math.toRadians(20.0f);
            elevation += 0.05f;

            // Convert the coordinates back to cartesian coordinates
            double a = radius * Math.cos(elevation);
            x = (float) (a * Math.cos(azimuth));
            y = (float) (radius * Math.sin(elevation));
            z = (float) (a * Math.sin(azimuth));

            boolean isGroup = level.type == LevelType.CREATOR;
            boolean isLevel = !isGroup;
            Slot slot = new Slot(
                new SlotID(isGroup ? SlotType.DEVELOPER_GROUP : SlotType.DEVELOPER, level.id),
                null,
                new Vector4f(x, y, z, 0.0f).normalize()
            );
            slot.icon = ResourceConverter.getTexture(context, level.root + ".mip");

            String key = "PSP_" + Paths.get(level.root).getFileName().toString().toUpperCase();

            // Keep track of the groups because I'm not exactly sure
            // if creators actually get linked to their levels in PSP.
            if (isGroup)
            {
                // Generally follows the format of ThemeCreator, add an underscore just
                // to fit LBP's general style, not needed just stylistic choice, I suppose.
                String creatorKey = key.replace("CREATOR", "_CREATOR_NAME");
                this.addTranslationTag(creatorKey, level.creator);
                slot.authorName = creatorKey;

                key = key.replace("CREATOR", "_GROUP");
                groups.add(slot);

                // Re-assign the slot ID to a hash for no conflicts
                slot.id.slotNumber = Crypto.makeGUID("gamedata/psp" + Archive.resolve(level.root)).getValue();
            }

            this.addTranslationTag(key + "_NAME", level.title);
            this.addTranslationTag(key + "_DESC", level.description);
            slot.translationTag = key;

            if (isLevel)
            {
                String rowPath = ("gamedata/psp" + Archive.resolve(level.root + ".bin"));
                GUID guid = Crypto.makeGUID(rowPath);
                if (this.pspDB.get(guid) == null)
                    context.getBiffDatabase().newFileDBRow(rowPath, guid);

                slot.root = new ResourceDescriptor(guid, ResourceType.LEVEL);

                slot.id.slotNumber = guid.getValue();
                this.createEntryFromData(rowPath, guid.getValue(),
                    SerializedResource.compress(this.getLevel(level.root + ".level.biff").build(
                    GAME_REVISION,
                    CompressionFlags.USE_ALL_COMPRESSION
                )));

                if (level.nextLevelIndex != 0)
                {
                    StoryLevel linkLevel = levels.get(level.nextLevelIndex);
                    int linkLevelId =
                        (int) Crypto.makeGUID("gamedata/psp" + Archive.resolve(linkLevel.root +
                                                                               ".bin")).getValue();
                    slot.primaryLinkLevel = new SlotID(isTurboSlot ? cwlib.enums.SlotType.DLC_LEVEL : cwlib.enums.SlotType.DEVELOPER, linkLevelId);
                }

                slot.developerLevelType = level.type == LevelType.MINIGAME ?
                    cwlib.enums.LevelType.MINI_GAME : cwlib.enums.LevelType.MAIN_PATH;

                // Abuse the fact that level layout is consistent in PSP
                String theme = level.root.split("\\\\")[1];
                String name = level.root.split("\\\\")[2];

                // Credits theme is technically part of Australia
                if (theme.equals("Credits")) theme = "Australia";

                // Java 8's stream API isn't nowhere near as good as C# honestly,
                // just going to use a for loop
                for (Slot group : groups)
                {
                    if (group.translationTag.contains(theme.toUpperCase()))
                    {
                        slot.group = group.id;
                        slot.authorName = group.authorName;
                        break;
                    }
                }

                // Set this to TRUE for release builds!
                slot.initiallyLocked = false;

                // Some levels in the first theme has unique behavior we have to account for.
                switch (name)
                {
                    case "CreditsLevel01":
                    {
                        slot.initiallyLocked = false;
                        slot.developerLevelType = cwlib.enums.LevelType.MINI_LEVEL;
                        slot.authorName = "SCEE Cambridge Studio";
                        break;
                    }
                    case "AustraliaLevel01":
                    {
                        slot.gameProgressionState = GameProgressionStatus.FIRST_LEVEL_COMPLETED;
                        break;
                    }
                    case "AustraliaLevel03b":
                    {
                        slot.gameProgressionState =
                            GameProgressionStatus.GAME_PROGRESSION_COMPLETED;
                        break;
                    }
                }
            }
            
            if (isTurboSlot)
            {

                Pack pack = new Pack();
                pack.slot = slot;
                if (isGroup)
                {
                    pack.contentsType = ContentsType.GROUP;
                    pack.mesh = new ResourceDescriptor(CommonMeshes.POLAROID_GUID, ResourceType.MESH);
                    pack.slot.id.slotType = SlotType.DLC_PACK;
                    pack.contentID = TURBO_CONTENT_ID;
                }
                else
                {
                    pack.contentsType = ContentsType.LEVEL;
                    pack.mesh = new ResourceDescriptor(CommonMeshes.LEVEL_BADGE_GUID, ResourceType.MESH);
                    pack.slot.id.slotType = SlotType.DLC_LEVEL;
                }

                // If the slot already exists, remove it
                contentPacks.removeIf(p -> p.slot.id.equals(slot.id));
                contentPacks.add(pack);
            }
            else
            {
                slots.add(slot);
            }
        }

        RSlotList slotList = new RSlotList(slots);
        this.createEntryFromData("gamedata/data/developer_slots.slt", 21480,
            SerializedResource.compress(
            slotList.build(GAME_REVISION,
                CompressionFlags.USE_ALL_COMPRESSION
            )));
    }

    private void loadMoonSlot()
    {
        boolean loadLocalLevel = false;
        String profile = "E:/emu/ppsspp/memstick/PSP/SAVEDATA/UCUS98744PROFILE/";
        String localSource = profile + "4B.LBF";
        String source = "Levels\\Credits\\CreditsLevel01.level.biff";
        byte[] data = null;

        LoadContext context;
        if (loadLocalLevel)
        {
            data = FileIO.read(localSource);
            Crypto.xor(data);
            byte[] info = Arrays.copyOfRange(data, data.length - 0x19, data.length);
            data = Arrays.copyOfRange(data, 0x0, data.length - 0x19);
            int realSize = Bytes.toIntegerLE(info, 0x0);
            boolean isCompressed = info[0x4] == 1;
            if (isCompressed)
                data = Crypto.decompress(data, realSize);

            Biff biff = new Biff(data);
            context = new LoadContext(this, biff);
            this.load(context);
        }
        else context = loadDeveloperAsset(source);

        this.createZeroedEntry("gamedata/levels/palettes/borders/blank_level_large.bin", 16704);
        RLevel level = this.getLevel(context);

        data = SerializedResource.compress(level.build(GAME_REVISION,
            CompressionFlags.USE_ALL_COMPRESSION));

        FileIO.write(
            this.looseDirectory + "gamedata/levels/palettes/borders/blank_level_large.bin",
            data
        );

        // FileIO.write(
        //     this.looseDirectory + "gamedata/levels/palettes/borders/blank_level_large.bin",
        //     FileIO.read("F:/cache/blank_level_large.bin")
        // );
    }

    private void bakeAllUniqueMaterials()
    {
        CwlibConfiguration.SCE_CGC_EXECUTABLE = new File("E:/usr/local/sce-cgc.exe");

        HashMap<Integer, Material> uniqueMaterialDefinitions = new HashMap<>();

        for (String path : Archive.PATHS.values())
        {
            path = Archive.resolve(path);
            if (!path.endsWith(".model")) continue;

            byte[] modelData = getPSPResource(path);
            if (modelData == null)
            {
                System.out.println("Couldn't retrieve file for " + path);
                continue;
            }

            Model model = null;
            try { model = new Model(modelData); }
            catch (Exception ex)
            {
                System.out.println("Failed to load serialized model: " + path);
                continue;
            }

            for (Material material : model.materials)
                uniqueMaterialDefinitions.put(material.getHash(), material);
        }

        String template = FileIO.getResourceFileAsString("/templates/shader.cg");
        for (int hash : uniqueMaterialDefinitions.keySet())
        {
            Material material = uniqueMaterialDefinitions.get(hash);

            Vector4f diff = Colors.RGBA32.fromARGB(material.diffuseColor);
            Vector4f spec = Colors.RGBA32.fromARGB(material.specularColor);

            String shader = template
                .replace("__PSP_MATERIAL_COEFFICIENT__", Float.toString(material.specularCoefficient))
                .replace("__PSP_DIFFUSE_COLOR__", String.format("float4(%f, %f, %f, %f)", diff.x, diff.y, diff.z, diff.w))
                .replace("__PSP_SPECULAR_COLOR__", String.format("float4(%f, %f, %f, %f)", spec.x, spec.y, spec.z, spec.w));

            RGfxMaterial gmat = new RGfxMaterial();
            gmat.cosinePower = material.specularCoefficient / 22.0f;
            gmat.shaders = new byte[4][];
            CgAssembler.compile(shader, gmat, GameShader.LBP1);

            byte[] resourceData = SerializedResource.compress(gmat.build(GAME_REVISION, CompressionFlags.USE_ALL_COMPRESSION));
            FileIO.write("D:/projects/cambridge/cambridge/src/main/resources/render/baked/" + Bytes.toHex(hash).toLowerCase() + ".gmat", resourceData);
        }
    }

    public static void main(String[] args)
    {
        ResourceSystem.LOG_LEVEL = ResourceLogLevel.NONE;
        ResourceSystem.GUI_MODE = false;

        File installConfigFilePath = new File(FileIO.JAR_DIRECTORY, "install.json");
        if (!installConfigFilePath.exists())
        {
            System.out.println("Install config file doesn't exist!");
            return;
        }

        if (!FileIO.TEXCONV_EXECUTABLE.exists())
        {
            System.out.println("Could not find texconv executable at " + FileIO.TEXCONV_EXECUTABLE.getAbsolutePath());
            return;
        }

        // if (!CwlibConfiguration.FFMPEG_EXECUTABLE.exists())
        // {
        //     System.out.println("Could not find ffmpeg executable at " + CwlibConfiguration.FFMPEG_EXECUTABLE.getAbsolutePath());
        //     return;
        // }
    
        String installConfigJsonData = new String(FileIO.read(installConfigFilePath.getAbsolutePath()));
        InstallConfig config = new GsonBuilder().create().fromJson(installConfigJsonData, InstallConfig.class);
        Biffloader loader = new Biffloader(
            config.gameUsrdir,
            config.targetCachePath,
            config.targetDatabasePath,
            config.archives
        );

        loader.loadMoonSlot();
        loader.loadStory();
        for (TranslationUnit unit : loader.translationData)
        {
            byte[] languageData = unit.ps3TranslationData.build();
            loader.createEntryFromData(String.format("gamedata/languages/%s.trans", unit.name),
                unit.guid.getValue(), languageData);
        }

        loader.downloadables.guids.sort((a, z) -> Long.compareUnsigned(a.guid.getValue(), z.guid.getValue()));
        loader.createEntryFromData("gamedata/languages/keys.trans", E_KEYS_TRANS_KEY, loader.keyCache.build());
        loader.createEntryFromData("gamedata/data/dlc_packs.pck", E_CONTENT_PACKS_KEY, SerializedResource.compress(loader.contentPacks.build(GAME_REVISION, CompressionFlags.USE_ALL_COMPRESSION)));
        loader.createEntryFromData("gamedata/data/downloadables.dlc", E_DOWNLOADABLES_KEY, SerializedResource.compress(loader.downloadables.build(GAME_REVISION, CompressionFlags.USE_ALL_COMPRESSION)));
        
        loader.pspDB.save(new File(config.targetDatabasePath));
        loader.pspCache.save();

        // String csv = "# path, x, y, z\n";
        // for (String modelFilePath : PartConverter.modelOffsetPaths.keySet())
        // {
        //     ArrayList<Vector3f> offsets = PartConverter.modelOffsetPaths.get(modelFilePath);
        //     for (int i = 0; i < offsets.size(); ++i)
        //     {
        //         if (i != 0) csv += "# ";
        //         Vector3f offset = offsets.get(i);
        //         csv += String.format("%s, %f, %f, %f\n", modelFilePath, offset.x, offset.y, offset.z);
        //     }
        // }

        // FileIO.write("C:/Users/Aidan/Desktop/offsets.csv", csv.getBytes(StandardCharsets.UTF_8));

        if (config.saveUsrdir != null)
        {
            Pattern regex = Pattern.compile("littlefart\\d+");
            File[] localCacheFiles = new File(config.saveUsrdir).listFiles((dir, name) -> regex.matcher(name).matches());
            if (localCacheFiles.length == 0) return;

            File localCacheFile = localCacheFiles[localCacheFiles.length - 1];
            SaveArchive save = new SaveArchive(localCacheFile);
            RLocalProfile local = save.loadResource(save.getKey().getRootHash(), RLocalProfile.class);


            int numItemsAdded = 0;
            for (FileDBRow row : loader.pspDB) 
            {
                String path = row.getPath();
                if (!path.endsWith(".plan") || !path.startsWith("gamedata/psp/")) continue;

                // local.inventory.remove(local.getItem(new ResourceDescriptor(row.getGUID(), ResourceType.PLAN)));

                if (local.hasItem(row.getGUID())) continue;

                byte[] planData = loader.pspCache.extract(row.getSHA1());
                if (planData == null)
                    planData = FileIO.read(new File(loader.looseDirectory, path).getAbsolutePath());
                if (planData == null)
                    continue;
                
                RPlan plan = new SerializedResource(planData).loadResource(RPlan.class);
                local.addItem(plan, new ResourceDescriptor(row.getGUID(), ResourceType.PLAN), loader.keyCache);
                numItemsAdded++;
            }

            if (numItemsAdded != 0)
            {
                save.getKey().setRootHash(
                    save.add(SerializedResource.compress(local.build(save.getGameRevision(),
                    CompressionFlags.USE_ALL_COMPRESSION)))
                );

                save.save();
            }
        }
    }

    /**
     * Install config file loaded from file.
     */
    public static class InstallConfig
    {
        /**
         * The user's save game directory.
         */
        public String saveUsrdir;

        /**
         * The user's game directory.
         */
        public String gameUsrdir;

        /**
         * The cache to save all file data to.
         */
        public String targetCachePath;

        /**
         * The database to save all files to.
         */
        public String targetDatabasePath;

        /**
         * The list of PSP archives to use
         */
        public String[] archives;
    }

    /**
     * Load data for a particular biff.
     */
    public static class LoadContext
    {
        /**
         * The loader this context belongs to.
         */
        public final Biffloader loader;

        /**
         * Whether we're loading a developer level.
         */
        public boolean isDeveloperLevel;

        /**
         * The unique tag for developer levels, used for assigning speech translation tags.
         */
        public String developerLevelTag;

        /**
         * The path to store developer emitter objects.
         */
        public String developerEmitterPath;

        /**
         * Biff associated with this load context.
         */
        public final Biff biff;

        /**
         * Elements loaded from the Biff.
         */
        public ArrayList<Element> elements = new ArrayList<>();

        /**
         * Mapping from PSP UIDs to converted game objects.
         */
        public HashMap<UID, Thing> lookup = new HashMap<>();

        /**
         * All created things in the context.
         */
        public ArrayList<Thing> things = new ArrayList<>();

        /**
         * Cache of objects that are grouped together by UID.
         */
        public HashMap<UID, Thing[]> objects = new HashMap<>();

        /**
         * Internal tracker for used object UIDs.
         */
        private int lastUID = 0;

        public LoadContext(Biffloader loader, Biff biff)
        {
            this.loader = loader;
            this.biff = biff;
            if (biff != null)
                this.elements = biff.getAll(null);
        }

        public ArrayList<Thing> getObjectForEmitter(UID rootObjectUID)
        {
            Thing rootGroupThing = lookup.get(rootObjectUID);
            if (rootGroupThing == null) return null;

            HashSet<Thing> groupHeads = new HashSet<>();
            groupHeads.add(rootGroupThing);

            // First pass of the things to gather all group heads
            ArrayList<Thing> rootObjects = new ArrayList<>();
            for (int i = 0; i < 2; ++i)
            {
                for (Thing gameObject : things)
                {
                    if (gameObject == null || gameObject.groupHead == null) continue;
                    if (groupHeads.contains(gameObject.groupHead))
                    {
                        if (gameObject.hasPart(Part.GROUP))
                            groupHeads.add(gameObject);
                    }
                }
            }

            // Second pass of things to gather all roots
            for (Thing gameObject : things)
            {
                if (gameObject == null || gameObject.groupHead == null) continue;
                if (groupHeads.contains(gameObject.groupHead))
                {
                    if (gameObject.hasPart(Part.BODY) || gameObject.parent != null)
                        rootObjects.add(gameObject);
                }
            }

            ArrayList<Thing> emitted = new ArrayList<>();
            for (Thing thing : rootObjects)
                deleteThingAndChildren(emitted, thing);
            for (Thing thing : groupHeads)
                deleteThingAndChildren(emitted, thing);

            return emitted;
        }

        public ArrayList<Thing> deleteThingAndChildren(Thing root)
        {
            ArrayList<Thing> thingsRemovedList = new ArrayList<>();
            deleteThingAndChildren(thingsRemovedList, root);
            return thingsRemovedList;
        }

        public void deleteThingAndChildren(ArrayList<Thing> thingsRemovedList, Thing root)
        {
            if (root == null) return;

            things.remove(root);
            if (!thingsRemovedList.contains(root))
                thingsRemovedList.add(root);
            
            for (Thing thing : new ArrayList<Thing>(things))
            {
                if (thing == null) continue;
                if (thing.parent == root || thing.groupHead == root)
                    deleteThingAndChildren(thingsRemovedList, thing);
            }
        }

        public Thing getEmptyThing()
        {
            return new Thing(++lastUID);
        }

        public void reassignThing(Thing thing)
        {
            thing.UID = ++lastUID;
        }

        public String getLoosePath()
        {
            return this.loader.looseDirectory;
        }

        public FileDB getBiffDatabase()
        {
            return this.loader.pspDB;
        }

        public boolean exists(GUID guid)
        {
            return this.loader.pspDB.get(guid) != null;
        }
    }

    public static class TranslationUnit
    {
        public int id;
        public String name;
        public GUID guid;

        public RTranslationTable ps3TranslationData;
        public Translations pspTranslationData;
    }

    public static float GetWorldAngle(Matrix4f matrix)
    {
        return (float) Math.atan2(-matrix.m10(), matrix.m00());
    }

    public static float ModAngleToWithinPI(float angle)
    {
        double pi2 = Math.PI * 2.0;
        double a = (angle + Math.PI) / pi2;
        return (float)((a - Math.floor(a)) * pi2 - Math.PI);
    }

    // MathFunctions
    public static float GetAngleModdedToWithin2PI(float angle)
    {
        double pi2 = Math.PI * 2.0;
        return (float)(angle - (Math.floor(angle / pi2) * pi2));
    }

    // PEmitter
    public static float GetAllowedZ(float z)
    {
        return (float)(200.0 * Math.floor(z / 200.0 + 0.5));
    }
}
