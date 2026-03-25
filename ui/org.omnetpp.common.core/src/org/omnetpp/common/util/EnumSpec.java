package org.omnetpp.common.util;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.commons.lang3.ArrayUtils;
import org.eclipse.core.runtime.Assert;

/**
 * Utility class for handling enumerated display string values, like "dashed/dotted/solid"
 * or "left/right/top". A value needs to be recognized in the display string ("das" ==> "dashed"),
 * combo box selection has to be offered (with options "dashed", "dotted" and "solid"),
 * and the value has to be written back into the display string in a short form ("dashed" as "da").
 * This class facilitates the above tasks.
 *
 * @author andras
 */
public class EnumSpec {
    private static class Item {
        String name; // full name ("dashed")
        String shorthandRegex;  // for recognizing the value ("da.*")
        String shorthand; // for representing the value in the display string ("da")
    }
    private Map<String,EnumSpec.Item> specs = new LinkedHashMap<String, EnumSpec.Item>();
    private EnumSpec.Item[] specsReversed;

    /**
     * Format of the specification string: "name=shorthandRegex,shorthand;...".
     * Spaces are allowed. The order of items is significant, because shorthand regexes
     * will be matched in REVERSE order (last-to-first).
     *
     * Example: "solid=s.*,s; dotted=d.*,d; dashed=da.*,da". Note that "d" will be
     * recognized as "dotted", because of reverse-order matching.
     */
    public EnumSpec(String specString) {
        for (String specText : specString.split(";")) {
            Assert.isTrue(specText.matches("^[^=,]+=[^=,]+,[^=,]*$"), "enum spec is in wrong format");
            EnumSpec.Item spec = new Item();
            spec.name = StringUtils.substringBefore(specText, "=").trim();
            spec.shorthandRegex = StringUtils.substringBetween(specText, "=", ",").trim();
            spec.shorthand = StringUtils.substringAfter(specText, ",").trim();
            specs.put(spec.name, spec);
        }
        specsReversed = specs.values().toArray(new EnumSpec.Item[]{});
        ArrayUtils.reverse(specsReversed);

        // sanity checks
        for (EnumSpec.Item spec : specs.values()) {
            Assert.isTrue(spec.name.equals(getNameFor(spec.name)), "enum name must map to itself");
            Assert.isTrue(spec.name.equals(getNameFor(spec.shorthand)), "enum shorthand must map to itself");
        }
    }

    /**
     * Returns an array of all names ("dotted", "dashed", etc).
     */
    public String[] getNames() {
        return specs.keySet().toArray(new String[]{});
    }

    /**
     * Returns the list of all shorthands (standard abbreviations)
     */
    public String[] getShorthands() {
        ArrayList<String> result = new ArrayList<String>();
        for (EnumSpec.Item spec : specs.values())
            result.add(spec.shorthand);
        return result.toArray(new String[]{});
    }

    /**
     * Return the name whose shorthandRegex matches the given string,
     * or null if none matches. Note: matching is done in reverse order.
     */
    public String getNameFor(String text) {
        for (EnumSpec.Item spec : specsReversed)
            if (text.matches(spec.shorthandRegex))
                return spec.name;
        return null;
    }

    /**
     * Return the shorthand (standard abbreviation) whose shorthandRegex
     * matches the given string, or null if none matches. Note: matching
     * is done in reverse order.
     */
    public String getShorthandFor(String text) {
        for (EnumSpec.Item spec : specsReversed)
            if (text.matches(spec.shorthandRegex))
                return spec.shorthand;
        return null;
    }

    /**
     * Returns the shorthand (standard abbreviation) for the given name,
     * e.g. returns "da" for "dashed". It is an error if the name does not exist.
     */
    public String getShorthandForName(String name) {
        Assert.isTrue(specs.containsKey(name), "invalid enum value");
        return specs.get(name).shorthand;
    }

}