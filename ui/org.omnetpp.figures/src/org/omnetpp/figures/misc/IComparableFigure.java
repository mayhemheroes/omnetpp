package org.omnetpp.figures.misc;

import org.omnetpp.figures.canvas.AbstractCanvasFigure;

/**
 * A simple interface mainly to unify the handling of
 * {@link AbstractCanvasFigure}s when they have to be sorted by their z-indices,
 * and ordinal positions. Implementers should notify their parents (if
 * those are {@link IFigureSortingParent} instances) of the change of their
 * z-index and/or ordinal.
 *
 * @author attila
 */
public interface IComparableFigure {
    public void setZIndex(double zIndex);
    public double getZIndex();

    public void setOrdinal(int ordinal);
    public int getOrdinal();
}
