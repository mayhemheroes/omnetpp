//==========================================================================
//  CDYNAMICDESCRIPTOR.CC - part of
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2017 Andras Varga
  Copyright (C) 2006-2017 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include "omnetpp/cdynamicdescriptor.h"
#include "omnetpp/cexception.h"
#include "omnetpp/cvalue.h"

namespace omnetpp {
namespace internal {

cDynamicDescriptor::cDynamicDescriptor(const char *className, const char *baseClassName)
    : cClassDescriptor(className, baseClassName)
{
}

const char **cDynamicDescriptor::getPropertyNames() const
{
    static const char *empty[] = {nullptr};
    return empty;
}

const char *cDynamicDescriptor::getProperty(const char *propertyName) const
{
    return nullptr;
}

int cDynamicDescriptor::getFieldCount() const
{
    return 0;
}

const char *cDynamicDescriptor::getFieldName(int field) const
{
    return nullptr;
}

unsigned int cDynamicDescriptor::getFieldTypeFlags(int field) const
{
    return 0;
}

const char *cDynamicDescriptor::getFieldTypeString(int field) const
{
    return nullptr;
}

const char **cDynamicDescriptor::getFieldPropertyNames(int field) const
{
    static const char *empty[] = {nullptr};
    return empty;
}

const char *cDynamicDescriptor::getFieldProperty(int field, const char *propertyName) const
{
    return nullptr;
}

int cDynamicDescriptor::getFieldArraySize(any_ptr object, int field) const
{
    return 0;
}

void cDynamicDescriptor::setFieldArraySize(any_ptr object, int field, int size) const
{
    throw cRuntimeError("Cannot set array size of field %d", field);
}

std::string cDynamicDescriptor::getFieldValueAsString(any_ptr object, int field, int i) const
{
    return "";
}

void cDynamicDescriptor::setFieldValueAsString(any_ptr object, int field, int i, const char *value) const
{
    throw cRuntimeError("Cannot set field %d", field);
}

cValue cDynamicDescriptor::getFieldValue(any_ptr object, int field, int i) const
{
    throw cRuntimeError("Cannot return field %d as cValue", field);
}

void cDynamicDescriptor::setFieldValue(any_ptr object, int field, int i, const cValue& value) const
{
    throw cRuntimeError("Cannot set field %d from cValue", field);
}

const char *cDynamicDescriptor::getFieldStructName(int field) const
{
    return nullptr;
}

any_ptr cDynamicDescriptor::getFieldStructValuePointer(any_ptr object, int field, int i) const
{
    return any_ptr(nullptr);
}

void cDynamicDescriptor::setFieldStructValuePointer(any_ptr object, int field, int i, any_ptr ptr) const
{
    throw cRuntimeError("Cannot set struct pointer for field %d", field);
}

}  // namespace internal
}  // namespace omnetpp
