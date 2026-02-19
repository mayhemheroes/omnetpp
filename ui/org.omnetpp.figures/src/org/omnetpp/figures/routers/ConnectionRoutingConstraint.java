/*--------------------------------------------------------------*
  Copyright (C) 2006-2015 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  'License' for details on this and other legal matters.
*--------------------------------------------------------------*/

package org.omnetpp.figures.routers;

/**
 * Holds connection routing parameters extracted from the display string's "m" tag.
 * Used as a routing constraint on Connection figures, read by CompoundModuleConnectionRouter.
 */
public class ConnectionRoutingConstraint {
    /** Routing mode: 'a'=auto, 'm'=manual, 'n'=north, 'e'=east, 'w'=west, 's'=south */
    public char mode = 'a';

    /** Source anchor x percentage (0-100), used in 'm' mode */
    public double srcAnchX = 50;
    /** Source anchor y percentage (0-100), used in 'm' mode */
    public double srcAnchY = 50;
    /** Destination anchor x percentage (0-100), used in 'm' mode */
    public double destAnchX = 50;
    /** Destination anchor y percentage (0-100), used in 'm' mode */
    public double destAnchY = 50;

    /** Index of this connection within its bundle (0-based) */
    public int bundleIndex = 0;
    /** Total number of connections in this bundle */
    public int bundleSize = 1;
}
