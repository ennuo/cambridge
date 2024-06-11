package cambridge.enums;

import java.util.HashMap;

public class LanguageID
{
    public static final String[] NAMES = new String[]
        {
            "danish",
            "german",
            "english",
            "spanish",
            "finnish",
            "french",
            "italian",
            "japanese",
            "korean",
            "dutch",
            "norwegian",
            "polish",
            "portuguese",
            "russian",
            "swedish",
            "american",
            "chinese"
        };

    public static final HashMap<String, Integer> CODE_TO_ID = new HashMap<>()
    {{
        put("da", LanguageID.DANISH);
        put("de", LanguageID.GERMAN);
        put("en", LanguageID.ENGLISH);
        put("es", LanguageID.SPANISH);
        put("fi", LanguageID.FINNISH);
        put("fr", LanguageID.FRENCH);
        put("it", LanguageID.ITALIAN);
        put("jp", LanguageID.JAPANESE);
        put("ko", LanguageID.KOREAN);
        put("nl", LanguageID.DUTCH);
        put("no", LanguageID.NORWEGIAN);
        put("pl", LanguageID.POLISH);
        put("pt", LanguageID.PORTUGUESE);
        put("ru", LanguageID.RUSSIAN);
        put("sv", LanguageID.SWEDISH);
        put("us", LanguageID.AMERICAN);
        put("zh", LanguageID.CHINESE);
    }};

    public static final int DANISH = 0;
    public static final int GERMAN = 1;
    public static final int ENGLISH = 2;
    public static final int SPANISH = 3;
    public static final int FINNISH = 4;
    public static final int FRENCH = 5;
    public static final int ITALIAN = 6;
    public static final int JAPANESE = 7;
    public static final int KOREAN = 8;
    public static final int DUTCH = 9;
    public static final int NORWEGIAN = 10;
    public static final int POLISH = 11;
    public static final int PORTUGUESE = 12;
    public static final int RUSSIAN = 13;
    public static final int SWEDISH = 14;
    public static final int AMERICAN = 15;
    public static final int CHINESE = 16;

    public static final int MAX = 17;
}
