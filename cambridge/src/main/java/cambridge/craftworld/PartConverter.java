package cambridge.craftworld;

import cambridge.craftworld.Biffloader.LoadContext;
import cambridge.enums.ElementType;
import cambridge.enums.LanguageID;
import cambridge.enums.ObjectType;
import cambridge.resources.Archive;
import cambridge.resources.Biff.Element;
import cambridge.structures.*;
import cambridge.structures.data.UID;
import cambridge.structures.joints.*;
import cambridge.structures.joints.bolts.Bolt;
import cambridge.structures.joints.bolts.MotorBolt;
import cambridge.structures.joints.bolts.SprungBolt;
import cambridge.structures.joints.bolts.WobbleBolt;
import cambridge.structures.switches.MagneticKey;
import cambridge.structures.switches.StickerSwitch;
import cambridge.util.Crypto;
import cwlib.enums.*;
import cwlib.resources.RPlan;
import cwlib.structs.things.Thing;
import cwlib.structs.things.components.EggLink;
import cwlib.structs.things.components.decals.Decal;
import cwlib.structs.things.components.script.ScriptInstance;
import cwlib.structs.things.components.script.ScriptObjectUID;
import cwlib.structs.things.components.switches.SwitchTarget;
import cwlib.structs.things.parts.*;
import cwlib.types.data.GUID;
import cwlib.types.data.ResourceDescriptor;

import org.joml.Matrix3f;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

public class PartConverter
{
    /**
     * LBP PSP is 1/1000 the scale of LBP PS3.
     */
    public static final float WORLD_SCALE = 1000.0f;


    /**
     * LBP PSP -> LBP PS3 GameObject positional offset.
     */
    public static final Vector3f WORLD_OFFSET = new Vector3f(
        20037.6868282f,
        1654.03056097f,
        // 39.999994636f
        100.0f
    );

    private static int ClampZToBand(float z)
    {
        return (int)(Math.floor(((z + 500.0) / 200.0) + 0.5) * 200.0) - 500;
    }

    private static float UpdateFrontBack(PShape shape, float f, float b)
    {
        int thickness = (int) Math.abs(f - b);
        int front = ClampZToBand(f);
        int back = ClampZToBand(b);

        if (front < back)
        {
            int temp = front;
            front = back;
            back = temp;
        }

        if (front == back)
        {
            if (thickness <= 20)
            {
                back -= 10;
                front += 10;
            }
            else if (thickness <= 40)
            {
                // I'm too tired to read disassembly right now, just a max macro
                int center = back - (back + 500 & -((back < -500) ? 1 : 0));
                center = center + 100 & -((center < -100) ? 1 : 0);

                back = center - 90;
                front = center - 50;
            }
            else
            {
                back += 10;
                front += 190;
            }
        }
        else
        {
            if (thickness > 140)
            {
                back += 10;
                front -= 10;
            }
            else
            {
                back += 30;
                front -= 30;
            }
        }

        shape.thickness = (front - back) / 2.0f;
        return (float) front - shape.thickness;
    }
    
    public static Thing addLandscape(LoadContext context, LandChunkEntity landscape)
    {
        // A layer is 0.22 units
        // BACK THIN: Z = -0.44, Depth = 0.02
        // BACK: Z = -0.24, Depth = 0.2
        // MIDDLE THIN: Z = -0.22, Depth = 0.02
        // FRONT: Z = -0.02, Depth = 0.2
        // FRONT THIN: Z = 0.0, Depth = 0.02
        // ALL: Z = -0.02, Depth = 0.42

        Vector3f com = new Vector3f();
        for (Vector3f vertex : landscape.mesh.vertices)
        {
            vertex.mul(WORLD_SCALE);
            vertex.z = 0.0f;
            com.add(vertex);
        }

        com.div(landscape.mesh.vertices.length);

        MaterialLookupData material = MaterialLookupData.MLUT.getOrDefault(
            (landscape.lethalType == cambridge.enums.LethalType.GAS) ?
                landscape.mesh.oldMaterialUID : landscape.mesh.materialUID
            , MaterialLookupData.MLUT.get(MaterialLookupData.DEFAULT_MATERIAL_UID));

        Thing thing = context.getEmptyThing();

        PShape shape = new PShape(landscape.mesh.vertices);
        shape.bevelSize = material.bevelSize;
        shape.COM.setColumn(3, new Vector4f(com, 1.0f));


        if (landscape.decals.length != 0)
        {
            PStickers stickers = new PStickers();
            for (cambridge.structures.Decal decal : landscape.decals)
            {
                stickers.decals.add(new Decal(
                    ResourceConverter.getTexture(context, decal.texture),
                    decal.u * 2.0f,
                    1.0f - (decal.v * 2.0f),
                    decal.scale,
                    -decal.angle,
                    decal.flipped
                ));
            }

            thing.setPart(Part.STICKERS, stickers);
        }

        shape.material = landscape.mesh.objectType == ObjectType.STATIC 
        ? material.staticPhysicsResource : material.physicsResource;

        // if (landscape.mesh.objectType == ObjectType.STATIC)
        //     thing.<PBody>getPart(Part.BODY).frozen = -1;

        thing.setPart(Part.BODY, new PBody());
        thing.setPart(Part.SHAPE, shape);
        thing.setPart(
            Part.GENERATED_MESH,
            new PGeneratedMesh(material.renderMaterial, material.bevel)
        );

        // if (landscape.mesh.massDepth >= 0.3)
        // {
        //     shape.massDepth = 2.0f;
        //     shape.thickness = 190.0f;
        // }
        // else if (landscape.mesh.massDepth >= 0.2)
        // {
        //     shape.massDepth = 1.0f;
        //     shape.thickness = 90.0f;
        // }
        // else
        // {
        //     shape.massDepth = 0.2f;
        //     shape.thickness = 10.0f;
        // }

        LethalType lethality = LethalType.NOT;
        switch (landscape.lethalType)
        {
            case ELECTRICITY:
                lethality = LethalType.ELECTRIC;
                break;
            case FIRE:
                lethality = LethalType.FIRE;
                break;
            case GAS:
                lethality = LethalType.GAS;
                break;
            case SPIKE:
                lethality = LethalType.SPIKE;
                break;
            case CRUSH:
                lethality = LethalType.CRUSH;
                break;
            default:
                break;
        }
        shape.lethalType = lethality;

        Vector3f translation =
            landscape.position.mul(WORLD_SCALE, new Vector3f()).add(WORLD_OFFSET);

        float front = (float) translation.z;
        float back = front - (landscape.mesh.massDepth * WORLD_SCALE);
        shape.massDepth = (front - back) / 200.0f;
        translation.z = UpdateFrontBack(shape, front, back);


        
        // if (shape.massDepth == 2.0f) translation.z = -100.0f;
        // else if (shape.massDepth == 1.0f)
        //     translation.z = (translation.z > -50.0f) ? 0.0F : -200.0f;
        // else
        // {
        //     if (translation.z < -200)
        //         translation.z = -300.0f;
        //     else if (translation.z < 0)
        //         translation.z = -100;
        //     else
        //         translation.z = 100;
        // }

        Matrix4f transform = new Matrix4f().identity().setTranslation(translation);
        transform.rotateZ(landscape.angle);
        thing.setPart(Part.POS, new PPos(transform));

        context.lookup.put(landscape.uid, thing);
        context.things.add(thing);

        return thing;
    }

    public static Thing addGameAsset(LoadContext context, PS3Asset asset, GameObject object)
    {
        Thing thing = context.loader.getGameAsset(context, asset)[0];

        PPos pos = thing.getPart(Part.POS);
        Matrix4f wpos = pos.worldPosition;
        wpos.setTranslation(
            object.position.mul(WORLD_SCALE, new Vector3f()).add(WORLD_OFFSET)
        );
        wpos.rotateZ(object.angle);

        pos.worldPosition = wpos;
        pos.localPosition = wpos;

        context.lookup.put(object.uid, thing);
        context.things.add(thing);

        return thing;
    }

    // public static HashMap<String, ArrayList<Vector3f>> modelOffsetPaths = new HashMap<>();

    public static Thing addMesh(LoadContext context, MeshEntity object)
    {
        // if (object.model.toLowerCase().contains("gadgets")) return null;


        Vector3f com = new Vector3f();
        for (Vector3f vertex : object.mesh.vertices)
        {
            vertex.mul(WORLD_SCALE);
            vertex.z = 0.0f;
            com.add(vertex);
        }

        com.div(object.mesh.vertices.length);

        Thing thing = context.getEmptyThing();

        PPos pos = new PPos();
        Matrix4f wpos = pos.worldPosition;
        Vector3f translation = object.position.mul(WORLD_SCALE, new Vector3f()).add(WORLD_OFFSET);
        
        PShape shape = new PShape(object.mesh.vertices);
        shape.COM.setColumn(3, new Vector4f(com, 1.0f));
        float front = (float) translation.z;
        float back = front - (object.mesh.massDepth * WORLD_SCALE);
        translation.z = UpdateFrontBack(shape, front, back);

        // ArrayList<Vector3f> offsets = modelOffsetPaths.get(object.model);
        // if (offsets == null)
        // {
        //     offsets = new ArrayList<>();
        //     modelOffsetPaths.put(object.model, offsets);
        // }

        // Vector3f offset = object.modelPositionOffset.mul(WORLD_SCALE, new Vector3f());
        // offset.z += shape.thickness;

        // boolean hasDuplicate = false;
        // for (Vector3f v : offsets)
        // {
        //     if (v.equals(offset, 0.1f))
        //     {
        //         hasDuplicate = true;
        //         break;
        //     }
        // }

        // if (!hasDuplicate)
        //     offsets.add(offset);

        
        // float massDepth =
        //     (float) Math.floor(((object.mesh.massDepth * 1000.0f) / 200.0f) + 0.5f) * 200.0f;
        // if (massDepth >= 400.0f)
        // {
        //     shape.massDepth = 2.0f;
        //     shape.thickness = 190.0f;
        //     translation.z = -100.0f;
        // }
        // else if (massDepth >= 200.0f)
        // {
        //     shape.massDepth = 1.0f;
        //     shape.thickness = 90.0f;
        //     translation.z = (translation.z > -50.0f) ? 0.0F : -200.0f;
        // }
        // else
        // {
        //     shape.massDepth = 0.2f;
        //     shape.thickness = 10.0f;
        //     if (translation.z < -200)
        //         translation.z = -300.0f;
        //     else if (translation.z < 0)
        //         translation.z = -100;
        //     else
        //         translation.z = 100;
        // }

        wpos.setTranslation(translation);
        wpos.rotateZ(object.angle);

        pos.worldPosition = wpos;
        pos.localPosition = wpos;

        thing.setPart(Part.POS, pos);
        thing.setPart(Part.BODY, new PBody());

        // if (object.mesh.objectType == ObjectType.STATIC)
        //     thing.<PBody>getPart(Part.BODY).frozen = -1;
        
        MaterialLookupData material = MaterialLookupData.MLUT.getOrDefault(
            (object.lethalType == cambridge.enums.LethalType.GAS) ?
                object.mesh.oldMaterialUID : object.mesh.materialUID
            , MaterialLookupData.MLUT.get(MaterialLookupData.DEFAULT_MATERIAL_UID));


        shape.material = object.mesh.objectType == ObjectType.STATIC 
        ? material.staticPhysicsResource : material.physicsResource;

        PRenderMesh mesh = new PRenderMesh(
            ResourceConverter.getMesh(context, object.model, null),
            new Thing[] { thing }
        );

        thing.setPart(Part.SHAPE, shape);
        thing.setPart(Part.RENDER_MESH, mesh);


        context.things.add(thing);

        // dumb hack
        if (object.model.toLowerCase().contains("scoreboard"))
        {

            // try to get 


            Thing screen = context.getEmptyThing();
            context.things.add(screen);

            Matrix4f baseWorldPos = pos.worldPosition;
            Matrix4f screenWorldPos = new Matrix4f(baseWorldPos).translate(0.0f, 390.0f, -270.0f).rotateZ((float)Math.PI).rotateX(-1.570796f).scale(0.18f, 0.18f, 0.18f);
            pos.localPosition = screenWorldPos.invert(new Matrix4f()).mul(baseWorldPos);


            Matrix3f screenScaleRotMat = screenWorldPos.get3x3(new Matrix3f());
            Matrix3f screenScaleRotMatInv = screenScaleRotMat.invert(new Matrix3f());

            PShape screenShape = new PShape();
            screen.setPart(Part.BODY, new PBody());
            
            screenShape.material = object.mesh.objectType == ObjectType.STATIC 
                ? material.staticPhysicsResource : material.physicsResource;
            screen.setPart(Part.SHAPE, screenShape);

            screenShape.polygon.requiresZ = true;
            screenShape.polygon.vertices = new Vector3f[]
            {
                new Vector3f(-100.551f,-1.79028f,0.0f),
                new Vector3f(-96.4487f,82.7277f,0.0f),
                new Vector3f(-91.939f,92.0786f,0.0f),
                new Vector3f(-85.2065f,95.7196f,0.0f),
                new Vector3f(-73.3364f,96.9724f,0.0f),
                new Vector3f(15.2109f,99.3813f,0.0f),
                new Vector3f(70.0938f,99.3813f,0.0f),
                new Vector3f(158.641f,96.9724f,0.0f),
                new Vector3f(170.511f,95.7196f,0.0f),
                new Vector3f(177.244f,92.0786f,0.0f),
                new Vector3f(181.753f,82.7277f,0.0f),
                new Vector3f(185.855f,-1.79028f,0.0f),
                new Vector3f(181.862f,-71.614f,0.0f),
                new Vector3f(176.126f,-84.0135f,0.0f),
                new Vector3f(162.756f,-89.2237f,0.0f),
                new Vector3f(94.2676f,-94.2075f,0.0f),
                new Vector3f(81.0537f,-94.0754f,0.0f),
                new Vector3f(74.2661f,-93.1014f,0.0f),
                new Vector3f(79.811f,-113f,0.0f),
                new Vector3f(5.49365f,-113f,0.0f),
                new Vector3f(11.0381f,-93.1014f,0.0f),
                new Vector3f(4.25049f,-94.0754f,0.0f),
                new Vector3f(-8.96289f,-94.2075f,0.0f),
                new Vector3f(-77.4517f,-89.2237f,0.0f),
                new Vector3f(-90.8213f,-84.0135f,0.0f),
                new Vector3f(-96.5571f,-71.614f,0.0f)
            };
            screenShape.polygon.loops = new int[] { screenShape.polygon.vertices.length };

            screenShape.massDepth = 0.2f;
            screenShape.thickness = 10.0f;
            screenShape.COM = new Matrix4f(screenScaleRotMatInv);

            for (Vector3f vertex : screenShape.polygon.vertices)
            {
                vertex.x -= 45.0f;
                vertex.y += 6.0f;
                vertex.mul(115.0f);
                vertex.mul(screenScaleRotMat);
            }

            screen.setPart(Part.POS, new PPos(screenWorldPos));
            screen.setPart(Part.SCRIPT, new PScript(new ResourceDescriptor(11599, ResourceType.SCRIPT)));


            thing.setPart(Part.TRIGGER, new PTrigger(TriggerType.RADIUS, 500.0f));
            thing.setPart(Part.SCRIPT, new PScript(new ResourceDescriptor(17789, ResourceType.SCRIPT)));
            thing.setPart(Part.BODY, null);
            
            screen.setPart(Part.RENDER_MESH, thing.getPart(Part.RENDER_MESH));
            thing.setPart(Part.RENDER_MESH, null);

            Thing scoreboardGroupThing = context.getEmptyThing();
            scoreboardGroupThing.setPart(Part.GROUP, new PGroup());
            context.things.add(scoreboardGroupThing);

            thing.parent = screen;

            thing.groupHead = scoreboardGroupThing;
            screen.groupHead = scoreboardGroupThing;
        }
        else context.lookup.put(object.uid, thing);

        return thing;
    }

    public static Thing addJetpack(LoadContext context, Jetpack jetpack)
    {
        Thing thing = addGameAsset(context, PS3Asset.JETPACK, jetpack);

        PScript script = thing.getPart(Part.SCRIPT);
        script.instance.memberData.put("TetherLength", jetpack.tetherLength * 50.0f * 100.0f);

        return thing;
    }

    public static Thing addMusic(LoadContext context, Music music)
    {
        // Bit of a weird way, but let's just pull the plan that we generated earlier
        long audioPlanKey = ResourceConverter.AUDIO_BIFF_TO_KEY.get(music.path);
        RPlan plan = context.loader.loadPS3Resource(audioPlanKey, RPlan.class);

        // There should only be a singular thing in the plan anyway
        Thing thing = plan.getThings()[0];

        context.reassignThing(thing);

        thing.<PTrigger>getPart(Part.TRIGGER).radiusMultiplier = music.radius * WORLD_SCALE;

        ScriptInstance instance = thing.<PScript>getPart(Part.SCRIPT).instance;
        instance.addField("StartPoint", music.start);
        instance.addField("Master_Volume", music.volume);
        instance.addField("Pair_0_Volume", music.volume);
        instance.addField("HideInPlayMode", music.visibility != 0);

        PPos pos = thing.getPart(Part.POS);
        Matrix4f wpos = pos.worldPosition;

        Vector3f translation = music.position.mul(WORLD_SCALE, new Vector3f()).add(WORLD_OFFSET);
        if (translation.z < -200)
            translation.z = -300.0f;
        else if (translation.z < 00)
            translation.z = -100;
        else
            translation.z = 100;
        
        wpos.setTranslation(translation);
        wpos.rotateZ(music.angle);

        pos.worldPosition = wpos;
        pos.localPosition = wpos;

        context.lookup.put(music.uid, thing);
        context.things.add(thing);

        return thing;
    }

    public static Thing addScoreBubble(LoadContext context, ScoreBubble bubble)
    {
        Thing thing = context.loader.getGameAsset(context, PS3Asset.SCORE_BUBBLE)[0];

        PPos pos = thing.getPart(Part.POS);
        Matrix4f wpos = pos.worldPosition;

        Vector3f translation = bubble.position.mul(WORLD_SCALE, new Vector3f()).add(WORLD_OFFSET);
        translation.z -= thing.<PShape>getPart(Part.SHAPE).thickness;
        wpos.setTranslation(translation);
        
        pos.worldPosition = wpos;
        pos.localPosition = wpos;

        context.lookup.put(bubble.uid, thing);
        context.things.add(thing);

        return thing;
    }

    public static Thing addPrizeBubble(LoadContext context, PrizeBubble bubble)
    {
        Thing thing = context.loader.getGameAsset(context, PS3Asset.PRIZE_BUBBLE)[0];

        PPos pos = thing.getPart(Part.POS);
        Matrix4f wpos = pos.worldPosition;
        Vector3f translation = bubble.position.mul(WORLD_SCALE, new Vector3f()).add(WORLD_OFFSET);
        translation.z -= thing.<PShape>getPart(Part.SHAPE).thickness;
        wpos.setTranslation(translation);

        pos.worldPosition = wpos;
        pos.localPosition = wpos;

        PGameplayData data = thing.getPart(Part.GAMEPLAY_DATA);
        ResourceDescriptor item = ResourceConverter.getItem(context, bubble.prize);
        if (item != null)
        {
            data.eggLink = new EggLink();
            data.eggLink.plan = item;
        }

        context.lookup.put(bubble.uid, thing);
        context.things.add(thing);

        return thing;
    }

    public static Thing addMagicMouth(LoadContext context, MagicMouth speech)
    {
        Thing[] things = context.loader.getGameAsset(context, PS3Asset.MAGIC_MOUTH);
        context.lookup.put(speech.uid, things[0]);

        PPos rootPosition = things[0].getPart(Part.POS);

        Vector3f translation = speech.position.mul(WORLD_SCALE, new Vector3f()).add(WORLD_OFFSET);
        // if (translation.z < -200)
        //     translation.z = -300.0f;
        // else if (translation.z < 00)
        //     translation.z = -100;
        // else
        //     translation.z = 100;
        
        rootPosition.worldPosition.setTranslation(translation);

        rootPosition.worldPosition.rotateZ(speech.angle);
        rootPosition.localPosition = rootPosition.worldPosition;

        for (Thing thing : things)
        {
            if (thing != things[0])
            {
                PPos pos = thing.getPart(Part.POS);
                pos.worldPosition = rootPosition.worldPosition.mul(pos.localPosition,
                    new Matrix4f());
            }

            context.things.add(thing);
        }

        float sx = things[1].<PPos>getPart(Part.POS).worldPosition.getScale(new Vector3f()).x;
        things[1].<PTrigger>getPart(Part.TRIGGER).radiusMultiplier =
            (speech.radius * WORLD_SCALE) / sx;

        ScriptInstance instance = things[0].<PScript>getPart(Part.SCRIPT).instance;
        instance.setField("HideInPlayMode", speech.visibility != 0);

        // If we're in a developer level, load the translations into the translation table
        if (context.isDeveloperLevel)
        {
            String tag = String.format("PSP_%s_%d_SPEECH", context.developerLevelTag, things[0].UID);
            for (String languageTag : speech.translations.keySet())
            {
                int languageId = LanguageID.CODE_TO_ID.getOrDefault(languageTag, -1);
                if (languageId == -1)
                    throw new RuntimeException("Unhandled language ID, this is probably my fault?");

                String text = speech.translations.get(languageTag);
                context.loader.addTranslationTag(languageId, tag, text);
            }

            ScriptObjectUID object = new ScriptObjectUID(ScriptObjectType.STRINGW, tag);
            instance.setField("Text", object);
            instance.setField("OriginalText", object);
            instance.setField("UserEntered", false);
        }
        else
        {
            // TODO: Double check what translation field actually gets set for user text,for now
            //  just pull US
            ScriptObjectUID object = new ScriptObjectUID(ScriptObjectType.STRINGW,
                speech.translations.getOrDefault("us", ""));
            instance.setField("Text", object);
            instance.setField("OriginalText", object);
            instance.setField("UserEntered", true);
        }


        if (speech.cutscene)
        {
            instance.setField("CameraEnabled", true);

            PCameraTweak tweak = new PCameraTweak();
            tweak.pitchAngle = new Vector3f(speech.pitchAngle.y, speech.pitchAngle.x, 0.0f);
            tweak.targetBox =
                new Vector4f(speech.targetBox.sub(speech.position, new Vector3f()).mul(WORLD_SCALE), 0.0f);
            tweak.targetBox.z = 0.0f;
            tweak.cameraType = 2;
            tweak.activationLimit = 1.0f;
            tweak.zoomDistance = WORLD_SCALE;

            things[0].setPart(Part.CAMERA_TWEAK, tweak);
        }

        return things[0];
    }

    public static Thing addMagneticKey(LoadContext context, MagneticKey key)
    {
        Thing thing = context.loader.getGameAsset(context, PS3Asset.TAG)[0];
        Vector3f translation = key.position.mul(WORLD_SCALE, new Vector3f()).add(WORLD_OFFSET);
        // if (translation.z < -200)
        //     translation.z = -300.0f;
        // else if (translation.z < 00)
        //     translation.z = -100;
        // else
        //     translation.z = 100;

        PPos pos = thing.getPart(Part.POS);
        Matrix4f wpos = pos.worldPosition;
        wpos.setTranslation(translation);

        pos.worldPosition = wpos;
        pos.localPosition = wpos;

        PSwitchKey part = thing.getPart(Part.SWITCH_KEY);
        part.colorIndex = key.colorIndex - 2;

        PRenderMesh mesh = thing.getPart(Part.RENDER_MESH);
        Color color = Color.getHSBColor(((part.colorIndex % 8) / 9.0f) + (2.0f / 3.0f), 0.75f,
            1.0f);
        mesh.editorColor
            =
            (0xFF << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | (color.getBlue() << 0);

        context.lookup.put(key.uid, thing);
        context.things.add(thing);

        return thing;
    }

    public static Thing addCheckpoint(LoadContext context, GameObject obj, boolean isEntrance)
    {
        PS3Asset asset = isEntrance ? PS3Asset.ENTRANCE : PS3Asset.CHECKPOINT;
        Thing[] things = context.loader.getGameAsset(context, asset);
        context.lookup.put(obj.uid, things[0]);

        PPos rootPosition = things[0].getPart(Part.POS);


        Vector3f translation = obj.position.mul(WORLD_SCALE, new Vector3f()).add(WORLD_OFFSET);
        if (translation.z >= -100) translation.z = -50.0f;
        else translation.z = -250.0f;

        rootPosition.worldPosition.setTranslation(translation);

        rootPosition.worldPosition.rotateZ(obj.angle);
        rootPosition.localPosition = rootPosition.worldPosition;

        for (Thing thing : things)
        {
            if (thing != things[0])
            {
                PPos pos = thing.getPart(Part.POS);
                pos.worldPosition = rootPosition.worldPosition.mul(pos.localPosition,
                    new Matrix4f());
            }

            context.things.add(thing);
        }

        return things[0];
    }

    public static Thing addCameraZone(LoadContext context, CameraGadget camera)
    {
        Thing thing = context.loader.getGameAsset(context, PS3Asset.CAMERA_ZONE)[0];
        Vector3f translation = camera.position.mul(WORLD_SCALE, new Vector3f()).add(WORLD_OFFSET);
        // if (translation.z < -200)
        //     translation.z = -300.0f;
        // else if (translation.z < 00)
        //     translation.z = -100;
        // else
        //     translation.z = 100;

        PPos pos = thing.getPart(Part.POS);
        Matrix4f wpos = pos.worldPosition;
        wpos.setTranslation(translation);
        wpos.rotateX((float) Math.toRadians(90.0f));

        pos.worldPosition = wpos;
        pos.localPosition = wpos;

        PCameraTweak tweak = thing.getPart(Part.CAMERA_TWEAK);

        tweak.pitchAngle = new Vector3f(camera.pitchAngle.y, camera.pitchAngle.x, 0.0f);
        tweak.targetBox =
            new Vector4f(camera.targetPosition.sub(camera.position, new Vector3f()).mul(WORLD_SCALE), 0.0f);
        tweak.targetBox.z = 0.0f;

        Vector3f triggerPosition = camera.triggerPosition.mul(WORLD_SCALE, new Vector3f());
        tweak.triggerBox = new Vector4f(
            triggerPosition.x,
            triggerPosition.y,
            camera.triggerBox.x * WORLD_SCALE,
            camera.triggerBox.y * WORLD_SCALE
        );

        tweak.zoomDistance = camera.zoomDistance * WORLD_SCALE;
        tweak.positionFactor = 1.0f - camera.trackPlayer;

        context.lookup.put(camera.uid, thing);
        context.things.add(thing);

        return thing;
    }

    public static void addJoint(LoadContext context, Element element)
    {
        PS3Asset asset = null;

        switch (element.getType())
        {
            case PISTON:
                asset = PS3Asset.PISTON;
                break;
            case STRING:
                asset = PS3Asset.STRING;
                break;
            case ROD:
                asset = PS3Asset.ROD;
                break;
            case ELASTIC:
                asset = PS3Asset.ELASTIC;
                break;
            case WINCH:
                asset = PS3Asset.WINCH;
                break;
            case SPRING:
                asset = PS3Asset.SPRING;
                break;

            case BOLT:
                asset = PS3Asset.BOLT;
                break;
            case MOTOR_BOLT:
                asset = PS3Asset.MOTOR_BOLT;
                break;
            case WOBBLE_BOLT:
                asset = PS3Asset.WOBBLE_BOLT;
                break;
            case SPRUNG_BOLT:
                asset = PS3Asset.SPRUNG_BOLT;
                break;

            default: return;
        }

        Thing thing = context.loader.getGameAsset(context, asset)[0];

        // In case the joint is attached to an object in the plan, remove any
        // attachments.
        thing.groupHead = null;
        thing.parent = null;

        Joint joint = element.load();

        Thing a = context.lookup.get(joint.a);
        Thing b = context.lookup.get(joint.b);

        if (a == null || b == null)
        {
            //System.out.println("[AddJoints] Couldn't find connection objects for joint!");
            return;
        }

        // 101.25 for aCOM, -100 for Z for whatever reason

        Matrix4f aWorldPos = a.<PPos>getPart(Part.POS).worldPosition;
        Matrix4f bWorldPos = b.<PPos>getPart(Part.POS).worldPosition;

        Matrix3f aScaleRotMat = aWorldPos.get3x3(new Matrix3f()).invert();
        Matrix3f bScaleRotMat = bWorldPos.get3x3(new Matrix3f()).invert();

        Vector3f aCOM = aWorldPos.getTranslation(new Vector3f());
        Vector3f bCOM = bWorldPos.getTranslation(new Vector3f());

        PJoint part = thing.getPart(Part.JOINT);
        if (joint.visibility != 0)
            part.hideInPlayMode = true;
        part.a = a;
        part.b = b;
        part.angle = 0.0f;

        part.aContact =
            joint.contactA.mul(WORLD_SCALE, new Vector3f()).add(WORLD_OFFSET).sub(aCOM);
        part.bContact =
            joint.contactB.mul(WORLD_SCALE, new Vector3f()).add(WORLD_OFFSET).sub(bCOM);

        part.aContact.z = 0.0f;
        part.bContact.z = 0.0f;

        // part.aContact.add(joint.aDirection.mul(-25.0f, new Vector3f()));
        // part.bContact.add(joint.bDirection.mul(-25.0f, new Vector3f()));

        part.strength = 1.0f;
        if (joint.isBolt)
        {
            part.renderScale = 0.4f;
            part.length = 0.0f;
            part.slideDir = new Vector3f().zero();

            // Need to calculate the B contact if we're using an older version,
            // since they didn't store the contact point, or maybe they did and old me
            // never noticed, who knows.
            if (!Joint.USE_V013)
            {
                Vector3f boltA = aCOM.add(part.aContact, new Vector3f());
                Vector3f boltB = new Vector3f(
                    boltA.x - bCOM.x,
                    boltA.y - bCOM.y,
                    0.0f
                );

                part.bContact = boltB;
            }

            // Set the angle between the contacts
            Matrix4f local = bWorldPos.invert(new Matrix4f()).mul(aWorldPos);
            Quaternionf rotation = local.getNormalizedRotation(new Quaternionf());
            part.angle = rotation.getEulerAnglesXYZ(new Vector3f()).z;
        }
        else
        {
            part.aAngleOffset =
                (float) Math.atan2(joint.aDirection.y, joint.aDirection.x) - 1.57079f;
            part.bAngleOffset =
                (float) Math.atan2(joint.bDirection.y, joint.bDirection.x) + 1.57079f;
            part.slideDir = joint.bDirection.normalize(new Vector3f());
            part.length = joint.length * 50.0f * 20.0f;
        }

        part.aContact.mul(aScaleRotMat);
        part.bContact.mul(bScaleRotMat);

        if (joint instanceof Rod)
            part.stiff = ((Rod) joint).stiff;
        else if (joint instanceof Spring)
        {
            Spring spring = ((Spring) joint);
            part.stiff = spring.stiff;
            float v;
            if (spring.strength < 0.065f)
            {
                v = (float) Math.pow((-0.8f + 1.0f) * (-0.8f + 1.0f) - 0.8f * -4.0f * (spring.strength / 0.065f), 0.5f);
                v = 5.0f * (1.0f / -0.8f) * (((-0.8f - (spring.strength / 0.065f) * -0.8f) + 1.0f) - v);
            }
            else v = (5.0f + ((10.0f - 5.0f) * (spring.strength - 0.065f)) / (0.1f - 0.065f));
            part.strength = (float) Math.pow(v / 10.0f, 3.0);
        }
        else if (joint instanceof Elastic)
        {
            Elastic elastic = ((Elastic) joint);
            part.stiff = elastic.stiff;
            part.strength = (float) Math.pow(elastic.strength * 10.0f, 3.0);
        }
        else if (joint instanceof Winch)
        {
            Winch winch = (Winch) joint;

            part.length = winch.minLength * 50.0f * 20.0f;
            part.animationRange =
                ((joint.length * 50.0f) - (winch.minLength * 50.0f)) * 20.0f;
            if (winch.flipperMotion != 0)
                part.animationPattern = 2;
            if (winch.flipperMotion == 1 || winch.backwards)
                part.animationRange = -part.animationRange;

            part.animationTime = winch.time * 30.0f;
            part.animationPause = winch.pause * 30.0f;
            part.animationPhase = winch.sync * 30.0f;

            part.strength =
                (float) Math.pow(((-1.750184 / (winch.strength + 0.1750153) + 10.00018) / 10.0f), 3.0);
        }
        else if (joint instanceof Piston)
        {
            Piston piston = (Piston) joint;


            // pfVar[0] = 0.0f
            // pfVar[1] = 1.0f
            // pfVar[2] = 0.0f
            // pfVar[3] = 50.0f

            part.length = piston.minLength * 50.0f * 20.0f;
            part.animationRange =
                ((piston.length * 50.0f) - (piston.minLength * 50.0f)) * 20.0f;


            if (piston.flipperMotion != 0)
                part.animationPattern = 2;
            if (piston.flipperMotion == 1 || piston.backwards)
            {
                part.length += part.animationRange;
                part.animationRange = -part.animationRange;
            }

            part.animationTime = piston.time * 30.0f;
            part.animationPause = piston.pause * 30.0f;
            part.animationPhase = piston.sync * 30.0f;
            part.stiff = piston.stiff;

            part.strength =
                (float) Math.pow(((-1.750184 / (piston.strength + 0.1750153) + 10.00018) / 10.0f), 3.0);


        }
        else if (joint instanceof Bolt)
        {
            Bolt bolt = (Bolt) joint;


            float fVar5 = 4500.0f;
            float fVar1 = (float) Math.log((bolt.tightness * 1000000.0f) / 7.304f + 1.0f);
            float fVar2 = (float) Math.log(3);
            fVar1 = (float) (Math.log((fVar1 / fVar2) * 0.08f * (fVar5 - 1.0f) + 1.0f));
            fVar2 = (float) Math.log(fVar5);


            part.strength = (float) Math.pow((fVar1 / fVar2), 3.0);

            part.length = 0.0f;
        }
        else if (joint instanceof MotorBolt)
        {
            MotorBolt bolt = (MotorBolt) joint;

            part.length = 0.0f;
            part.animationSpeed = (bolt.speed * 4.0f) * 0.0174532925f;
            if (bolt.direction != 1)
                part.animationSpeed = -part.animationSpeed;
            part.strength =
                (float) Math.pow((Math.log(((bolt.tightness / 2.0f) * (15000000.0f - 1.0f) + 1.0f)) / Math.log(15000000.0f)), 3.0);
        }
        else if (joint instanceof WobbleBolt)
        {
            WobbleBolt bolt = (WobbleBolt) joint;
            part.strength =
                (float) Math.pow(((-1.750184 / (bolt.tightness + 0.1750153) + 10.00018) / 10.0f), 3.0);
            part.length = 0.0f;
            part.animationRange = bolt.rotation;
            part.angle = bolt.angle;
            if (bolt.flipperMotion != 0)
                part.animationPattern = 2;
            if (bolt.flipperMotion == 1 || bolt.backwards)
                part.animationRange = -part.animationRange;

            part.animationTime = bolt.time * 30.0f;
            part.animationPause = bolt.pause * 30.0f;
            part.animationPhase = bolt.sync * 30.0f;
        }
        else if (joint instanceof SprungBolt)
        {
            SprungBolt bolt = (SprungBolt) joint;


            float[] values = {
                0.001f,
                0.5f,
                0.005f,
                1.0f,
                0.02f,
                6.5f,
                1.0f,
                10.0f,
            };

            float strength = 0.0f;
            if (bolt.tightness != 0.0)
            {
                int offset = 0;
                for (int i = 0; i < 4; ++i, offset += 2)
                {
                    if (values[offset] <= bolt.tightness && bolt.tightness <= values[offset + 2])
                        break;
                }
                strength = values[offset + 1] +
                            ((values[offset + 3] - values[offset + 1]) * (bolt.tightness - values[offset])) /
                            (values[offset + 2] - values[offset]);
            }


            part.strength = (float) Math.pow(strength, 3.0);
            part.length = 0.0f;
            part.animationRange = bolt.angle;
        }

        context.lookup.put(joint.uid, thing);
        context.things.add(thing);
    }

    public static void addGlue(LoadContext context)
    {
        for (Element element : context.biff.getAll(ElementType.GLUE))
        {
            Glue glue = element.load();
            if (glue.objects.length == 0) continue;

            Thing root = context.lookup.get(glue.objects[0]);
            if (root == null)
            {
                //System.out.printf("[AddGlue] Could not find root object \"%s\" in element list!%n", glue.objects[0]);
                continue;
            }

            PPos parentPosPart = root.getPart(Part.POS);
            Matrix4f parentInvMatrix = parentPosPart.worldPosition.invert(new Matrix4f());
            float parentZ = parentPosPart.worldPosition.get(3, 2);
            boolean parentHasShape = root.hasPart(Part.SHAPE);

            for (int i = 1; i < glue.objects.length; ++i)
            {
                Thing child = context.lookup.get(glue.objects[i]);

                if (child == null)
                {
                    //System.out.printf("[AddGlue]\tCould not find glue candidate \"%s\" in element list!\n", glue.objects[i]);
                    continue;
                }

                // Joints shouldn't have parents or groups
                if (child.hasPart(Part.JOINT)) continue;

                PPos pos = child.getPart(Part.POS);

                // Really dumb way of handling switches, but whatever
                if (parentHasShape && !child.hasPart(Part.SHAPE) && !child.hasPart(Part.BODY) && child.hasPart(Part.RENDER_MESH))
                    pos.worldPosition.set(3, 2, parentZ + root.<PShape>getPart(Part.SHAPE).thickness);

                pos.localPosition = parentInvMatrix.mul(pos.worldPosition, new Matrix4f());

                child.parent = root;
                child.setPart(Part.BODY, null);
            }
        }
    }

    public static void attachSwitchTargets(LoadContext context)
    {
        for (Element element : context.elements)
        {
            if (!element.getType().isSwitch()) continue;
            Switch sw = element.load();

            Thing thing = context.lookup.get(sw.uid);
            if (thing == null) continue;
            PSwitch part = thing.getPart(Part.SWITCH);

            ArrayList<SwitchTarget> targets = new ArrayList<>(sw.targets.length);
            for (int i = 0; i < sw.targets.length; ++i)
            {
                Thing target = context.lookup.get(sw.targets[i]);
                if (target == null)
                {
                    //System.out.println("[AddSwitches] Couldn't find switch target in list!");
                    continue;
                }
                if (target.hasPart(Part.SCRIPT))
                {
                    PScript script = target.getPart(Part.SCRIPT);
                    ScriptInstance instance = script.instance;
                    if (new GUID(18420).equals(instance.script.getGUID()))
                        instance.memberData.put("TriggerThing", thing);
                }
                targets.add(new SwitchTarget(target));
            }
            part.outputs[0].targetList = targets;
            part.outputs[0].activation.activation = 0.0f;
        }
    }

    public static void addSwitch(LoadContext context, Element element)
    {

        PS3Asset asset = null;
        switch (element.getType())
        {
            case STICKER_SWITCH:
                asset = PS3Asset.STICKER_SWITCH;
                break;
            case PROXIMITY_SWITCH:
                asset = PS3Asset.PROXIMITY_SWITCH;
                break;
            case GRAB_SWITCH:
                asset = PS3Asset.GRAB_SWITCH;
                break;
            case TAG_SENSOR:
                asset = PS3Asset.TAG_SENSOR;
                break;
            default:
                return;
        }

        Thing thing = context.loader.getGameAsset(context, asset)[0];
        thing.groupHead = null;
        thing.parent = null;
        Switch sw = element.load();

        Vector3f translation = sw.position.mul(WORLD_SCALE, new Vector3f()).add(WORLD_OFFSET);
        // if (translation.z < -200)
        //     translation.z = -300.0f;
        // else if (translation.z < 00)
        //     translation.z = -100;
        // else
        //     translation.z = 100;

        PPos pos = thing.getPart(Part.POS);
        Matrix4f wpos = pos.worldPosition;
        wpos.setTranslation(translation);

        pos.worldPosition = wpos;
        pos.localPosition = wpos;

        PSwitch part = thing.getPart(Part.SWITCH);
        part.radius = sw.radius * WORLD_SCALE;
        part.inverted = sw.inverted;
        switch (sw.behavior)
        {
            case 0:
                part.behavior = SwitchBehavior.OFF_ON;
                break;
            case 1:
                part.behavior = SwitchBehavior.DIRECTION;
                break;
            case 2:
                part.behavior = SwitchBehavior.ONE_SHOT;
                break;
            case 3:
                part.behavior = SwitchBehavior.SPEED_SCALE;
                break;
        }

        PRenderMesh mesh = thing.getPart(Part.RENDER_MESH);
        mesh.editorColor = 0;
        part.colorIndex = sw.colorIndex - 2;
        part.behaviorOld = part.behavior.getValue();
        part.hideInPlayMode = sw.visibility != 0;
        part.oldActivation = 0.0f;

        switch (element.getType())
        {
            case STICKER_SWITCH:
            {
                StickerSwitch sticker = element.load();
                if (!sticker.sticker.isEmpty())
                {
                    String biff =
                        "stickers" + sticker.sticker.substring(sticker.sticker.lastIndexOf(
                            "/")).replace(".mip", ".plan");
                    GUID guid = context.getBiffDatabase().get(biff).getGUID();
                    part.stickerPlan = new ResourceDescriptor(guid, ResourceType.PLAN);
                }
                break;
            }
            case TAG_SENSOR:
            {
                Color color =
                    Color.getHSBColor(((part.colorIndex % 8) / 9.0f) + (2.0f / 3.0f), 0.75f,
                        1.0f);
                mesh.editorColor
                    =
                    (0xFF << 24) | (color.getRed() << 16) | (color.getGreen() << 8) | (color.getBlue() << 0);
                break;
            }
            default:
                break;
        }

        context.lookup.put(sw.uid, thing);
        context.things.add(thing);
    }

    public static Thing addEmitter(LoadContext context, Emitter emitter)
    {
        Thing thing = context.loader.getGameAsset(context, PS3Asset.EMITTER)[0];

        Vector3f translation = emitter.position.mul(WORLD_SCALE, new Vector3f()).add(WORLD_OFFSET);
        // if (translation.z < -200)
        //     translation.z = -300.0f;
        // else if (translation.z < 00)
        //     translation.z = -100;
        // else
        //     translation.z = 100;

        PPos pos = thing.getPart(Part.POS);
        Matrix4f wpos = pos.worldPosition;
        wpos.setTranslation(translation);
        wpos.rotateX((float) Math.toRadians(90.0f));

        pos.worldPosition = wpos;
        pos.localPosition = wpos;

        PEmitter part = thing.getPart(Part.EMITTER);

        part.modScaleActive = true;
        part.speedScaleStartFrame = 0.0f;
        part.emitScale = 1.0f;
        part.currentEmitted = 0;
        part.parentRelativeRotation = 0.0f;
        part.worldRotation = 0.0f;

        part.linearVel = emitter.linearVelocity * 6.666f * 10.0f;
        part.angVel = emitter.angularVelocity * 5.0f / 30.0f;
        part.frequency = Math.round(emitter.frequency * 30.0f);
        part.lifetime = Math.round(emitter.lifetime * 30.0f);
        part.maxEmitted = emitter.maxEmitted;
        part.maxEmittedAtOnce = emitter.maxEmittedAtOnce;
        part.phase = Math.round(emitter.sync * 30.0f);
        part.hideInPlayMode = (emitter.visibility != 0);
        
        context.lookup.put(emitter.uid, thing);
        context.things.add(thing);

        return thing;
    }
    
    public static void addGroup(LoadContext context, MyObject object)
    {
        Thing group = context.getEmptyThing();
        group.setPart(Part.GROUP, new PGroup());

        ArrayList<Thing> members = new ArrayList<>();
        for (UID uid : object.objects)
        {
            Thing member = context.lookup.get(uid);
            if (member != null)
            {
                members.add(member);
                member.groupHead = group;
            }
        }
        
        // Make sure to add the group itself so it properly gets destroyed.
        members.add(group);

        // If the only thing in the group is the group head,
        // then just means we don't have everything in it supported, discard it.
        if (members.size() == 1) return;

        context.lookup.put(object.uid, group);
        context.things.add(group);
        context.objects.put(object.uid, members.toArray(Thing[]::new));
    }
}
