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

#include "arrow.h"
#include "layout/arrowcoords.h"

namespace omnetpp {
namespace qtenv {

static layout::Rc toRc(const QRectF& r) {
    return layout::Rc(r.left(), r.top(), 0, r.width(), r.height());
}

QLineF arrowcoords(const QRectF& srcRect, const QRectF& destRect,
                   int bundle_i, int bundle_n, char mode,
                   QPointF srcAnch, QPointF destAnch)
{
    layout::Ln ln = layout::arrowcoords(
        toRc(srcRect), toRc(destRect),
        bundle_i, bundle_n, mode,
        srcAnch.x(), srcAnch.y(),
        destAnch.x(), destAnch.y());
    return QLineF(ln.begin.x, ln.begin.y, ln.end.x, ln.end.y);
}

}  // namespace qtenv
}  // namespace omnetpp

