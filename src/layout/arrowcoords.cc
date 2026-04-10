//==========================================================================
//  ARROWCOORDS.CC - part of
//
//                     OMNeT++/OMNEST
//
//  Implementation of
//   connection arrow positioning in module drawing
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2017 Andras Varga
  Copyright (C) 2006-2017 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include "arrowcoords.h"

#include <cstdio>
#include <cstring>
#include <cstdlib>
#include <cmath>
#include <algorithm>

#include "common/commonutil.h" // for Assert

namespace omnetpp {
namespace layout {

// Adjust p1 toward p2 so that it fits on the rectangle boundary.
// If p2 is inside the rectangle, do nothing.
static void clip_line_to_rect(Pt& p1, const Pt& p2, const Rc& rect)
{
    int p1_inside = rect.contains(p1);
    int p2_inside = rect.contains(p2);

    // we'll clip the line to two edges of the rect: y=cy (horiz) and x=cx (vert)
    double cx, cy;

    if (p1_inside && p2_inside) {
        cx = p1.x < p2.x ? rect.getLeft() : rect.getRight();
        cy = p1.y < p2.y ? rect.getTop() : rect.getBottom();
    }
    else if (p1_inside) {
        cx = p1.x < p2.x ? rect.getRight() : rect.getLeft();
        cy = p1.y < p2.y ? rect.getBottom() : rect.getTop();
    }
    else if (p2_inside) {
        cx = p1.x < p2.x ? rect.getLeft() : rect.getRight();
        cy = p1.y < p2.y ? rect.getTop() : rect.getBottom();
    }
    else {
        cx = p1.x < p2.x ? rect.getRight() : rect.getLeft();
        cy = p1.y < p2.y ? rect.getBottom() : rect.getTop();
    }

    // first, deal with the special cases: line is vert or horiz
    if (p1.x == p2.x) {
        if (p1.x < rect.getLeft())
            p1.x = rect.getLeft();
        if (p1.x > rect.getRight())
            p1.x = rect.getRight();
        if (p1.x != rect.getLeft() && p1.x != rect.getRight())
            p1.y = cy;
        return;
    }

    if (p1.y == p2.y) {
        if (p1.y < rect.getTop())
            p1.y = rect.getTop();
        if (p1.y > rect.getBottom())
            p1.y = rect.getBottom();
        if (p1.y != rect.getTop() && p1.y != rect.getBottom())
            p1.x = cx;
        return;
    }

    // write the line into y=ax+b form
    double a = (p2.y - p1.y) / (p2.x - p1.x);
    double b = p1.y - a * p1.x;

    double xx, yy;

    // clip to the y=cy horizontal edge of the rect
    xx = (cy - b) / a;  // ==> (xx,cy)

    // clip to the x=cx vertical edge of the rect
    yy = a * cx + b;  // ==> (cx,yy)

    if (xx >= rect.getLeft() && xx <= rect.getRight()) {
        p1.x = xx;
        p1.y = cy;
    }
    else if (yy >= rect.getTop() && yy <= rect.getBottom()) {
        p1.x = cx;
        p1.y = yy;
    }
    else {
        p1.x = cx;
        p1.y = cy;
    }
    // default: rect and line don't have any common point :-(
    // leave line unchanged
}

// A special case of "arrowcoords" where one rectangle is
// fully contained within the other.
// NOTE: The result will always point from inner to outer.
static std::vector<Pt> arrowcoords_contained(
    const Rc& innerRect,
    const Rc& outerRect,
    int bundle_i, int bundle_n, // bundle index and size
    char mode) // "anews"
{
    double src_x, src_y, dest_x, dest_y;

    // a - N,E,S,W (to nearest border,calculated from side)
    if (mode == 'a') {
        double top = innerRect.getTop() - outerRect.getTop(),
                bottom = outerRect.getBottom() - innerRect.getBottom(),
                left = innerRect.getLeft() - outerRect.getLeft(),
                right = outerRect.getRight() - innerRect.getRight();
        if (top <= bottom && top <= left && top <= right)
            mode = 'n';
        else if (right <= bottom && right <= left && right <= top)
            mode = 'e';
        else if (left <= bottom && left <= right && left <= top)
            mode = 'w';
        else if (bottom <= left && bottom <= right && bottom <= top)
            mode = 's';
    }

    //  E,W - connection points E or W. Vert shift by gate indices
    //  N,S - connection points N or S. Horiz shift by gate indices
    switch (mode) {
        case 'n':
            src_x = dest_x = innerRect.getLeft() + (bundle_i+1) * innerRect.rs.width / (bundle_n+1);
            src_y = innerRect.getTop();
            dest_y = outerRect.getTop();
            break;

        case 's':
            src_x = dest_x = innerRect.getLeft() + (bundle_i+1) * innerRect.rs.width / (bundle_n+1);
            src_y = innerRect.getBottom();
            dest_y = outerRect.getBottom();
            break;

        case 'e':
            src_x = innerRect.getRight();
            dest_x = outerRect.getRight();
            src_y = dest_y = innerRect.getTop() + (bundle_i+1) * innerRect.rs.height / (bundle_n+1);
            break;

        case 'w':
            src_x = innerRect.getLeft();
            dest_x = outerRect.getLeft();
            src_y = dest_y = innerRect.getTop() + (bundle_i+1) * innerRect.rs.height / (bundle_n+1);
            break;
    }

    return {Pt(src_x, src_y, 0), Pt(dest_x, dest_y, 0)};
}

// Adjusts two intervals ([smaller_1, smaller_2] and [larger_1, larger_2]),
// so that they are the same size (at least half of smaller), and overlap
// as much as possible, while not expanding either in any direction.
// Think of it as a "specialized intersection with a lower bound on size".
static void junctionrect(double &smaller_1, double &smaller_2, double &larger_1, double &larger_2)
{
    double smaller_s = smaller_2 - smaller_1;
    double intersection_1 = std::max(smaller_1, larger_1);
    double intersection_2 = std::min(smaller_2, larger_2);
    double intersection_s = intersection_2 - intersection_1;
    if (intersection_s >= smaller_s / 2) {
        smaller_1 = intersection_1;
        smaller_2 = intersection_2;
        larger_1 = intersection_1;
        larger_2 = intersection_2;
    }
    else {
        double final_s = std::max(smaller_s / 2, intersection_s);
        if (smaller_1 < larger_1) {
            smaller_1 = smaller_2 - final_s;
            larger_2 = larger_1 + final_s;
        }
        else {
            smaller_2 = smaller_1 + final_s;
            larger_1 = larger_2 - final_s;
        }
    }
}

// Returns the distance between the (infinite) line across the origin,
// in the direction of `dir`. `dir` must be unit length!
// Basically, the magnitude of the cross product.
static double perpendicularDistance(const Pt& dir, const Pt& point)
{
    return std::abs(dir.y * point.x - dir.x * point.y);
}

static inline bool fuzzyCompare(double a, double b)
{
    return std::abs(a - b) <= 1e-9 * std::max(1.0, std::max(std::abs(a), std::abs(b)));
}

std::vector<Pt> arrowcoords(const Rc &srcRect, const Rc &destRect,
                  int bundle_i, int bundle_n, // bundle index and size
                  char mode, // "amnews"
                  double srcAnchX, double srcAnchY,
                  double destAnchX, double destAnchY)
{
    Pt src(0, 0, 0), dest(0, 0, 0);

    // error checks
    Assert(strchr("amnews", mode)); // mode must be one of (a,m,n,e,w,s)

    // see if the two rects are the same, one is in the other etc.
    enum class Relation {
        SAME_RECT,
        SRC_WITHIN_DEST,
        DEST_WITHIN_SRC,
        OVERLAPPING,
        DISJOINT
    };

    Relation rel =
        (srcRect == destRect) ? Relation::SAME_RECT :
        srcRect.contains(destRect) ? Relation::DEST_WITHIN_SRC :
        destRect.contains(srcRect) ? Relation::SRC_WITHIN_DEST :
        srcRect.intersected(destRect).isValid() ? Relation::OVERLAPPING :
        Relation::DISJOINT;

    // prepare for mode 'm'
    if (mode != 'm')
        srcAnchX = srcAnchY = destAnchX = destAnchY = 50;
    src.x = srcRect.getLeft() + srcAnchX * srcRect.rs.width / 100;
    src.y = srcRect.getTop() + srcAnchY * srcRect.rs.height / 100;
    dest.x = destRect.getLeft() + destAnchX * destRect.rs.width / 100;
    dest.y = destRect.getTop() + destAnchY * destRect.rs.height / 100;
    // handle 'm' mode first
    if (mode == 'm') {
        if (rel != Relation::OVERLAPPING) {
            clip_line_to_rect(src, dest, srcRect);
            clip_line_to_rect(dest, src, destRect);
        }

        return {src, dest};
    }

    // where this connection should be shifted within the available range
    double bundle_coeff = (bundle_i + 1.0) / (bundle_n + 1.0);

    // do all rectangle relations one-by-one
    switch (rel) {
        case Relation::SAME_RECT: {
            // a - height>width: E or W
            //     otherwise:    N or S
            if (mode == 'a')
                mode = (srcRect.rs.height > srcRect.rs.width) ? 'e' : 'n';

            //  E,W - connection points E or W. Vert shift by gate indices
            //  N,S - connection points N or S. Horiz shift by gate indices
            switch (mode) {
                case 'n':
                    src.x = dest.x = srcRect.getLeft() + bundle_coeff * srcRect.rs.width;
                    src.y = srcRect.getBottom();
                    dest.y = srcRect.getTop();
                    break;

                case 's':
                    src.x = dest.x = srcRect.getLeft() + bundle_coeff * srcRect.rs.width;
                    src.y = srcRect.getTop();
                    dest.y = srcRect.getBottom();
                    break;

                case 'e':
                    src.x = srcRect.getLeft();
                    dest.x = srcRect.getRight();
                    src.y = dest.y = srcRect.getTop() + bundle_coeff * srcRect.rs.height;
                    break;

                case 'w':
                    src.x = srcRect.getRight();
                    dest.x = srcRect.getLeft();
                    src.y = dest.y = srcRect.getTop() + bundle_coeff * srcRect.rs.height;
                    break;
            }
        }
        break;
        case Relation::SRC_WITHIN_DEST:
            return arrowcoords_contained(srcRect,
                                     destRect,
                                     bundle_i, bundle_n,
                                     mode);
        case Relation::DEST_WITHIN_SRC: {
            std::vector<Pt> pts = arrowcoords_contained(destRect,
                                            srcRect,
                                            bundle_i, bundle_n,
                                            mode);
            // flip the line so it points inward
            std::reverse(pts.begin(), pts.end());
            return pts;
        }
        case Relation::OVERLAPPING:
        case Relation::DISJOINT: {
            // disjoint (or partially overlapping) rectangles
            //  a - E,W if one module's y range is within y range of other module
            //      N,S if one module's x range is within x range of other module
            //      otherwise M mode with (50%,50%) (50%,50%)

            // The algorithm finds smaller "junction rectangles" (of equal size),
            // one within both srcRect and destRect, as close to each other as
            // possible - using junctionrect(). The endpoints of the connection
            // lines will be first placed within these rectangles, and then
            // clipped to the edge of the original rectangles.

            Rc src_wireRect = srcRect;
            Rc dest_wireRect = destRect;

            if (srcRect.rs.width < destRect.rs.width) {
                double smaller_1 = srcRect.getLeft();
                double smaller_2 = srcRect.getRight();
                double larger_1 = destRect.getLeft();
                double larger_2 = destRect.getRight();
                junctionrect(smaller_1, smaller_2, larger_1, larger_2);
                src_wireRect.setLeft(smaller_1);
                src_wireRect.setRight(smaller_2);
                dest_wireRect.setLeft(larger_1);
                dest_wireRect.setRight(larger_2);
            } else {
                double smaller_1 = destRect.getLeft();
                double smaller_2 = destRect.getRight();
                double larger_1 = srcRect.getLeft();
                double larger_2 = srcRect.getRight();
                junctionrect(smaller_1, smaller_2, larger_1, larger_2);
                dest_wireRect.setLeft(smaller_1);
                dest_wireRect.setRight(smaller_2);
                src_wireRect.setLeft(larger_1);
                src_wireRect.setRight(larger_2);
            }

            if (srcRect.rs.height < destRect.rs.height) {
                double smaller_1 = srcRect.getTop();
                double smaller_2 = srcRect.getBottom();
                double larger_1 = destRect.getTop();
                double larger_2 = destRect.getBottom();
                junctionrect(smaller_1, smaller_2, larger_1, larger_2);
                src_wireRect.setTop(smaller_1);
                src_wireRect.setBottom(smaller_2);
                dest_wireRect.setTop(larger_1);
                dest_wireRect.setBottom(larger_2);
            } else {
                double smaller_1 = destRect.getTop();
                double smaller_2 = destRect.getBottom();
                double larger_1 = srcRect.getTop();
                double larger_2 = srcRect.getBottom();
                junctionrect(smaller_1, smaller_2, larger_1, larger_2);
                dest_wireRect.setTop(smaller_1);
                dest_wireRect.setBottom(smaller_2);
                src_wireRect.setTop(larger_1);
                src_wireRect.setBottom(larger_2);
            }

            // they must be the same size
            Assert(fuzzyCompare(src_wireRect.rs.width, dest_wireRect.rs.width));
            Assert(fuzzyCompare(src_wireRect.rs.height, dest_wireRect.rs.height));

            src = src_wireRect.center();
            dest = dest_wireRect.center();

            if (src == dest) {
                // Rectangles overlap too much (ie. the "junction" rectangles in them
                // are the same), pick centers of the original rectangles instead.
                src = srcRect.center();
                dest = destRect.center();
            }

            if (src == dest) {
                // Last resort: just nudge them apart a bit...
                // This can only happen if the rectangles have the same
                // center but one is wider and the other is taller.
                dest.x += 1;
            }

            Ln centerLine(src, dest);

            Pt dir = centerLine.unitVector().end - src;
            // Computing the extent of src_wireRect in the direction of dest_wireRect.
            // (how far we can move lines between their centers perpendicularly without
            // exiting the rectangles).
            double spread = std::max(
                perpendicularDistance(dir, Pt(src_wireRect.rs.width, src_wireRect.rs.height, 0)),
                perpendicularDistance(dir, Pt(-src_wireRect.rs.width, src_wireRect.rs.height, 0))
            );

            Pt normDir(dir.y, -dir.x, 0); // perpendicular
            // make direction consistent (left/right and top/bottom)
            if (dir.x < dir.y || (dir.x == dir.y && normDir.x < normDir.y))
                normDir = -normDir;

            Pt offs = normDir * spread * (bundle_coeff - 0.5);

            src += offs;
            dest += offs;

            // clip the line to the bounding rectangles if they are not overlapping
            if (rel == Relation::DISJOINT) {
                clip_line_to_rect(src, dest, srcRect);
                clip_line_to_rect(dest, src, destRect);
            }
        }
    }

    std::vector<Pt> result = {src, dest};

    // TEMPORARY: insert a random midpoint for testing polyline rendering.
    // This will be replaced with real routing logic later.
    if (result.size() == 2) {
        Pt s = result[0], d = result[1];
        Pt mid((s.x + d.x) / 2, (s.y + d.y) / 2, 0);
        // perpendicular offset based on a hash of the coordinates (deterministic per connection)
        double dx = d.x - s.x;
        double dy = d.y - s.y;
        double len = std::sqrt(dx*dx + dy*dy);
        if (len > 1) {
            // use a simple hash to get a deterministic but varied offset
            //int hash = (int)(s.x * 7 + s.y * 13 + d.x * 17 + d.y * 23);
            //double offset = 15 + (hash % 30); // 15..44 pixels
            double offset = dy < 0 ? 30 : -30;
            //if (hash % 2) offset = -offset;
            mid.x += -dy / len * offset;
            mid.y += dx / len * offset;
            result = {s, mid, d};
        }
    }

    return result;
}

}  // namespace layout
}  // namespace omnetpp
