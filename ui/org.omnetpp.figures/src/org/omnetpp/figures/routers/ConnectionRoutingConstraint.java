/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.figures.routers;

/**
 * Holds connection routing parameters extracted from the display string's "m" tag.
 * Submod-to-parent: m=&lt;dir&gt; where dir is a cardinal direction (n/e/s/w) or '\0' (nearest).
 * Submod-to-submod: m=&lt;srcDir&gt;,&lt;destDir&gt; where each is h, v, or cardinal synonym
 * (e/w=h, n/s=v), or '\0' (unconstrained).
 * srcDir='m' for manual mode with anchor percentages.
 * Used as a routing constraint on Connection figures, read by CompoundModuleConnectionRouter.
 */
public class ConnectionRoutingConstraint {
    /** Source direction: 'h','v' (or cardinal synonyms n/e/s/w), 'm' (manual), or '\0' (auto) */
    public char srcDir = '\0';
    /** Destination direction: 'h','v' (or cardinal synonyms n/e/s/w), or '\0' (unconstrained) */
    public char destDir = '\0';

    /** Source anchor x percentage (0-100), used in manual mode (srcDir == 'm') */
    public double srcAnchX = 50;
    /** Source anchor y percentage (0-100), used in manual mode (srcDir == 'm') */
    public double srcAnchY = 50;
    /** Destination anchor x percentage (0-100), used in manual mode (srcDir == 'm') */
    public double destAnchX = 50;
    /** Destination anchor y percentage (0-100), used in manual mode (srcDir == 'm') */
    public double destAnchY = 50;

    /** Index of this connection within its bundle (0-based) */
    public int bundleIndex = 0;
    /** Total number of connections in this bundle */
    public int bundleSize = 1;
}
