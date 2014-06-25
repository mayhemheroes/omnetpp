package org.omnetpp.figures.canvas;

import org.eclipse.draw2d.geometry.PrecisionRectangle;
import org.eclipse.swt.graphics.Path;
import org.eclipse.swt.widgets.Display;
import org.omnetpp.figures.misc.AnchoredRectangle;

public class CanvasOvalFigure extends AbstractCanvasShape {
	private AnchoredRectangle definingRectangle;

	public CanvasOvalFigure() {
	    definingRectangle = new AnchoredRectangle();
		updatePath();
	}

	public AnchoredRectangle getDefiningRectangle() {
        return definingRectangle;
    }

    public void setDefiningRectangle(AnchoredRectangle rectangle) {
        if ((rectangle != null) && (!definingRectangle.equals(rectangle))) {
            erase();
            definingRectangle = rectangle;
            updatePath();
            repaint();
        }
    }

	private void updatePath() {
	    PrecisionRectangle boundingRectangle = definingRectangle.getRectangle();
		path = new Path(Display.getDefault());
		path.addArc((float)boundingRectangle.preciseX(), (float)boundingRectangle.preciseY(),
				(float)boundingRectangle.preciseWidth(), (float)boundingRectangle.preciseHeight(), 0, 360);
		path.close();
	}
}
