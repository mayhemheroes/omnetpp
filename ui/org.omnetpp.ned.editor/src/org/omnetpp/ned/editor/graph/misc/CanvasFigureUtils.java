package org.omnetpp.ned.editor.graph.misc;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.draw2d.geometry.Point;
import org.eclipse.draw2d.geometry.PrecisionDimension;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.draw2d.geometry.Rectangle;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.FontData;
import org.omnetpp.common.util.Converter;
import org.omnetpp.common.util.GeomUtils;
import org.omnetpp.figures.misc.AnchoredRectangle;
import org.omnetpp.figures.misc.Transform;
import org.omnetpp.figures.misc.AnchoredRectangle.Anchor;
import org.omnetpp.figures.misc.TransformDescription;
import org.omnetpp.ned.core.NedCanvasFigureValidator;
import org.omnetpp.ned.model.INedElement;
import org.omnetpp.ned.model.ex.CompoundModuleElementEx;
import org.omnetpp.ned.model.ex.PropertyElementEx;
import org.omnetpp.ned.model.pojo.PropertyKeyElement;

import static org.omnetpp.common.canvas.CanvasFigureConstants.*;

/**
 * HACK uses 1<<2 in the font style bit field as if it was SWT.UNDERLINE
 */
public final class CanvasFigureUtils {

    public static final int UNDERLINE = 1 << 2;

    // US locale to force the decimal separator to a dot
    private static DecimalFormat doubleFormatter = (DecimalFormat)DecimalFormat.getInstance(Locale.US);

    static {
        doubleFormatter.setMaximumFractionDigits(2);
        doubleFormatter.setMinimumFractionDigits(0);
        doubleFormatter.setDecimalSeparatorAlwaysShown(false);
        doubleFormatter.setGroupingUsed(false);
    }

    public static String getParentIndex(String index) {
        return index.contains(".") ? index.substring(0, index.lastIndexOf('.')) : "";
    }

    /**
     * Returns the ordinal position of the given figure index in the figures map.
     * Used for z-ordering instead of source line numbers, because inherited
     * figures have line numbers from different files that are not comparable.
     * The merged LinkedHashMap preserves the correct order (root ancestor first,
     * derived type last).
     */
    public static int getOrdinal(Map<String, PropertyElementEx> figures, String index) {
        int ordinal = 0;
        for (String key : figures.keySet()) {
            if (key.equals(index))
                return ordinal;
            ordinal++;
        }
        return 0;
    }

    public static PropertyElementEx getClosestAncestor(PropertyElementEx property) {
        if (property == null) {
            return null;
        }

        CompoundModuleElementEx module = (CompoundModuleElementEx)property.getParent().getParent();

        // All the figure properties of the CompoundModule which contains this figure.
        Map<String, PropertyElementEx> figures = module.getProperties().get("figure");

        if (figures == null) {
            return null;
        }

        String ancestorIndex = getParentIndex(property.getIndex());

        while (!ancestorIndex.isEmpty() && !figures.containsKey(ancestorIndex)) {
            ancestorIndex = getParentIndex(ancestorIndex);
        }

        return (ancestorIndex.isEmpty() || ancestorIndex.equals("submodules")) ? null : figures.get(ancestorIndex);
    }

    public static void removeInvalidParameters(PropertyElementEx property) {
        List<String> validParameters = NedCanvasFigureValidator.getValidPropertyKeysForType(property.getValue(PKEY_TYPE));
        for (Iterator<INedElement> iterator = property.iterator(); iterator.hasNext(); ) {
            INedElement child = iterator.next();

            if (child instanceof PropertyKeyElement) {
                PropertyKeyElement keyElement = (PropertyKeyElement)child;

                String key = keyElement.getName();
                if (!validParameters.contains(key) && !key.startsWith("x-")) {
                    iterator.remove();
                }
            }
        }
    }


    // Some common defaults.

    public static PrecisionPoint getDefaultPoint() {
        return new PrecisionPoint(100, 100);
    }

    public static PrecisionDimension getDefaultDimension() {
        return new PrecisionDimension(100, 100);
    }

    public static Anchor getDefaultAnchor() {
        return Anchor.ANCHOR_NONE;
    }

    public static List<Point> getDefaultPointList() {
        List<Point> points = new ArrayList<Point>();

        points.add(new PrecisionPoint(10, 10));
        points.add(new PrecisionPoint(60, 110));
        points.add(new PrecisionPoint(110, 10));

        return points;
    }


    /**
     * Rounds the coordinates of the given point based on the specified zoom level.
     * The rounding ensures coordinates have no more digits than makes sense
     * considering the granularity allowed by the zoom level.
     *
     * E.g. for a zoom of 1x..9x, round to integers; for a zoom of 10x..99x, round to 1 decimal place, etc.
     */
    public static void roundForZoom(PrecisionPoint point, double scale) {
        point.setPreciseX(GeomUtils.roundForZoom(point.preciseX(), scale));
        point.setPreciseY(GeomUtils.roundForZoom(point.preciseY(), scale));
    }

    /**
     * Rounds the dimensions based on the specified zoom level.
     * @see #roundForZoom(PrecisionPoint, double)
     */
    public static void roundForZoom(PrecisionDimension dim, double scale) {
        dim.setPreciseWidth(GeomUtils.roundForZoom(dim.preciseWidth(), scale));
        dim.setPreciseHeight(GeomUtils.roundForZoom(dim.preciseHeight(), scale));
    }

    public static PrecisionPoint parsePoint(PropertyElementEx property, String key) {
        if (property == null) {
            return null;
        }

        List<String> values = property.getValueAsList(key);
        if (values == null)
            return null;

        try {
            return (values.size() >= 2) ? new PrecisionPoint(Double.parseDouble(values.get(0)), Double.parseDouble(values.get(1))) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void setPoint(PropertyElementEx property, String key, Point point) {
        if (property == null) {
            return;
        }

        property.setValues(key, Arrays.asList(new String[]
                { doubleFormatter.format(point.preciseX()),
                  doubleFormatter.format(point.preciseY()) } ));
    }

    public static List<Point> parsePointList(PropertyElementEx property, String key) {
        List<Point> points = new ArrayList<Point>();

        if (property == null) {
            return null;
        }

        List<String> values = property.getValueAsList(key);
        if (values == null)
            return points;

        try {
            for (int i = 0; i < (values.size() - 1); i += 2) {
                points.add(new PrecisionPoint(Double.parseDouble(values.get(i)), Double.parseDouble(values.get(i + 1))));
            }
        } catch (NumberFormatException e) {
            // Nothing.
        }

        return points;
    }

    public static void setPointList(PropertyElementEx property, String key, List<Point> pointList) {
        if (property == null) {
            return;
        }

        List<String> values = new ArrayList<String>();

        for (Point point : pointList) {
            values.add(doubleFormatter.format(point.preciseX()));
            values.add(doubleFormatter.format(point.preciseY()));
        }

        property.setValues(key, values);
    }

    public static PrecisionRectangle parseRectangle(PropertyElementEx property, String key) {
        if (property == null) {
            return null;
        }

        List<String> values = property.getValueAsList(key);
        if (values == null)
            return null;

        try {
            return (values.size() >= 4)
                    ? new PrecisionRectangle(Double.parseDouble(values.get(0)), Double.parseDouble(values.get(1)),
                                             Double.parseDouble(values.get(2)), Double.parseDouble(values.get(3)))
                    : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void setRectangle(PropertyElementEx property, String key, Rectangle rectangle) {
        if (property == null) {
            return;
        }

        property.setValues(key, Arrays.asList(new String[]
                { doubleFormatter.format(rectangle.preciseX()),
                  doubleFormatter.format(rectangle.preciseY()),
                  doubleFormatter.format(rectangle.preciseWidth()),
                  doubleFormatter.format(rectangle.preciseHeight())} ));
    }

    public static PrecisionDimension parseDimension(PropertyElementEx property, String key) {
        if (property == null) {
            return null;
        }

        List<String> values = property.getValueAsList(key);
        if (values == null)
            return null;

        try {
            return (values.size() >= 2) ? new PrecisionDimension(Double.parseDouble(values.get(0)), Double.parseDouble(values.get(1))) : null;
        } catch (NumberFormatException e) {
            return null;
        }
    }

    public static void setDimension(PropertyElementEx property, String key, Dimension dimension) {
        if (property == null) {
            return;
        }

        property.setValues(key, Arrays.asList(new String[]
                { doubleFormatter.format(dimension.preciseWidth()),
                  doubleFormatter.format(dimension.preciseHeight()) } ));
    }

    public static Anchor parseAnchor(PropertyElementEx property, String key) {
        if (property == null) {
            return null;
        }

        String anchor = property.getValue(key);
        if (anchor == null) {
            return null;
        } else {
            if (anchor.equals(PVAL_ANCHOR_N)) {
                return Anchor.ANCHOR_N;
            } else if (anchor.equals(PVAL_ANCHOR_W)) {
                return Anchor.ANCHOR_W;
            } else if (anchor.equals(PVAL_ANCHOR_S)) {
                return Anchor.ANCHOR_S;
            } else if (anchor.equals(PVAL_ANCHOR_E)) {
                return Anchor.ANCHOR_E;
            } else if (anchor.equals(PVAL_ANCHOR_NW)) {
                return Anchor.ANCHOR_NW;
            } else if (anchor.equals(PVAL_ANCHOR_NE)) {
                return Anchor.ANCHOR_NE;
            } else if (anchor.equals(PVAL_ANCHOR_SW)) {
                return Anchor.ANCHOR_SW;
            } else if (anchor.equals(PVAL_ANCHOR_SE)) {
                return Anchor.ANCHOR_SE;
            } else if (anchor.equals(PVAL_ANCHOR_C) || anchor.equals(PVAL_ANCHOR_CENTER)) {
                return Anchor.ANCHOR_C;
            } else {
                return null;
            }
        }
    }

    public static void setAnchor(PropertyElementEx property, String key, Anchor anchor) {
        if (property == null) {
            return;
        }

        String value;

        switch (anchor) {
        case ANCHOR_C:      value = "c";  break;
        case ANCHOR_E:      value = "e";  break;
        case ANCHOR_N:      value = "n";  break;
        case ANCHOR_NE:     value = "ne"; break;
        case ANCHOR_NW:     value = "nw"; break;
        case ANCHOR_S:      value = "s";  break;
        case ANCHOR_SE:     value = "se"; break;
        case ANCHOR_SW:     value = "sw"; break;
        case ANCHOR_W:      value = "w";  break;
        default: /* NONE */ value = null; break;
        }

        if (value == null)
            property.removeKey(key);
        else
            property.setValue(key, value);
    }


    public static AnchoredRectangle parseAnchoredRectangle(PropertyElementEx property, String boundsKey, String coordsKey, String sizeKey, String anchorKey) {
        Rectangle bounds = parseRectangle(property, boundsKey);

        if (bounds != null) {
            return new AnchoredRectangle(bounds);
        } else {
            Point location = parsePoint(property, coordsKey);
            Dimension size = parseDimension(property, sizeKey);
            Anchor anchor = parseAnchor(property, anchorKey);

            if (location == null)
                location = getDefaultPoint();

            if (size == null)
                size = getDefaultDimension();

            if (anchor == null)
                anchor = getDefaultAnchor();

            return new AnchoredRectangle(location.preciseX(), location.preciseY(), size.preciseWidth(), size.preciseHeight(), anchor);
        }
    }

    public static void setAnchoredRectangle(PropertyElementEx property, String boundsKey, String coordsKey, String sizeKey, String anchorKey, AnchoredRectangle rect) {
        if (property == null) {
            return;
        }

        List<String> boundsValues = property.getValueAsList(boundsKey);
        if (rect.getAnchor() == AnchoredRectangle.Anchor.ANCHOR_NONE
                && boundsValues != null && !boundsValues.isEmpty()) {
                // will keep the bounds key if the rect was specified by that, and no anchor is given
            setRectangle(property, boundsKey, rect.getRectangle());

            property.removeKey(coordsKey);
            property.removeKey(sizeKey);
            property.removeKey(anchorKey);
        } else {
            setPoint(property, coordsKey, rect.getLocation());
            setDimension(property, sizeKey, rect.getSize());
            setAnchor(property, anchorKey, rect.getAnchor());

            property.removeKey(boundsKey);
        }
    }

    public static List<TransformDescription> parseTransforms(PropertyElementEx property, String key) {
        if (property == null) {
            return null;
        }

        List<TransformDescription> transforms = new ArrayList<TransformDescription>();
        List<String> values = property.getValueAsList(key);

        if (values != null) {
            for (String value : values) {
                transforms.add(TransformDescription.parse(value));
            }
        }

        return transforms;
    }

    public static void setTransforms(PropertyElementEx property, String key, List<TransformDescription> transforms) {
        if (property == null) {
            return;
        }

        List<String> values = new ArrayList<String>();

        for (TransformDescription transform : transforms) {
            if (transform != null) {
                values.add(transform.toString());
            }
        }

        property.setValues(key, values);
    }

    public static Transform parseTransform(PropertyElementEx property, String key) {
        if (property == null) {
            return null;
        }

        Transform result = new Transform();

        List<String> values = property.getValueAsList(key);
        if (values == null)
            return result;
        Collections.reverse(values); // to match the order of transformations to tkenv

        for (String transform : values) {
            TransformDescription tr = TransformDescription.parse(transform);
            if (tr != null) {
                result.multiply(tr.getTransform());
            }
        }

        return result;
    }

    /**
     * HACK uses 1<<2 in the style bitfield as if it was SWT.UNDERLINE
     */
    public static FontData parseFont(PropertyElementEx property, String key) {
        String name = "Arial";
        int size = 10;
        int style = SWT.NORMAL;

        FontData fontData = new FontData(name, size, style);

        if (property == null) {
            return fontData;
        }

        List<String> font = property.getValueAsList(PKEY_FONT);


        if (font != null) {
            if ((font.size() >= 1) && (!font.get(0).isEmpty())) {
                fontData.setName(font.get(0));
            }

            if ((font.size()) >= 2 && (!font.get(1).isEmpty())) {
                fontData.setHeight(Converter.stringToInteger(font.get(1)));
            }

            if ((font.size()) >= 3 && (!font.get(2).isEmpty())) {
                setFontDataFlag(fontData, SWT.BOLD, font.get(2).contains("bold"));
                setFontDataFlag(fontData, SWT.ITALIC, font.get(2).contains("italic"));
                setFontDataFlag(fontData, UNDERLINE, font.get(2).contains("underline"));
            }
        }

        return fontData;
    }

    public static void setFont(PropertyElementEx property, String key, FontData fontData) {
        if (property == null) {
            return;
        }

        String styleString = "";

        if (fontData.getStyle() == SWT.NORMAL) {
            styleString = "normal";
        } else {
            if (getFontDataFlag(fontData, SWT.BOLD)) {
                styleString += "bold ";
            }

            if (getFontDataFlag(fontData, SWT.ITALIC)) {
                styleString += "italic ";
            }

            if (getFontDataFlag(fontData, UNDERLINE)) {
                styleString += "underline ";
            }
        }

        property.setValues(key, Arrays.asList(new String[] {fontData.getName(),
                Converter.integerToString(fontData.getHeight()), styleString.trim()}));
    }

    public static boolean getFontDataFlag(FontData fontData, int flag) {
        return (fontData.getStyle() & flag) != 0;
    }

    public static void setFontDataFlag(FontData fontData, int flag, boolean value) {
        fontData.setStyle(value ? (fontData.getStyle() | flag) : (fontData.getStyle() & (~flag)));
    }
}
