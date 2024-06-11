package cambridge.enums;

import com.fasterxml.jackson.core.Version;

import cambridge.io.Serializable;
import cambridge.structures.*;
import cambridge.structures.data.CatalogData;
import cambridge.structures.joints.*;
import cambridge.structures.joints.bolts.Bolt;
import cambridge.structures.joints.bolts.MotorBolt;
import cambridge.structures.joints.bolts.SprungBolt;
import cambridge.structures.joints.bolts.WobbleBolt;
import cambridge.structures.switches.*;

public enum ElementType
{
    INVALID(null),

    OBJECT("MYOB", MyObject.class),

    TEXT("TEXT"),

    JETPACK("JTPK", Jetpack.class),

    LANDSCAPE("LAND", Landscape.class),
    GLUE("GLUE", Glue.class),
    VERTICES("VERT"),

    SCORE_BUBBLE("SBUB", ScoreBubble.class),
    PRIZE_BUBBLE("PBUB", PrizeBubble.class),

    BOLT("BOLT", Bolt.class),
    WOBBLE_BOLT("WBLT", WobbleBolt.class),
    SPRUNG_BOLT("SBLT", SprungBolt.class),
    MOTOR_BOLT("MBLT", MotorBolt.class),

    STRING("STRN", Str.class),
    SPRING("SPRN", Spring.class),
    WINCH("WNCH", Winch.class),
    PISTON("PSTN", Piston.class),
    ROD("ROD ", Rod.class),
    ELASTIC("ELAS", Elastic.class),

    ENTRANCE("ENTR", GameObject.class),
    CHECKPOINT("CHKP", GameObject.class),
    SCOREBOARD("SCBD", Scoreboard.class),
    BUTTON("BUTN", Button.class),

    MAGNETIC_KEY("MAGK", MagneticKey.class),

    MAGIC_MOUTH("MAGM", MagicMouth.class),

    THRUSTER("RCKT"),

    MATERIAL_MANAGER("MMGR", MaterialManager.class),
    MATERIAL_DATA("MDAT"),

    ITEM_METADATA("MET0", ItemMetadata.class),

    STICKER_SWITCH("STKS", StickerSwitch.class),
    PROXIMITY_SWITCH("SENS", ProximitySwitch.class),
    GRAB_SWITCH("GRBS", GrabSensor.class),
    TAG_SENSOR("KEYS", TagSensor.class),

    CAMERA_ZONE("GCAM", GameCamera.class),

    EMITTER("EMIT", Emitter.class),
    MESH("MERO", ModelObject.class),

    MUSIC("MUSC", Music.class),

    BACKGROUND_DATA("BDAT", BackgroundData.class),
    BACKGROUND_COLOR("BCLR", BackgroundColor.class),
    BACKGROUND_LIGHTING("BALS", BackgroundLighting.class),

    WORLD("WRLD", World.class),

    DATA("DATA", CatalogData.class),
    SLOT("SLOT", Slots.class),

    LEVEL_EXIT("LVEX", LevelExit.class),

    V001("V001", VersionData.class),
    V010("V010", VersionData.class),
    V013("V013", VersionData.class);

    private final String id;
    private final Class<? extends Serializable> clazz;

    ElementType(String id)
    {
        this.id = id;
        this.clazz = null;
    }

    ElementType(String id, Class<? extends Serializable> clazz)
    {
        this.id = id;
        this.clazz = clazz;
    }

    public String getId()
    {
        return this.id;
    }

    public Class<? extends Serializable> getSerializable()
    {
        return this.clazz;
    }

    public static ElementType get(String id)
    {
        for (ElementType type : ElementType.values())
        {
            if (id.equals(type.id))
                return type;
        }
        return ElementType.INVALID;
    }

    public boolean isSwitch()
    {
        return 
            this == STICKER_SWITCH ||
            this == PROXIMITY_SWITCH ||
            this == GRAB_SWITCH ||
            this == TAG_SENSOR;
    }
}
