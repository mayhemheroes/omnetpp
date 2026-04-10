//==========================================================================
//  ARROWCOORDS.H - part of
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2017 Andras Varga
  Copyright (C) 2006-2017 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __OMNETPP_LAYOUT_ARROWCOORDS_H
#define __OMNETPP_LAYOUT_ARROWCOORDS_H

#include <vector>
#include "geometry.h"
#include "layoutdefs.h"

namespace omnetpp {
namespace layout {

// Computes the polyline (list of points) for a connection arrow between
// two rectangles representing the bounding boxes of modules.
//
// The rectangles represent the bounding boxes of the source and destination modules.
// The srcAnchX/Y and destAnchX/Y parameters are used in 'm' mode to specify the anchor points
// within the rectangles as percentages (0-100) of width and height.
//
// bundle_i is the index of this connection within the bundle (0-based),
// and bundle_n is the total number of connections in the bundle.
// mode must be one of "amnews":
//   'a' = automatic, 'm' = manual, 'n' = north, 'e' = east, 'w' = west, 's' = south
LAYOUT_API std::vector<Pt> arrowcoords(
    const Rc& srcRect, const Rc& destRect,
    int bundle_i = 0, int bundle_n = 1,
    char mode = 'a',
    double srcAnchX = 50, double srcAnchY = 50,
    double destAnchX = 50, double destAnchY = 50);

}  // namespace layout
}  // namespace omnetpp

#endif
