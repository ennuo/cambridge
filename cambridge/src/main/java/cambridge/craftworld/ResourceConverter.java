package cambridge.craftworld;

import cambridge.craftworld.Biffloader.LoadContext;
import cambridge.enums.ElementType;
import cambridge.enums.LanguageID;
import cambridge.io.streams.MemoryOutputStream;
import cambridge.resources.Archive;
import cambridge.resources.Biff;
import cambridge.resources.Group;
import cambridge.resources.Skeleton;
import cambridge.resources.model.*;
import cambridge.structures.*;
import cambridge.structures.MaterialManager.RenderMaterial;
import cambridge.structures.data.UID;
import cambridge.util.Crypto;
import cambridge.util.FileIO;
import cwlib.CwlibConfiguration;
import cwlib.enums.*;
import cwlib.io.FMOD;
import cwlib.io.serializer.SerializationData;
import cwlib.resources.*;
import cwlib.structs.inventory.CreationHistory;
import cwlib.structs.inventory.InventoryItemDetails;
import cwlib.structs.mesh.Bone;
import cwlib.structs.mesh.Primitive;
import cwlib.structs.things.Thing;
import cwlib.structs.things.components.decals.Decal;
import cwlib.structs.things.parts.*;
import cwlib.types.SerializedResource;
import cwlib.types.data.GUID;
import cwlib.types.data.NetworkPlayerID;
import cwlib.types.data.ResourceDescriptor;
import cwlib.types.data.Revision;
import cwlib.util.Bytes;
import cwlib.util.Colors;

import org.joml.*;

import javax.imageio.ImageIO;
import javax.management.RuntimeErrorException;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.spi.ResourceBundleProvider;
import java.lang.Math;


public class ResourceConverter
{
    public static HashMap<String, String> BONE_REMAP = new HashMap<>()
    {{
        put("bone_1365030636", "Dummy01");
        put("bone_2166118096", "Bip01 Pelvis");
        put("bone_1831866672", "Bip01 Spine");

        put("bone_2481166152", "Bip01 L Thigh");
        put("bone_235041922", "Bip01 L Calf");
        put("bone_4263754471", "Bip01 L Foot");
        put("bone_1643236507", "Bip01 L Toe0");

        put("bone_1011976026", "Bip01 R Thigh");
        put("bone_3103380149", "Bip01 R Calf");
        put("bone_1177085006", "Bip01 R Foot");
        put("bone_2617176568", "Bip01 R Toe0");

        put("bone_1477183843", "Bip01 Head");

        put("bone_3817638489", "Bip01 L UpperArm");
        put("bone_120206804", "Bip01 L Forearm");
        put("bone_281985531", "Bip01 L Hand");
        put("bone_2284292734", "Bip01 L Finger1");
        put("bone_1410951753", "Bip01 L Finger0");

        put("bone_1433873518", "Bip01 R UpperArm");
        put("bone_1651112657", "Bip01 R Forearm");
        put("bone_2831523666", "Bip01 R Hand");
        put("bone_1915270941", "Bip01 R Finger1");
        put("bone_2920699690", "Bip01 R Finger0");

        put("bone_3185732281", "bone_zip_pull01");
        put("bone_4151957090", "Bone_jaw");

        put("bone_1224964842", "bone_eye_pos_left");
        put("bone_3003926409", "bone_eye_pos_right");
    }};

    public static final RPlan COSTUME_TEMPLATE = new SerializedResource(FileIO.getResourceFile(
        "/templates/costume.plan")).loadResource(RPlan.class);
    public static final RBevel TEMPLATE_BEVEL =
        new SerializedResource(FileIO.getResourceFile("/templates/bevel.bev")).loadResource(RBevel.class);
    public static final String MUSIC_TEMPLATE =
        FileIO.getResourceFileAsString("/templates/music_settings.mus");

    public static final HashMap<String, Long> AUDIO_BIFF_TO_KEY = new HashMap<>();
    public static final HashMap<String, Vector3f> MESH_BONE_OFFSET = new HashMap<>();

    static
    {
        String csv = FileIO.getResourceFileAsString("/lists/offsets.csv");
        for (String line : csv.split("\n"))
        {
            line = line.trim();
            if (line.startsWith("#") || line.isEmpty()) continue;

            String[] columns = line.split(",");

            String path = Archive.resolve(columns[0].trim());
            Vector3f offset = new Vector3f(
                Float.parseFloat(columns[1].trim()),
                Float.parseFloat(columns[2].trim()),
                Float.parseFloat(columns[3].trim())
            );

            MESH_BONE_OFFSET.put(path, offset);
        }
    }

    public static ResourceDescriptor getBevel(LoadContext context, String path)
    {
        path = Archive.resolve(path);
        String name = new File(path).getName().split("\\.")[0];
        String rowPath = "gamedata/psp/bevels/" + name + ".bev";
        GUID guid = Crypto.makeGUID(rowPath);

        TEMPLATE_BEVEL.setMaterial(getMaterial(context, path, true), 1);

        byte[] data = SerializedResource.compress(TEMPLATE_BEVEL.build(new Revision(0x132),
            CompressionFlags.USE_NO_COMPRESSION));
        context.loader.createEntryFromData(rowPath, guid.getValue(), data);

        return new ResourceDescriptor(guid, ResourceType.BEVEL);
    }

    public static ResourceDescriptor getItem(LoadContext context, UID uid, Thing[] things)
    {
        String name =
            new File(context.developerEmitterPath).getName().split("\\.")[0].toLowerCase();
        String path =
            "gamedata/psp/virtual/" + name + "_" + (uid.getHash() & 0xffffffffL) + ".plan";
        GUID guid = Crypto.makeGUID(path);

        RPlan plan = new RPlan();
        plan.revision = Biffloader.GAME_REVISION;
        plan.compressionFlags = CompressionFlags.USE_ALL_COMPRESSION;
        plan.setThings(things);

        byte[] planData = SerializedResource.compress(plan.build());
        context.loader.createEntryFromData(path, guid.getValue(), planData);

        return new ResourceDescriptor(guid, ResourceType.PLAN);
    }

    public static ResourceDescriptor getItem(LoadContext context, String path)
    {
        return getItem(context, path, null);
    }

    public static Thing[] getCostumeThings(LoadContext context, InventoryItemDetails details,
                                           String path, CostumePieceCategory category, int hiddenBits)
    {
        Thing[] things = new Thing[] { new Thing(1) };

        String[] regions = new String[]
        {
            "_scalp",
            "_brow;_tongue",
            "_eyes",
            "_zippull;_zip",
            "_torso",
            "_torso",
            "_torso1",
            "_torso1",
            "_sleevel",
            "_arml",
            "_glovel",
            "_sleever",
            "_armr",
            "_glover",
            "_pants",
            "_shorts",
            "_legs",
            "_socks"
        };

        HashSet<String> r = new HashSet<>();
        HashSet<Integer> set = new HashSet<>();
        for (int i = 0; i < regions.length; ++i)
        {
            if ((hiddenBits & (1 << i)) != 0)
            {
                String[] selections = regions[i].split(";");
                for (String sel : selections)
                {
                    r.add(sel);
                    set.add(RAnimation.calculateAnimationHash(sel));
                }
            }
        }

        PRenderMesh mesh = new PRenderMesh(
            getMesh(context, path, category, new ArrayList<>(set)),
            things
        );

        PCostume costume = new PCostume();
        costume.costumePieces = null;
        costume.temporaryCostumePiece = null;

        things[0].setPart(Part.RENDER_MESH, mesh);
        things[0].setPart(Part.COSTUME, costume);

        details.type = EnumSet.of(InventoryObjectType.COSTUME);
        details.subType = category.getFlag();
        if (mesh.mesh == null)
            return null;

        return things;
    }

    public static ResourceDescriptor getItem(LoadContext context, String path,
                                             ItemMetadata metadata)
    {
        path = Archive.resolve(path);

        String name = new File(path).getName().split("\\.")[0];
        String tag = "PSP_" + name.toUpperCase();

        String rowPath = ("gamedata/psp" + path).replace(".meta.biff", ".plan");
        GUID guid = Crypto.makeGUID(rowPath);
        // if (context.exists(guid))
        //     return new ResourceDescriptor(guid, ResourceType.PLAN);

        if (metadata == null)
        {
            byte[] data = context.loader.getPSPResource(path);
            if (data == null) return null;

            Biff biff = new Biff(data);

            metadata = biff.getAll(ElementType.ITEM_METADATA).get(0).load();
        }

        if (metadata.version != 8)
            return null;

        int lastUID = 0;
        Thing[] things = new Thing[] { new Thing(++lastUID) };
        Thing thing = things[0];

        InventoryItemDetails details = new InventoryItemDetails();

        String folder = new File(metadata.resource).getParent();
        if (folder != null) folder = folder.replace("\\", "/");

        details.icon = ResourceConverter.getTexture(context, path.replace(".meta.biff", ".mip"));
        switch (metadata.type)
        {
            case BACKGROUND:
            {
                Biff item = new Biff(context.loader.getPSPResource(metadata.resource));

                BackgroundData bdat = item.get(ElementType.BACKGROUND_DATA);
                BackgroundColor bclr = item.get(ElementType.BACKGROUND_COLOR);
                BackgroundLighting bals = item.get(ElementType.BACKGROUND_LIGHTING);

                ResourceDescriptor background = ResourceConverter.getMesh(context,
                    bdat.backgroundModel, null);
                ResourceDescriptor floor = ResourceConverter.getMesh(context, bdat.brickModel,
                    null);

                if (background == null || floor == null)
                    return new ResourceDescriptor(31968, ResourceType.PLAN);


                Vector3f backgroundCOM = new Vector3f().zero();
                Vector3f floorCOM = new Vector3f().zero();

                // Vector3f backgroundCOM =
                //     ResourceConverter.getCOM(context, bdat.backgroundModel).mul(PartConverter.WORLD_SCALE);
                // Vector3f floorCOM =
                //     ResourceConverter.getCOM(context, bdat.brickModel).mul(PartConverter.WORLD_SCALE);

                Matrix4f wpos =
                    new Matrix4f().identity().setTranslation(PartConverter.WORLD_OFFSET.add(backgroundCOM, new Vector3f()));

                ArrayList<Thing> backgroundThings = new ArrayList<>();


                PLevelSettings lighting = new PLevelSettings();
                lighting.sunPositionScale = 310309.28f;

                lighting.sunPosition = new Vector3f(0.08f, 0.24439862f, 0.17429554f);
                lighting.sunColor = new Vector4f(0.9131379f, 0.8234369f, 0.833018f, 1.0426356f);
                lighting.ambientColor = new Vector4f(0.80777466f, 0.8210661f, 0.7313651f, 1.0f);
                lighting.rimColor = new Vector4f(0.8252604f, 0.69928366f, 0.55837226f, 1.5f);
                lighting.rimColor2 = new Vector4f(0.35081163f, 0.6995919f, 0.9100586f, 1.0f);
                lighting.fogColor = new Vector4f(0.6026691f, 0.76635367f, 0.78096366f, 1.0f);
                lighting.sunMultiplier = 1.5f;
                lighting.exposure = 1.05f;
                lighting.fogNear = 200.0f;
                lighting.fogFar = 15000.0f;

                int sunLightIndex = 0;
                if (bals.lights[sunLightIndex].diffuseColor.equals(new Vector4f(), 0.01f))
                    sunLightIndex = 1;


                System.out.println(sunLightIndex);

                lighting.sunPositionScale = 300_000.0f;
                lighting.sunPosition = bals.lights[sunLightIndex].lightVector;
                lighting.sunColor = bals.lights[sunLightIndex].diffuseColor;
                lighting.ambientColor = bals.globalAmbientColor.mul(1.5f);
                lighting.rimColor = bals.lights[2].diffuseColor.mul(1.5f);
                lighting.rimColor2 = bals.lights[3].diffuseColor.mul(1.5f);
                
                // lighting.fogColor = new Vector4f(bclr.rgb, 1.0f);
                lighting.sunMultiplier = 1.5f;
                lighting.exposure = 1.0f;
                // lighting.exposure = 1.0f / 0.6f;

                lighting.fogNear =  (bals.fogNear * PartConverter.WORLD_SCALE) + PartConverter.WORLD_OFFSET.z;
                lighting.fogFar =  (bals.fogFar * PartConverter.WORLD_SCALE) + PartConverter.WORLD_OFFSET.z;


                Thing settingsThing = new Thing(++lastUID);
                settingsThing.setPart(Part.POS, new PPos());
                settingsThing.setPart(Part.LEVEL_SETTINGS, lighting);
                settingsThing.setPart(Part.GROUP, new PGroup());

                Thing backgroundThing = new Thing(++lastUID);
                backgroundThing.groupHead = settingsThing;
                backgroundThing.setPart(Part.POS, new PPos(wpos));
                {
                    PRenderMesh mesh = new PRenderMesh(background);
                    mesh.boneThings = new Thing[] { backgroundThing };
                    backgroundThing.setPart(Part.RENDER_MESH, mesh);
                }

                backgroundThings.add(settingsThing);
                backgroundThings.add(backgroundThing);

                backgroundThing.parent = settingsThing;

                wpos =
                    new Matrix4f().identity().setTranslation(PartConverter.WORLD_OFFSET.add(floorCOM, new Vector3f()));

                for (int i = -1; i < 26; ++i)
                {
                    Thing floorThing = new Thing(++lastUID);
                    floorThing.groupHead = settingsThing;
                    floorThing.parent = settingsThing;
                    floorThing.setPart(Part.POS,
                        new PPos(wpos.translate(PartConverter.WORLD_SCALE * bdat.brickRepeatX * (i + 1), 0.0f, 0.0f, new Matrix4f())));
                    {
                        PRenderMesh mesh = new PRenderMesh(floor);
                        mesh.boneThings = new Thing[] { floorThing };
                        floorThing.setPart(Part.RENDER_MESH, mesh);
                    }
                    backgroundThings.add(floorThing);
                }

                for (int i = 0; i < 26; ++i)
                {
                    Thing floorThing = new Thing(++lastUID);
                    floorThing.groupHead = settingsThing;
                    floorThing.parent = settingsThing;
                    floorThing.setPart(Part.POS,
                        new PPos(wpos.translate(PartConverter.WORLD_SCALE * -bdat.brickRepeatX * (i + 1), 0.0f, 0.0f, new Matrix4f())));
                    {
                        PRenderMesh mesh = new PRenderMesh(floor);
                        mesh.boneThings = new Thing[] { floorThing };
                        floorThing.setPart(Part.RENDER_MESH, mesh);
                    }
                    backgroundThings.add(floorThing);
                }

                things = backgroundThings.toArray(Thing[]::new);
                details.type = EnumSet.of(InventoryObjectType.BACKGROUND);

                break;
            }

            case STICKER:
            {
                ResourceDescriptor sticker = getTexture(context, metadata.resource);
                thing.setPart(Part.STICKERS, new PStickers(sticker));
                thing.<PStickers>getPart(Part.STICKERS).decals.get(0).plan = new ResourceDescriptor(guid, ResourceType.PLAN);
                details.icon = sticker;
                details.type = EnumSet.of(InventoryObjectType.STICKER);
                break;
            }

            case MATERIAL:
            {
                PGeneratedMesh mesh = new PGeneratedMesh();
                PShape shape = new PShape();

                thing.setPart(Part.GENERATED_MESH, mesh);
                thing.setPart(Part.SHAPE, shape);

                MaterialManager manager = new Biff(
                    context.loader.getPSPResource(metadata.resource)
                ).getAll(ElementType.MATERIAL_MANAGER).get(0).load();
                RenderMaterial material = manager.materials[metadata.resourceIndex];
                MaterialLookupData lookup = MaterialLookupData.MLUT.get(material.uid.getHash());

                mesh.gfxMaterial = lookup.renderMaterial;
                mesh.bevel = lookup.bevel;
                mesh.planGUID = guid;

                shape.material = lookup.physicsResource;

                details.type = EnumSet.of(InventoryObjectType.PRIMITIVE_MATERIAL);
                break;
            }

            case OBJECT:
            {
                things = context.loader.loadDeveloperObject(metadata.resource.replace("catalog", "object"));
                details.type = EnumSet.of(InventoryObjectType.READYMADE);
                break;
            }

            case COSTUME_HEAD:
            {
                things = getCostumeThings(context, details, folder + "/head.model",
                    CostumePieceCategory.HEAD, metadata.hiddenShapeFlags);
                break;
            }

            case COSTUME_HAIR:
            {
                things = getCostumeThings(context, details, folder + "/hair.model",
                    CostumePieceCategory.HAIR, metadata.hiddenShapeFlags);
                break;
            }

            case COSTUME_EYES:
            {
                things = getCostumeThings(context, details, folder + "/eyes.model",
                    CostumePieceCategory.EYES, metadata.hiddenShapeFlags);
                break;
            }

            case COSTUME_GLASSES:
            {
                things = getCostumeThings(context, details, folder + "/glasses.model",
                    CostumePieceCategory.GLASSES, metadata.hiddenShapeFlags);
                break;
            }

            case COSTUME_MOUTH:
            {
                things = getCostumeThings(context, details, folder + "/mouth.model",
                    CostumePieceCategory.MOUTH, metadata.hiddenShapeFlags);
                break;
            }

            case COSTUME_MOUSTACHE:
            {
                things = getCostumeThings(context, details, folder + "/tache.model",
                    CostumePieceCategory.MOUSTACHE, metadata.hiddenShapeFlags);
                break;
            }

            case COSTUME_NECK:
            {
                things = getCostumeThings(context, details, folder + "/neck.model",
                    CostumePieceCategory.NECK, metadata.hiddenShapeFlags);
                break;
            }

            case COSTUME_TORSO:
            {
                things = getCostumeThings(context, details, folder + "/torso.model",
                    CostumePieceCategory.TORSO, metadata.hiddenShapeFlags);
                break;
            }

            case COSTUME_WAIST:
            {
                things = getCostumeThings(context, details, folder + "/waist.model",
                    CostumePieceCategory.WAIST, metadata.hiddenShapeFlags);
                break;
            }

            case COSTUME_HANDS:
            {
                things = getCostumeThings(context, details, folder + "/hands.model",
                    CostumePieceCategory.HANDS, metadata.hiddenShapeFlags);
                break;
            }

            case COSTUME_LEGS:
            {
                things = getCostumeThings(context, details, folder + "/legs.model",
                    CostumePieceCategory.LEGS, metadata.hiddenShapeFlags);
                break;
            }

            case COSTUME_FEET:
            {
                things = getCostumeThings(context, details, folder + "/feet.model",
                    CostumePieceCategory.FEET, metadata.hiddenShapeFlags);
                break;
            }

            case MUSIC:
            {
                AUDIO_BIFF_TO_KEY.put(metadata.resource, guid.getValue());
                
                String templateRowPath =
                    "gamedata/psp" + Archive.resolve(metadata.resource.replace(".music.biff",
                        ".mus"));
                GUID templateGUID = Crypto.makeGUID(templateRowPath);
                context.loader.createZeroedEntry(templateRowPath, templateGUID.getValue());

                context.loader.createEntryFromData(templateRowPath, templateGUID.getValue(),
                    MUSIC_TEMPLATE.replace("TEMPLATE_TAG", tag).getBytes());


                String audioRowPath = "gamedata/psp" + Archive.resolve(metadata.resource.replace(
                    ".music.biff", ".fsb"));
                GUID audioGUID = Crypto.makeGUID(audioRowPath);
                context.loader.createZeroedEntry(audioRowPath, audioGUID.getValue());

                File destinationFilePath = new File(context.getLoosePath(), audioRowPath);
                if (!destinationFilePath.exists())
                {
                    System.out.println("[ResourceConverter::GetItem] Creating FSB for " + metadata.resource + ", this is going to be really slow!");
                    FileIO.write(destinationFilePath.getAbsolutePath(), context.loader.getPSPResource(metadata.resource.replace(".music.biff", ".at3")));
                    FileIO.write(destinationFilePath.getAbsolutePath(), FMOD.FMODSampleBank.fromAudioFile(destinationFilePath.getAbsolutePath()));
                }

                details.type = EnumSet.of(InventoryObjectType.MUSIC);
                thing.setPart(Part.RENDER_MESH, new PRenderMesh(new ResourceDescriptor(30124, ResourceType.MESH), things));
                thing.setPart(Part.POS, new PPos(thing, 0));
                thing.setPart(Part.TRIGGER, new PTrigger(TriggerType.RADIUS, 600.0f));
                thing.<PTrigger>getPart(Part.TRIGGER).allZLayers = true;
                PScript script = new PScript(new ResourceDescriptor(18256, ResourceType.SCRIPT));
                script.instance.addField("FSB", audioGUID);
                script.instance.addField("SettingsFile", templateGUID);
                thing.setPart(Part.SCRIPT, script);

                PStickers stickers = new PStickers(details.icon);
                Decal decal = stickers.decals.get(0);
                decal.u = 0.626676f;
                decal.v = 0.14709048f;
                decal.xvecu = -0.10829564f;
                decal.xvecv = -0.0032766173f;
                decal.yvecu = 0.003069249f;
                decal.yvecv = -0.11560983f;

                thing.setPart(Part.STICKERS, stickers);

                break;
            }

            default:
                // Unsupported resource type
                // things = new Thing[0];
                // details.type = EnumSet.of(InventoryObjectType.READYMADE);
                return null;
            // break;
        }

        if (things == null || things.length == 0) return null;
        for (Thing t : things)
        {
            if (t == null) continue;

            PGroup group = t.getPart(Part.GROUP);
            if (group != null)
            {
                group.planDescriptor = new ResourceDescriptor(guid, ResourceType.PLAN);
                group.creator = Biffloader.DEFAULT_AUTHOR_ID;
            }

            t.planGUID = guid;
        }

        details.dateAdded = 0;
        details.translationTag = tag;
        for (int i = 0; i < LanguageID.MAX; ++i)
        {
            details.titleKey = context.loader.addTranslationTag(i, tag + "_NAME",
                metadata.nameTranslations[i]);
            details.descriptionKey = context.loader.addTranslationTag(i, tag + "_DESC",
                metadata.nameTranslations[i]);
        }

        Group categoryGroup = context.loader.getPopitGroup(metadata.categoryKey);
        Group themeGroup = context.loader.getPopitGroup(metadata.themeKey);

        if (categoryGroup != null)
        {
            details.categoryTag = "PSP_" + categoryGroup.name;
            for (int i = 0; i < LanguageID.MAX; ++i)
                details.category = context.loader.addTranslationTag(i, details.categoryTag,
                    categoryGroup.translations[i]);
        }

        if (themeGroup != null)
        {
            details.locationTag = "PSP_" + themeGroup.name;
            for (int i = 0; i < LanguageID.MAX; ++i)
                details.location = context.loader.addTranslationTag(i, details.locationTag,
                    themeGroup.translations[i]);
        }

        String creator = metadata.creatorTranslations[2].replaceAll(" ", "_");
        if (creator.length() > 16)
            creator = creator.substring(0, 15);
        details.creator = new NetworkPlayerID(creator);
        details.creationHistory = new CreationHistory(creator);

        RPlan plan = new RPlan(
            Biffloader.GAME_REVISION,
            CompressionFlags.USE_ALL_COMPRESSION,
            things,
            details
        );

        byte[] planData = SerializedResource.compress(plan.build());
        context.loader.createEntryFromData(rowPath, guid.getValue(), planData);

        return new ResourceDescriptor(guid, ResourceType.PLAN);
    }

    public static ResourceDescriptor getTexture(LoadContext context, String path)
    {
        return getTexture(context, path, false);
    }

    public static ResourceDescriptor getTexture(LoadContext context, String path, boolean rotate90)
    {
        path = Archive.resolve(path);
        String rowPath = ("gamedata/psp" + path).replace(".mip", ".tex");
        GUID guid = Crypto.makeGUID(rowPath);
        if (context.exists(guid))
            return new ResourceDescriptor(guid, ResourceType.TEXTURE);

        try
        {
            File input = new File("TEXTURE.PNG");
            File output = new File("TEXTURE.DDS");

            byte[] mipData = context.loader.getPSPResource(path);
            if (mipData == null)
                mipData = context.loader.getPSPResource(path.replace(".mip", ".16.mip"));
            if (mipData == null)
            {
                System.out.println("Couldn't retrieve file for " + path);
                return null;
            }

            try
            {
                Texture texture = new Texture(mipData);
                BufferedImage image = texture.getImage();
                if (rotate90)
                {
                    int w = image.getWidth();
                    int h = image.getHeight();
                    BufferedImage dest = new BufferedImage(h, w, image.getType());
                    for (int y = 0; y < h; y++)
                        for (int x = 0; x < w; x++)
                            dest.setRGB(y, w - x - 1, image.getRGB(x, y));
                    image = dest;
                }


                ImageIO.write(image, "png", input);
            }
            catch (Exception ex)
            {
                System.out.println("Failed to load texture: " + path);
                return null;
            }

            ProcessBuilder builder =
                new ProcessBuilder(FileIO.TEXCONV_EXECUTABLE.getAbsolutePath(),
                "texconv",
                "-f",
                "DXT5",
                "-y",
                "-nologo",
                "-m",
                "0",
                input.getAbsolutePath(),
                "-o",
                new File("./").getAbsolutePath());

            builder.start().waitFor();

            byte[] imageData = null;
            if (output.exists())
            {
                imageData = FileIO.read(output.getAbsolutePath());
                output.delete();
            }

            input.delete();

            if (imageData == null)
                throw new RuntimeException("Failed to convert image to DDS!");

            byte[] textureData = SerializedResource.compress(new SerializationData(imageData));
            context.loader.createEntryFromData(rowPath, guid.getValue(), textureData);

            return new ResourceDescriptor(guid, ResourceType.TEXTURE);
        }
        catch (Exception ex)
        {
            System.out.println(path);
            return null;
        }
    }

    public static ResourceDescriptor getMaterial(LoadContext context, String path)
    {
        return getMaterial(context, path, 0, false);
    }

    public static ResourceDescriptor getMaterial(LoadContext context, String path, boolean isStitchMaterial)
    {
        return getMaterial(context, path, 0, isStitchMaterial);
    }

    public static ResourceDescriptor getMaterial(LoadContext context, String path, int templateHash, boolean isStitchMaterial)
    {
        path = Archive.resolve(path);
        String rowPath = "gamedata/psp/gmat/" + new File(path).getName().replace(".mip", ".gmat");
        GUID guid = Crypto.makeGUID(rowPath);
        if (context.exists(guid) && !isStitchMaterial)
            return new ResourceDescriptor(guid, ResourceType.GFX_MATERIAL);
        ResourceDescriptor diffuse = getTexture(context, path, isStitchMaterial);
        if (diffuse == null) return null;

        // byte[] resourceData = RShaderCache.LBP1.compile(new Vector4f(1.0f, 1.0f, 0.0f, 0.0f),
        // diffuse, true, true);

        // RGfxMaterial material = new SerializedResource(resourceData).loadResource(RGfxMaterial
        // .class);
        // resourceData = SerializedResource.compress(material.build(new Revision(0x272, 0x4c44,
        // 0x0008), CompressionFlags.USE_ALL_COMPRESSION));


        RGfxMaterial defaultMaterial;
        if (isStitchMaterial)
        {
            defaultMaterial = context.loader.loadPS3Resource(10824, RGfxMaterial.class);
        }
        else
        {
            // 65946
            //defaultMaterial = context.loader.loadPS3Resource(19502, RGfxMaterial.class);

            if (templateHash != 0)
                defaultMaterial = new SerializedResource(FileIO.getResourceFile("/render/baked/" + Bytes.toHex(templateHash).toLowerCase() + ".gmat")).loadResource(RGfxMaterial.class);
            else 
                defaultMaterial = new SerializedResource(FileIO.getResourceFile("/render/default.gmat")).loadResource(RGfxMaterial.class);
        }

        defaultMaterial.textures[0] = diffuse;
        byte[] resourceData =
            SerializedResource.compress(defaultMaterial.build(Biffloader.GAME_REVISION
                , CompressionFlags.USE_ALL_COMPRESSION));

        context.loader.createEntryFromData(rowPath, guid.getValue(), resourceData);
        return new ResourceDescriptor(guid, ResourceType.GFX_MATERIAL);
    }

    public static Vector3f getCOM(LoadContext context, String path)
    {
        byte[] modelData = context.loader.getPSPResource(path);
        if (modelData == null)
        {
            System.out.println("Couldn't retrieve file for " + path);
            return null;
        }

        Model model = null;
        try { model = new Model(modelData); }
        catch (Exception ex)
        {
            System.out.println("Failed to load serialized model: " + path);
            return null;
        }

        Mesh baseShapeMesh = model.meshes[0];
        for (int i = 0; i < model.meshes.length; ++i)
        {
            Mesh mesh = model.meshes[i];
            if (mesh.getName().equals("SwitchBaseShape") || mesh.getName().equals("SwitchStateShape")) continue;
            baseShapeMesh = mesh;
            break;
        }
        
        return baseShapeMesh.getMinVert().add(baseShapeMesh.getMaxVert()).div(2.0f);
    }

    public static ResourceDescriptor getMesh(LoadContext context, String path, CostumePieceCategory category)
    {
        return getMesh(context, path, category, null);
    }

    static HashSet<Vector4f> diffuseColors = new HashSet<>();
    static HashSet<Vector4f> specularColors = new HashSet<>();
    static HashSet<Float> coefficients = new HashSet<>();
    static HashSet<Integer> hashes = new HashSet<>();

    public static ResourceDescriptor getMesh(LoadContext context, String path,
                                             CostumePieceCategory category, ArrayList<Integer> regionIDsToHide)
    {
        boolean isCostume = category != null;
        path = Archive.resolve(path);
        String rowPath = ("gamedata/psp" + path).replace(".model", ".mol");
        GUID guid = Crypto.makeGUID(rowPath);
        if (context.exists(guid))
            return new ResourceDescriptor(guid, ResourceType.MESH);

        byte[] modelData = context.loader.getPSPResource(path);
        if (modelData == null)
        {
            System.out.println("Couldn't retrieve file for " + path);
            return null;
        }

        Model model = null;
        try { model = new Model(modelData); }
        catch (Exception ex)
        {
            System.out.println("Failed to load serialized model: " + path);
            return null;
        }


        ResourceDescriptor[] materials = new ResourceDescriptor[model.materials.length];


        // System.out.println(path);

        for (int i = 0; i < materials.length; ++i)
        {
            Material material = model.materials[i];

            String texture = model.textures[model.materials[i].textureIndex];
            if (texture.endsWith("/white.16.mip"))
                materials[i] = new ResourceDescriptor(21845, ResourceType.GFX_MATERIAL);
            else if (texture.endsWith("/cardboard.16.mip"))
                materials[i] = new ResourceDescriptor(10803, ResourceType.GFX_MATERIAL);
            else if (texture.endsWith("/black_seethrough.mip"))
                materials[i] = new ResourceDescriptor(26618, ResourceType.GFX_MATERIAL);
            else materials[i] = ResourceConverter.getMaterial(context, texture, material.getHash(), false);
        }

        Skin[] skins = new Skin[model.skins.length];
        for (int i = 0; i < skins.length; ++i)
        {
            byte[] skinData = context.loader.getPSPResource(model.skins[i]);
            if (skinData == null)
            {
                System.out.println("Couldn't retrieve file for " + path);
                return null;
            }
            Skin skin = null;
            try { skin = new Skin(skinData); }
            catch (Exception ex)
            {
                System.out.println("Failed to load serialized skin: " + path);
                return null;
            }
            skins[i] = skin;
        }

        int totalVertCount = 0;
        int totalIndexCount = 0;
        for (Mesh mesh : model.meshes)
        {
            totalVertCount += mesh.getVertexCount();
            totalIndexCount += mesh.getIndices().length;
        }

        ArrayList<Primitive> primitives = new ArrayList<>();

        Vector2f minUV = new Vector2f(Float.POSITIVE_INFINITY, Float.POSITIVE_INFINITY);
        Vector2f maxUV = new Vector2f(Float.NEGATIVE_INFINITY, Float.NEGATIVE_INFINITY);

        MemoryOutputStream vertexStream = new MemoryOutputStream(0x10 * totalVertCount);
        MemoryOutputStream attributeStream = new MemoryOutputStream(0x8 * totalVertCount);
        MemoryOutputStream skinningStream = new MemoryOutputStream(0x10 * totalVertCount);
        MemoryOutputStream indexStream = new MemoryOutputStream(0x2 * totalIndexCount);

        vertexStream.setLittleEndian(false);
        attributeStream.setLittleEndian(false);
        skinningStream.setLittleEndian(false);
        indexStream.setLittleEndian(false);

        Matrix4f[] joints = null;
        RMesh sackboy = null;
        int[] sackboyBoneRemap = null;
        if (isCostume)
        {
            sackboy = context.loader.loadPS3Resource(1087, RMesh.class);
            Skeleton skeleton = new Skeleton(context.loader.getPSPResource("meshes/sackboy" +
                                                                           "/sackboy.skeleton"));

            sackboyBoneRemap = new int[skeleton.joints.length];
            for (int i = 0; i < skeleton.joints.length; ++i)
            {
                String name = "bone_" + ((long) skeleton.joints[i].id & 0xffffffffL);
                String remap = BONE_REMAP.get(name);
                int index = Bone.indexOf(sackboy.getBones(),
                    RAnimation.calculateAnimationHash(remap));
                sackboyBoneRemap[i] = index;
            }

            Matrix4f[] inverses = new Matrix4f[skeleton.joints.length];
            joints = new Matrix4f[inverses.length];
            for (int i = 0; i < inverses.length; ++i)
                inverses[i] = skeleton.joints[i].getGlobalTransform().invert(new Matrix4f());
            skeleton.joints[0].rotation = new Quaternionf(0.707f, 0.0f, 0.0f, 0.707f);

            skeleton.joints[7].rotation.rotateZ((float) Math.toRadians(25.0f));
            skeleton.joints[12].rotation.rotateZ((float) Math.toRadians(25.0f));

            for (int i = 0; i < joints.length; ++i)
                joints[i] = skeleton.joints[i].getGlobalTransform().mul(inverses[i],
                    new Matrix4f());
        }


        Mesh baseShapeMesh = model.meshes[0];
        for (int i = 0; i < model.meshes.length; ++i)
        {
            Mesh mesh = model.meshes[i];
            if (mesh.getName().equals("SwitchBaseShape") || mesh.getName().equals("SwitchStateShape")) continue;
            baseShapeMesh = mesh;
            break;
        }

        Vector3f meshOffset = MESH_BONE_OFFSET.getOrDefault(Archive.resolve(path), new Vector3f().zero());
        int minVert = 0, firstIndex = 0;
        for (int i = 0; i < model.meshes.length; ++i)
        {
            Mesh mesh = model.meshes[i];
            Skin skin = skins.length != 0 ? skins[i] : null;

            VertexData[] stream = mesh.getVertexData(skin, true)[0];
            for (VertexData vertex : stream)
            {

                Vector3f position = vertex.position;
                Vector3f normal = vertex.normal;
                int[] blendIndices = new int[] { 0, 0, 0, 0 };

                if (isCostume)
                {
                    position = new Vector3f();
                    normal = new Vector3f();
                    for (int bone = 0; bone < 4; ++bone)
                    {
                        if (vertex.joints[bone] == -1) break;
                        blendIndices[bone] = sackboyBoneRemap[vertex.joints[bone]];
                        Matrix4f bmat = new Matrix4f(joints[vertex.joints[bone]]);
                        Vector4f wpos = new Vector4f(vertex.position, 1.0f);
                        Vector4f lpos = new Vector4f(
                            bmat.getRow(0, new Vector4f()).dot(wpos),
                            bmat.getRow(1, new Vector4f()).dot(wpos),
                            bmat.getRow(2, new Vector4f()).dot(wpos),
                            bmat.getRow(3, new Vector4f()).dot(wpos)
                        ).mul(vertex.weights[bone]);

                        normal.add(vertex.normal.mul(bmat.get3x3(new Matrix3f()), new Vector3f()).mul(vertex.weights[bone]));
                        position.add(new Vector3f(lpos.x, lpos.y, lpos.z));
                    }

                    position.mul(PartConverter.WORLD_SCALE);
                    normal.normalize();

                }
                else position = position.mul(PartConverter.WORLD_SCALE).add(meshOffset);

                vertexStream.v3(position);
                vertexStream.i32(0xFF);

                int scale = (vertex.weights[1] != 0.0f) ? 0xFE : 0xFF;
                skinningStream.u8(Math.round(vertex.weights[2] * scale));
                skinningStream.u8(Math.round(vertex.weights[1] * scale));
                skinningStream.u8(Math.round(vertex.weights[0] * scale));
                skinningStream.u8(blendIndices[0]); // joint0
                skinningStream.u24(Bytes.packNormal24(normal));
                skinningStream.u8(blendIndices[1]); // joint1
                skinningStream.u24(0); // tangent
                skinningStream.u8(blendIndices[2]); // joint2
                skinningStream.u24(0); // smooth normal
                skinningStream.u8(blendIndices[3]); // joint3

                vertex.uv.y = 1.0f - vertex.uv.y;
                if (vertex.uv.x > maxUV.x) maxUV.x = vertex.uv.x;
                if (vertex.uv.y > maxUV.y) maxUV.y = vertex.uv.y;

                if (vertex.uv.x < minUV.x) minUV.x = vertex.uv.x;
                if (vertex.uv.y < minUV.y) minUV.y = vertex.uv.y;


                attributeStream.v2(vertex.uv);
            }

            int indexCount = mesh.getIndices().length;
            for (int index : mesh.getIndices())
                indexStream.u16(index + minVert);

            Primitive primitive = new Primitive(materials[i], minVert, minVert + stream.length,
                firstIndex, indexCount);
            primitive.setRegion(RAnimation.calculateAnimationHash(mesh.getName()));
            primitives.add(primitive);

            firstIndex += indexCount;
            minVert += stream.length;
        }

        Bone bone = new Bone("BocchiTheRock!");
        bone.skinPoseMatrix = new Matrix4f().identity();
        bone.invSkinPoseMatrix = bone.skinPoseMatrix.invert(new Matrix4f());

        RMesh mesh = new RMesh(
            new byte[][] { vertexStream.getBuffer(), skinningStream.getBuffer() },
            attributeStream.getBuffer(),
            indexStream.getBuffer(),
            isCostume ? sackboy.getBones() : new Bone[] { bone }
        );

        if (regionIDsToHide != null && regionIDsToHide.size() != 0)
            mesh.setRegionIDsToHide(regionIDsToHide.stream().mapToInt(Integer::valueOf).toArray());

        if (isCostume)
            mesh.setCostumeCategoriesUsed(CostumePieceCategory.getFlags(EnumSet.of(category)));

        mesh.setPrimitives(primitives);
        mesh.setMinUV(minUV);
        mesh.setMaxUV(maxUV);
        mesh.calculateBoundBoxes(!isCostume);


        byte[] resourceData = SerializedResource.compress(mesh.build(
            new Revision(0x132),
            CompressionFlags.USE_NO_COMPRESSION
        ));

        context.loader.createEntryFromData(rowPath, guid.getValue(), resourceData);

        return new ResourceDescriptor(guid, ResourceType.MESH);
    }
}
