package org.omnetpp.ned.editor.graph.properties.util;

import org.omnetpp.common.properties.EnumComboboxPropertyDescriptor;
import org.omnetpp.common.util.EnumSpec;
import org.omnetpp.figures.misc.AnchoredRectangle.Anchor;

// simply to deduplicate the enumspec
public class AnchorPropertyDescriptor extends EnumComboboxPropertyDescriptor {
    protected final static EnumSpec anchorSpec = new EnumSpec(
            "center=^c.*,c; north=^n.*,n; west=^w.*,w; south=^s.*,s; east=^e.*,e;"
                    + "southeast=(^southe.*)|(^se$),se; southwest=(^southw.*)|(^sw$),sw;"
                    + "northeast=(^northe.*)|(^ne$),ne; northwest=(^northw.*)|(^nw$),nw;");

    public AnchorPropertyDescriptor(Object id) {
        this(id, "anchor");
    }

    public AnchorPropertyDescriptor(Object id, String displayName) {
        super(id, displayName, anchorSpec);
    }

    public static String getName(Anchor anchor) {
        return anchorSpec.getNameFor(anchor.toString());
    }

    public static Anchor getAnchor(String name) {
        return (name == null || name.equals(""))
                ? Anchor.ANCHOR_NONE
                : Anchor.valueOf("ANCHOR_" + anchorSpec.getShorthandFor(name).toUpperCase());
    }
}
