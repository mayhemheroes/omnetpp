//=========================================================================
//  STATSUM_T.H - part of
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2017 Andras Varga
  Copyright (C) 2006-2017 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __OMNETPP_COMMON_STATSUM_T_H
#define __OMNETPP_COMMON_STATSUM_T_H

#include <cmath>
#include "commondefs.h"
#include "omnetpp/platdep/config.h"

namespace omnetpp {
namespace common {

/**
 * Compensated summation using the Kahan-Babuška-Neumaier (KBN) algorithm.
 *
 * This class can be used as a drop-in replacement for `double` in running
 * summation scenarios. It significantly reduces the numerical error that
 * accumulates when many floating-point values are added together.
 *
 * The class provides implicit conversion to `double` (returning the
 * compensated sum), `operator+=` for accumulation, and assignment from
 * `double`.
 */
class COMMON_API NeumaierSum
{
  private:
    double sum = 0;
    double comp = 0;  // running compensation for lost low-order bits

  public:
    /** Default constructor, initializes to zero. */
    NeumaierSum() = default;

    /** Constructor, initializes to the given value. */
    NeumaierSum(double val) : sum(val), comp(0) {}

    /** Assigns a plain double value, resetting the compensation term. */
    NeumaierSum& operator=(double val) { sum = val; comp = 0; return *this; }

    /** Returns the compensated sum as a double. */
    operator double() const { return sum + comp; }

    /** Adds a value using the Neumaier compensated summation algorithm. */
    NeumaierSum& operator+=(double value) {
        double t = sum + value;
        if (std::abs(sum) >= std::abs(value))
            comp += (sum - t) + value;
        else
            comp += (value - t) + sum;
        sum = t;
        return *this;
    }
};

#ifdef WITH_NEUMAIER_SUMMATION
using statsum_t = NeumaierSum;
#else
using statsum_t = double;
#endif

}  // namespace common
}  // namespace omnetpp

#endif
