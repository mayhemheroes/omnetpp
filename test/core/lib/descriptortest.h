#ifndef DESCRIPTORTEST_H
#define DESCRIPTORTEST_H

#include <omnetpp.h>

using namespace omnetpp;

namespace testlib {

// Print object contents using the given descriptor, like Qtenv Object Inspector.
// Recursively expands compound fields and array elements with indentation.
void printDescriptor(std::ostream& out, any_ptr object, cClassDescriptor *desc, int indent = 0);

}

#endif
