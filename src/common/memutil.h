//=========================================================================
//  MEMUTIL.H - part of
//
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//=========================================================================

/*--------------------------------------------------------------*
  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#ifndef __OMNETPP_COMMON_MEMUTIL_H
#define __OMNETPP_COMMON_MEMUTIL_H

#include "commondefs.h"

namespace omnetpp {
namespace common {

/**
 * Returns the amount of memory actually used by the current process in bytes,
 * counting both pages resident in RAM and pages swapped/paged out.
 * Shared code/library pages are excluded. Returns 0 if unavailable.
 *
 * Corresponds to VmRSS+VmSwap on Linux, phys_footprint on macOS
 * (as shown by Activity Monitor), and PrivateUsage on Windows
 * (as shown by Task Manager's "Commit Size").
 */
COMMON_API size_t opp_memory_usage();

}  // namespace common
}  // namespace omnetpp

#endif
