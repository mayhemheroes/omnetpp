package org.omnetpp.ned.editor.graph.properties;

import org.eclipse.draw2d.geometry.PrecisionDimension;
import org.eclipse.draw2d.geometry.PrecisionPoint;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.omnetpp.common.properties.NumberPropertyDescriptor;
import org.omnetpp.figures.misc.AnchoredRectangle;
import org.omnetpp.ned.editor.graph.misc.CanvasFigureUtils;
import org.omnetpp.ned.editor.graph.properties.util.AnchorPropertyDescriptor;
import org.omnetpp.ned.editor.graph.properties.util.NedBasePropertySource;
import org.omnetpp.ned.model.ex.PropertyElementEx;

import static org.omnetpp.common.canvas.CanvasFigureConstants.*;

public class AnchoredRectanglePropertySource extends NedBasePropertySource {
    private enum ID {X_ID, Y_ID, WIDTH_ID, HEIGHT_ID, ANCHOR_ID }

    private PropertyDescriptor[] descriptors;

    public AnchoredRectanglePropertySource(PropertyElementEx figure) {
        this(figure, "Anchored rectangle");
    }

    public AnchoredRectanglePropertySource(PropertyElementEx figure, String category) {
        super(figure);

        descriptors = new PropertyDescriptor[] {
                new NumberPropertyDescriptor(ID.X_ID, "x"),
                new NumberPropertyDescriptor(ID.Y_ID, "y"),
                new NumberPropertyDescriptor(ID.WIDTH_ID, "width"),
                new NumberPropertyDescriptor(ID.HEIGHT_ID, "height"),
                new AnchorPropertyDescriptor(ID.ANCHOR_ID)
        };

        for (PropertyDescriptor desc : descriptors) {
            desc.setCategory(category);
        }
    }

    @Override
    public Object getPropertyValue(Object id) {
        AnchoredRectangle rect = CanvasFigureUtils.parseAnchoredRectangle((PropertyElementEx)getModel(), PKEY_BOUNDS, PKEY_POS, PKEY_SIZE, PKEY_ANCHOR);

        switch ((ID)id) {
        case ANCHOR_ID:
            return rect.getAnchor().toString();
        case HEIGHT_ID:
            return rect.getSize().preciseHeight();
        case WIDTH_ID:
            return rect.getSize().preciseWidth();
        case X_ID:
            return rect.getLocation().preciseX();
        case Y_ID:
            return rect.getLocation().preciseY();
        default:
            return null;
        }
    }

    @Override
    public void setPropertyValue(Object id, Object value) {
        AnchoredRectangle rect = CanvasFigureUtils.parseAnchoredRectangle((PropertyElementEx)getModel(), PKEY_BOUNDS, PKEY_POS, PKEY_SIZE, PKEY_ANCHOR);

        PrecisionDimension size = rect.getSize();
        PrecisionPoint location = rect.getLocation();

        switch ((ID)id) {
        case ANCHOR_ID:
            rect.setAnchor(AnchorPropertyDescriptor.getAnchor((String)value));
            break;
        case HEIGHT_ID:
            size.setPreciseHeight((Double)value);
            break;
        case WIDTH_ID:
            size.setPreciseWidth((Double)value);
            break;
        case X_ID:
            location.setPreciseX((Double)value);
            break;
        case Y_ID:
            location.setPreciseY((Double)value);
            break;
        }

        rect.setLocation(location);
        rect.setSize(size);

        CanvasFigureUtils.setAnchoredRectangle((PropertyElementEx)getModel(), PKEY_BOUNDS, PKEY_POS, PKEY_SIZE, PKEY_ANCHOR, rect);
    }

    @Override
    public IPropertyDescriptor[] getPropertyDescriptors() {
        return descriptors;
    }

}
