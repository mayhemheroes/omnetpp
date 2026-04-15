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
    char srcDir) // 'n','e','s','w' or '\0' (auto = nearest border)
{
    double src_x, src_y, dest_x, dest_y;

    // '\0' (auto) - pick nearest border
    if (srcDir == '\0') {
        double top = innerRect.getTop() - outerRect.getTop(),
                bottom = outerRect.getBottom() - innerRect.getBottom(),
                left = innerRect.getLeft() - outerRect.getLeft(),
                right = outerRect.getRight() - innerRect.getRight();
        if (top <= bottom && top <= left && top <= right)
            srcDir = 'n';
        else if (right <= bottom && right <= left && right <= top)
            srcDir = 'e';
        else if (left <= bottom && left <= right && left <= top)
            srcDir = 'w';
        else if (bottom <= left && bottom <= right && bottom <= top)
            srcDir = 's';
    }

    //  E,W - connection points E or W. Vert shift by gate indices
    //  N,S - connection points N or S. Horiz shift by gate indices
    switch (srcDir) {
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
                  const RoutingConstraint& constraint)
{
    char srcDir = constraint.srcDir;
    char destDir = constraint.destDir;

    Pt src(0, 0, 0), dest(0, 0, 0);

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

    // --- Legacy manual mode: srcDir == 'm' ---
    if (srcDir == 'm') {
        double srcAnchX = constraint.srcAnchX;
        double srcAnchY = constraint.srcAnchY;
        double destAnchX = constraint.destAnchX;
        double destAnchY = constraint.destAnchY;
        src.x = srcRect.getLeft() + srcAnchX * srcRect.rs.width / 100;
        src.y = srcRect.getTop() + srcAnchY * srcRect.rs.height / 100;
        dest.x = destRect.getLeft() + destAnchX * destRect.rs.width / 100;
        dest.y = destRect.getTop() + destAnchY * destRect.rs.height / 100;
        if (rel != Relation::OVERLAPPING) {
            clip_line_to_rect(src, dest, srcRect);
            clip_line_to_rect(dest, src, destRect);
        }
        return {src, dest};
    }

    // where this connection should be shifted within the available range
    double bundle_coeff = (bundle_i + 1.0) / (bundle_n + 1.0);

    // --- Containment cases ---
    if (rel == Relation::SRC_WITHIN_DEST)
        return arrowcoords_contained(srcRect, destRect, bundle_i, bundle_n, srcDir);
    if (rel == Relation::DEST_WITHIN_SRC) {
        std::vector<Pt> pts = arrowcoords_contained(destRect, srcRect, bundle_i, bundle_n, srcDir);
        std::reverse(pts.begin(), pts.end());
        return pts;
    }

    // --- Same-rect case ---
    if (rel == Relation::SAME_RECT) {
        // Normalize cardinal to axis
        char dir = srcDir;
        if (dir == 'e' || dir == 'w') dir = 'h';
        if (dir == 'n' || dir == 's') dir = 'v';
        // Auto: pick axis based on aspect ratio
        if (dir == '\0' || dir == 'h' || dir == 'v')
            ; // keep it
        else
            dir = '\0'; // unknown -> auto
        if (dir == '\0')
            dir = (srcRect.rs.height > srcRect.rs.width) ? 'h' : 'v';
        if (dir == 'h') {
            src.x = srcRect.getLeft();
            dest.x = srcRect.getRight();
            src.y = dest.y = srcRect.getTop() + bundle_coeff * srcRect.rs.height;
        } else { // 'v'
            src.x = dest.x = srcRect.getLeft() + bundle_coeff * srcRect.rs.width;
            src.y = srcRect.getTop();
            dest.y = srcRect.getBottom();
        }
        return {src, dest};
    }

    // --- Disjoint / Overlapping rectangles ---
    Assert(rel == Relation::DISJOINT || rel == Relation::OVERLAPPING);

    Pt srcCenter = srcRect.center();
    Pt destCenter = destRect.center();

    // Normalize cardinal directions to axis: e/w -> 'h', n/s -> 'v'
    if (srcDir == 'e' || srcDir == 'w') srcDir = 'h';
    if (srcDir == 'n' || srcDir == 's') srcDir = 'v';
    if (destDir == 'e' || destDir == 'w') destDir = 'h';
    if (destDir == 'n' || destDir == 's') destDir = 'v';

    // Invalidate same-axis combinations (h,h or v,v) -- can't form an L-shape
    if (srcDir != '\0' && srcDir == destDir) {
        srcDir = '\0';
        destDir = '\0';
    }

    // --- Rule 1: Overlap rule (strongest) ---
    // If there is meaningful overlap in x or y, draw a straight H or V line,
    // regardless of any specified directions.
    constexpr double OVERLAP_THRESHOLD = 10.0;

    double y_overlap_top = std::max(srcRect.getTop(), destRect.getTop());
    double y_overlap_bot = std::min(srcRect.getBottom(), destRect.getBottom());
    double y_overlap = y_overlap_bot - y_overlap_top;

    double x_overlap_left = std::max(srcRect.getLeft(), destRect.getLeft());
    double x_overlap_right = std::min(srcRect.getRight(), destRect.getRight());
    double x_overlap = x_overlap_right - x_overlap_left;

    if (y_overlap >= OVERLAP_THRESHOLD) {
        // Straight horizontal line through the y-overlap zone
        double y = y_overlap_top + bundle_coeff * y_overlap;
        src.y = dest.y = y;
        if (destCenter.x >= srcCenter.x) {
            src.x = srcRect.getRight();
            dest.x = destRect.getLeft();
        } else {
            src.x = srcRect.getLeft();
            dest.x = destRect.getRight();
        }
        return {src, dest};
    }

    if (x_overlap >= OVERLAP_THRESHOLD) {
        // Straight vertical line through the x-overlap zone
        double x = x_overlap_left + bundle_coeff * x_overlap;
        src.x = dest.x = x;
        if (destCenter.y >= srcCenter.y) {
            src.y = srcRect.getBottom();
            dest.y = destRect.getTop();
        } else {
            src.y = srcRect.getTop();
            dest.y = destRect.getBottom();
        }
        return {src, dest};
    }

    // --- Rule 2: L-shape (no overlap, at least one direction specified) ---
    if (srcDir != '\0' || destDir != '\0') {
        // Determine concrete src and dest exit/entry edges from the axis + geometry
        char srcEdge, destEdge;

        if (srcDir == 'h')
            srcEdge = (destCenter.x >= srcCenter.x) ? 'e' : 'w';
        else if (srcDir == 'v')
            srcEdge = (destCenter.y >= srcCenter.y) ? 's' : 'n';
        else {
            // srcDir unconstrained: use the perpendicular axis to destDir
            if (destDir == 'h')
                srcEdge = (destCenter.y >= srcCenter.y) ? 's' : 'n';
            else
                srcEdge = (destCenter.x >= srcCenter.x) ? 'e' : 'w';
        }

        if (destDir == 'h')
            destEdge = (srcCenter.x >= destCenter.x) ? 'e' : 'w';
        else if (destDir == 'v')
            destEdge = (srcCenter.y >= destCenter.y) ? 's' : 'n';
        else {
            // destDir unconstrained: use the perpendicular axis to srcDir
            if (srcDir == 'h')
                destEdge = (srcCenter.y >= destCenter.y) ? 's' : 'n';
            else
                destEdge = (srcCenter.x >= destCenter.x) ? 'e' : 'w';
        }

        // Compute exit point on src edge
        switch (srcEdge) {
            case 'e': src.x = srcRect.getRight();  src.y = srcRect.getTop() + bundle_coeff * srcRect.rs.height; break;
            case 'w': src.x = srcRect.getLeft();   src.y = srcRect.getTop() + bundle_coeff * srcRect.rs.height; break;
            case 's': src.y = srcRect.getBottom();  src.x = srcRect.getLeft() + bundle_coeff * srcRect.rs.width; break;
            case 'n': src.y = srcRect.getTop();     src.x = srcRect.getLeft() + bundle_coeff * srcRect.rs.width; break;
        }

        // Compute entry point on dest edge
        switch (destEdge) {
            case 'e': dest.x = destRect.getRight();  dest.y = destRect.getTop() + bundle_coeff * destRect.rs.height; break;
            case 'w': dest.x = destRect.getLeft();   dest.y = destRect.getTop() + bundle_coeff * destRect.rs.height; break;
            case 's': dest.y = destRect.getBottom();  dest.x = destRect.getLeft() + bundle_coeff * destRect.rs.width; break;
            case 'n': dest.y = destRect.getTop();     dest.x = destRect.getLeft() + bundle_coeff * destRect.rs.width; break;
        }

        // Bend point: intersection of H and V segments
        Pt bend(0, 0, 0);
        bool srcIsHorizontal = (srcEdge == 'e' || srcEdge == 'w');
        if (srcIsHorizontal) {
            bend.x = dest.x;
            bend.y = src.y;
        } else {
            bend.x = src.x;
            bend.y = dest.y;
        }

        // Degenerate L → straight line
        if ((bend.x == src.x && bend.y == src.y) || (bend.x == dest.x && bend.y == dest.y))
            return {src, dest};

        return {src, bend, dest};
    }

    // --- Rule 3: Fallback — straight line (no direction specified) ---
    {
        Rc src_wireRect = srcRect;
        Rc dest_wireRect = destRect;

        if (srcRect.rs.width < destRect.rs.width) {
            double smaller_1 = srcRect.getLeft(), smaller_2 = srcRect.getRight();
            double larger_1 = destRect.getLeft(), larger_2 = destRect.getRight();
            junctionrect(smaller_1, smaller_2, larger_1, larger_2);
            src_wireRect.setLeft(smaller_1); src_wireRect.setRight(smaller_2);
            dest_wireRect.setLeft(larger_1); dest_wireRect.setRight(larger_2);
        } else {
            double smaller_1 = destRect.getLeft(), smaller_2 = destRect.getRight();
            double larger_1 = srcRect.getLeft(), larger_2 = srcRect.getRight();
            junctionrect(smaller_1, smaller_2, larger_1, larger_2);
            dest_wireRect.setLeft(smaller_1); dest_wireRect.setRight(smaller_2);
            src_wireRect.setLeft(larger_1); src_wireRect.setRight(larger_2);
        }

        if (srcRect.rs.height < destRect.rs.height) {
            double smaller_1 = srcRect.getTop(), smaller_2 = srcRect.getBottom();
            double larger_1 = destRect.getTop(), larger_2 = destRect.getBottom();
            junctionrect(smaller_1, smaller_2, larger_1, larger_2);
            src_wireRect.setTop(smaller_1); src_wireRect.setBottom(smaller_2);
            dest_wireRect.setTop(larger_1); dest_wireRect.setBottom(larger_2);
        } else {
            double smaller_1 = destRect.getTop(), smaller_2 = destRect.getBottom();
            double larger_1 = srcRect.getTop(), larger_2 = srcRect.getBottom();
            junctionrect(smaller_1, smaller_2, larger_1, larger_2);
            dest_wireRect.setTop(smaller_1); dest_wireRect.setBottom(smaller_2);
            src_wireRect.setTop(larger_1); src_wireRect.setBottom(larger_2);
        }

        Assert(fuzzyCompare(src_wireRect.rs.width, dest_wireRect.rs.width));
        Assert(fuzzyCompare(src_wireRect.rs.height, dest_wireRect.rs.height));

        src = src_wireRect.center();
        dest = dest_wireRect.center();

        if (src == dest) {
            src = srcRect.center();
            dest = destRect.center();
        }
        if (src == dest)
            dest.x += 1;

        Ln centerLine(src, dest);
        Pt dir = centerLine.unitVector().end - src;
        double spread = std::max(
            perpendicularDistance(dir, Pt(src_wireRect.rs.width, src_wireRect.rs.height, 0)),
            perpendicularDistance(dir, Pt(-src_wireRect.rs.width, src_wireRect.rs.height, 0))
        );

        Pt normDir(dir.y, -dir.x, 0);
        if (dir.x < dir.y || (dir.x == dir.y && normDir.x < normDir.y))
            normDir = -normDir;

        Pt offs = normDir * spread * (bundle_coeff - 0.5);
        src += offs;
        dest += offs;

        if (rel == Relation::DISJOINT) {
            clip_line_to_rect(src, dest, srcRect);
            clip_line_to_rect(dest, src, destRect);
        }

        return {src, dest};
    }
}

}  // namespace layout
}  // namespace omnetpp
