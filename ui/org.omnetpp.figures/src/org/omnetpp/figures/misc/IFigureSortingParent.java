package org.omnetpp.figures.misc;

/**
 * This interface provides a way to be notified when an {@link IComparableFigure}
 * instance child's z-index and/or line number has changed.
 *
 * @author attila
 */
public interface IFigureSortingParent {
    public void childChanged(IComparableFigure child);
}
