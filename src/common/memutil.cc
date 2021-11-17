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
#include <cstdio>
#include <cstring>

#ifdef __linux__
# include <unistd.h>
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
    // Sum of resident (VmRSS) and swapped-out (VmSwap) private memory,
    // in bytes. This corresponds to memory actually used by the process,
    // regardless of whether it is currently paged in or out.
    FILE *file = fopen("/proc/self/status", "r");
    if (!file)
        return 0;
    size_t rssKB = 0, swapKB = 0;
    char line[256];
    while (fgets(line, sizeof(line), file)) {
        if (strncmp(line, "VmRSS:", 6) == 0)
            sscanf(line + 6, "%zu", &rssKB);
        else if (strncmp(line, "VmSwap:", 7) == 0)
            sscanf(line + 7, "%zu", &swapKB);
    }
    fclose(file);
    return (rssKB + swapKB) * (size_t)1024;

#elif defined(__APPLE__)
    // phys_footprint = resident + compressed + swapped private memory,
    // i.e. the "Memory" column in Activity Monitor.
    task_vm_info_data_t vm_info;
    mach_msg_type_number_t count = TASK_VM_INFO_COUNT;
    if (KERN_SUCCESS == task_info(mach_task_self(), TASK_VM_INFO,
                                  (task_info_t)&vm_info, &count))
        return (size_t)vm_info.phys_footprint;
    return 0;

#elif defined(_WIN32)
    // PrivateUsage = private commit charge (RAM + pagefile),
    // i.e. the "Commit Size" column in Task Manager.
    PROCESS_MEMORY_COUNTERS_EX counters;
    if (GetProcessMemoryInfo(GetCurrentProcess(),
                             (PROCESS_MEMORY_COUNTERS *)&counters,
                             sizeof(counters)))
        return counters.PrivateUsage;
    return 0;

#else
    return 0;
#endif
}

}  // namespace common
}  // namespace omnetpp
