package a7.tweakception.utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Matchers
{
    public static final Matcher minecraftUsername = Pattern.compile(
        "[A-Za-z0-9_]{1,16}"
    ).matcher("");
}
