//==========================================================================
//  ARROW.CC - part of
//
//                     OMNeT++/OMNEST
//
//  Thin wrapper that delegates to layout::arrowcoords(), converting
//  between Qt types and layout geometry types.
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2017 Andras Varga
  Copyright (C) 2006-2017 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include <vector>
#include "arrow.h"
#include "layout/arrowcoords.h"

namespace omnetpp {
namespace qtenv {

static layout::Rc toRc(const QRectF& r) {
    return layout::Rc(r.left(), r.top(), 0, r.width(), r.height());
}

QPolygonF arrowcoords(const QRectF& srcRect, const QRectF& destRect,
                   int bundle_i, int bundle_n,
                   const layout::RoutingConstraint& constraint)
{
    std::vector<layout::Pt> pts = layout::arrowcoords(
        toRc(srcRect), toRc(destRect),
        bundle_i, bundle_n, constraint);
    QPolygonF poly;
    for (const auto& pt : pts)
        poly << QPointF(pt.x, pt.y);
    return poly;
}

}  // namespace qtenv
}  // namespace omnetpp

