//==========================================================================
//  ARROW.H - part of
//
//                     OMNeT++/OMNEST
//
//  Contents:
//   connection arrow positioning in module drawing
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2017 Andras Varga
  Copyright (C) 2006-2017 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __OMNETPP_QTENV_ARROW_H
#define __OMNETPP_QTENV_ARROW_H

#include <QtCore/QPointF>
#include <QtCore/QRectF>
#include <QtCore/QLineF>
#include <QtGui/QPolygonF>
#include "qtenvdefs.h"

namespace omnetpp {
namespace qtenv {

// Calculates the coordinates of a connection polyline between two rectangles.
// The rectangles represent the bounding boxes of the source and destination modules.
// The srcAnch and destAnch parameters are used in 'm' mode to specify the anchor points
// within the rectangles as percentages (0-100) of width and height.
QTENV_API QPolygonF arrowcoords(const QRectF &srcRect, const QRectF &destRect,
                  int bundle_i = 0, int bundle_n = 1, // bundle index and size
                  char mode = 'a', // must be one of "amnews"
                  QPointF srcAnch = QPointF(50, 50),
                  QPointF destAnch = QPointF(50, 50));

}  // namespace qtenv
}  // namespace omnetpp


#endif

