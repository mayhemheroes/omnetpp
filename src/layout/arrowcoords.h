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

// Routing constraint parameters parsed from the display string's "m" tag.
//
// Submodule-to-parent connections:
//   srcDir is a cardinal direction ('n','e','s','w') selecting the parent edge,
//   or '\0' for automatic (nearest edge).
//
// Submodule-to-submodule connections:
//   srcDir and destDir are each 'h' (horizontal = leave/enter via east/west
//   edge) or 'v' (vertical = via north/south edge), or '\0' (unconstrained).
//   Callers should normalize 'e'/'w' -> 'h' and 'n'/'s' -> 'v' before calling.
//
// Manual mode:
//   srcDir='m' enables anchor-based routing where srcAnchX/Y and destAnchX/Y
//   specify anchor points within the rectangles as percentages (0-100).
//
// Auto mode:
//   srcDir='\0' with destDir='\0' is equivalent to 'a' (auto) mode.
struct LAYOUT_API RoutingConstraint {
    char srcDir = '\0';   // 'h','v','n','e','s','w', 'm' (manual), or '\0' (auto)
    char destDir = '\0';  // 'h','v', or '\0' (unconstrained)
    // Manual mode anchor points (only used when srcDir == 'm'):
    double srcAnchX = 50;
    double srcAnchY = 50;
    double destAnchX = 50;
    double destAnchY = 50;
};

/**
 * Computes the polyline (list of points) for a connection arrow between
 * two module bounding rectangles.
 *
 * ROUTING RULES (submodule-to-submodule):
 *
 * Rules are applied in priority order:
 *
 * 1. Overlap rule (strongest). If the two rectangles have meaningful
 *    overlap (>=10px) in either x or y, a straight horizontal or vertical
 *    line is drawn through the overlap zone, regardless of any specified
 *    directions.
 *
 * 2. L-shape rule. If there is no overlap and at least one direction is
 *    specified (and the combination is not the invalid same-axis h,h or
 *    v,v), an L-shaped route with one bend point is produced. The
 *    constrained endpoint exits/enters along the specified axis; the
 *    unconstrained endpoint uses the other axis implied by geometry.
 *
 * 3. Fallback. If no direction is specified, or the combination is invalid
 *    (h,h or v,v), a straight line is drawn between the two modules.
 *
 * SPECIAL CASES:
 *
 * Same-rect: auto-resolve to horizontal or vertical by aspect ratio.
 *
 * Containment (parent<->child):
 *   No bendpoints. Only srcDir determines which edge ('\0' = nearest).
 *   srcDir should be a cardinal direction ('n','e','s','w') in this case.
 *
 * Manual mode (srcDir='m'):
 *   Anchor points at specified percentages of src/dest rects, clipped to edges.
 *
 * BUNDLING:
 *   bundle_coeff = (bundle_i+1) / (bundle_n+1).
 *   Unconstrained: perpendicular offset within junction rects.
 *   Straight H/V (overlap): distribute within overlap zone.
 *   L-shape (disjoint): same bundle_coeff on both exit and entry edges ->
 *     parallel L-shapes with shifted bend points.
 *   Containment: bundle_coeff along the inner rect's edge.
 */
LAYOUT_API std::vector<Pt> arrowcoords(
    const Rc& srcRect, const Rc& destRect,
    int bundle_i = 0, int bundle_n = 1,
    const RoutingConstraint& constraint = RoutingConstraint());

}  // namespace layout
}  // namespace omnetpp

#endif
