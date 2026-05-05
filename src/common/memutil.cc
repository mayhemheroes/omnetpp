//=========================================================================
//  MEMUTIL.CC - part of
//
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//=========================================================================

/*--------------------------------------------------------------*
  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include "memutil.h"

#ifdef __linux__
# include <unistd.h>
# include <sys/resource.h>
#endif

#ifdef __APPLE__
# include <mach/mach.h>
#endif

#ifdef _WIN32
# include <windows.h>
# include <psapi.h>
#endif

namespace omnetpp {
namespace common {

size_t opp_memory_usage()
{
#if defined(__linux__)
    // Resident set size (RSS) in bytes. Note: getrusage() does not report
    // swapped-out memory
    struct rusage usage;
    if (getrusage(RUSAGE_SELF, &usage) == 0)
        return (size_t)usage.ru_maxrss * (size_t)1024;
    return 0;

#elif defined(__APPLE__)
    // Resident set size (RSS) in bytes
    mach_task_basic_info_data_t info;
    mach_msg_type_number_t count = MACH_TASK_BASIC_INFO_COUNT;
    if (KERN_SUCCESS == task_info(mach_task_self(), MACH_TASK_BASIC_INFO,
                                  (task_info_t)&info, &count))
        return (size_t)info.resident_size;
    return 0;

#elif defined(_WIN32)
    // Resident set size (working set) in bytes
    PROCESS_MEMORY_COUNTERS counters;
    if (GetProcessMemoryInfo(GetCurrentProcess(),
                             &counters, sizeof(counters)))
        return counters.WorkingSetSize;
    return 0;

#else
    return 0;
#endif
}

}  // namespace common
}  // namespace omnetpp
