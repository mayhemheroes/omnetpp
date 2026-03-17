//=========================================================================
//  BACKWARDUTIL.H - part of
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 2006-2017 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __OMNETPP_COMMON_BACKWARDUTIL_H
#define __OMNETPP_COMMON_BACKWARDUTIL_H

#include "commondefs.h"

namespace omnetpp {
namespace common {

/**
 * Returns true if stack trace printing is available (compiled with WITH_BACKTRACE).
 */
COMMON_API bool isStacktraceAvailable();

/**
 * Prints a stack trace to stderr. The numFramesToSkip parameter specifies
 * how many stack frames to omit from the top of the trace.
 */
COMMON_API void printStacktrace(int numFramesToSkip = 0);

}  // namespace common
}  // namespace omnetpp

#endif
