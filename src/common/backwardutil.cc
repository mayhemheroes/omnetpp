#include <cstdio>
#ifdef WITH_BACKTRACE
#include "backward.h"
#endif
#include "backwardutil.h"

namespace omnetpp {
namespace common {

bool isStacktraceAvailable()
{
#ifdef WITH_BACKTRACE
    return true;
#else
    return false;
#endif
}

void printStacktrace(int numFramesToSkip)
{
#ifdef WITH_BACKTRACE
    fprintf(stderr, "\n--- Stack trace begin ---\n");
    backward::StackTrace st;
    st.load_here(32);
    st.skip_n_firsts(numFramesToSkip);
    backward::Printer p;
    p.print(st, stderr);
    fprintf(stderr, "--- Stack trace end ---\n");
#endif
}

}  // namespace common
}  // namespace omnetpp
