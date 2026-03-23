//=========================================================================
//  CWATCH.CC - part of
//
//                  OMNeT++/OMNEST
//           Discrete System Simulation in C++
//
//
//   Member functions of
//    cWatchBase etc: make primitive types, structs etc inspectable
//
//=========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2017 Andras Varga
  Copyright (C) 2006-2017 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include "common/stringutil.h"
#include "omnetpp/cwatch.h"
#include "omnetpp/globals.h"
#include "omnetpp/cclassdescriptor.h"
#include "omnetpp/cvalue.h"

using namespace omnetpp::common;
using namespace omnetpp::internal;

namespace omnetpp {

const char **cWatchProxyDescriptor::getPropertyNames() const {
    static const char *empty[] = {nullptr};
    return targetDesc ? targetDesc->getPropertyNames() : empty;
}

const char *cWatchProxyDescriptor::getProperty(const char *propertyname) const {
    return targetDesc ? targetDesc->getProperty(propertyname) : nullptr;
}

int cWatchProxyDescriptor::getFieldCount() const {
    return targetDesc ? targetDesc->getFieldCount() : 0;
}

const char *cWatchProxyDescriptor::getFieldName(int field) const {
    return targetDesc ? targetDesc->getFieldName(field) : nullptr;
}

unsigned int cWatchProxyDescriptor::getFieldTypeFlags(int field) const {
    return targetDesc ? targetDesc->getFieldTypeFlags(field) : 0;
}

const char *cWatchProxyDescriptor::getFieldTypeString(int field) const {
    return targetDesc ? targetDesc->getFieldTypeString(field) : nullptr;
}

const char *cWatchProxyDescriptor::getFieldStructName(int field) const {
    return targetDesc ? targetDesc->getFieldStructName(field) : nullptr;
}

const char **cWatchProxyDescriptor::getFieldPropertyNames(int field) const {
    static const char *empty[] = {nullptr};
    return targetDesc ? targetDesc->getFieldPropertyNames(field) : empty;
}

const char *cWatchProxyDescriptor::getFieldProperty(int field, const char *propertyname) const {
    return targetDesc ? targetDesc->getFieldProperty(field, propertyname) : nullptr;
}

std::string cWatchProxyDescriptor::getValueAsString(any_ptr object) const {
    ASSERT(fromAnyPtr<cObject>(object) == watch);
    if (!targetDesc)
        return "nullptr";
    any_ptr ptr = watch->getValuePointer();
    if (targetDesc->extendsCObject()) {
        cObject *obj = fromAnyPtr<cObject>(ptr);
        std::string s = "-> " + obj->getClassAndFullName();
        std::string details = obj->str();
        if (!details.empty())
            s += " " + details;
        return s;
    }
    return targetDesc->getValueAsString(ptr);
}

void cWatchProxyDescriptor::setValueAsString(any_ptr object, const char *value) const {
    ASSERT(fromAnyPtr<cObject>(object) == watch);
    if (targetDesc) targetDesc->setValueAsString(watch->getValuePointer(), value);
}

std::string cWatchProxyDescriptor::getFieldValueAsString(any_ptr object, int field, int i) const {
    ASSERT(fromAnyPtr<cObject>(object) == watch);
    return targetDesc ? targetDesc->getFieldValueAsString(watch->getValuePointer(), field, i) : "n/a";
}

void cWatchProxyDescriptor::setFieldValueAsString(any_ptr object, int field, int i, const char *value) const {
    ASSERT(fromAnyPtr<cObject>(object) == watch);
    if (targetDesc) targetDesc->setFieldValueAsString(watch->getValuePointer(), field, i, value);
}

cValue cWatchProxyDescriptor::getFieldValue(any_ptr object, int field, int i) const {
    ASSERT(fromAnyPtr<cObject>(object) == watch);
    return targetDesc ? targetDesc->getFieldValue(watch->getValuePointer(), field, i) : cValue();
}

void cWatchProxyDescriptor::setFieldValue(any_ptr object, int field, int i, const cValue& value) const {
    ASSERT(fromAnyPtr<cObject>(object) == watch);
    if (targetDesc) targetDesc->setFieldValue(watch->getValuePointer(), field, i, value);
}

any_ptr cWatchProxyDescriptor::getFieldStructValuePointer(any_ptr object, int field, int i) const {
    ASSERT(fromAnyPtr<cObject>(object) == watch);
    return targetDesc ? targetDesc->getFieldStructValuePointer(watch->getValuePointer(), field, i) : any_ptr(nullptr);
}

void cWatchProxyDescriptor::setFieldStructValuePointer(any_ptr object, int field, int i, any_ptr ptr) const {
    ASSERT(fromAnyPtr<cObject>(object) == watch);
    if (targetDesc) targetDesc->setFieldStructValuePointer(watch->getValuePointer(), field, i, ptr);
}

int cWatchProxyDescriptor::getFieldArraySize(any_ptr object, int field) const {
    ASSERT(fromAnyPtr<cObject>(object) == static_cast<cObject *>(watch));
    return targetDesc ? targetDesc->getFieldArraySize(watch->getValuePointer(), field) : 0;
}

void cWatchProxyDescriptor::setFieldArraySize(any_ptr object, int field, int size) const {
    ASSERT(fromAnyPtr<cObject>(object) == static_cast<cObject *>(watch));
    if (targetDesc) targetDesc->setFieldArraySize(watch->getValuePointer(), field, size);
}

std::string cWatchProxyDescriptor::getFieldArrayIndexString(any_ptr object, int field, int arrayIndex) const {
    ASSERT(fromAnyPtr<cObject>(object) == static_cast<cObject *>(watch));
    return targetDesc ? targetDesc->getFieldArrayIndexString(watch->getValuePointer(), field, arrayIndex) : "";
}

// ----

/**
 * Internal. Wraps a cVisitor, in such a way that it prevents a single
 * cObject from being visited. Used to break reference loops when a
 * cWatch is pointed to its parent.
 */
class LoopCuttingVisitor : public cVisitor
{
  private:
    cVisitor *wrapped;
    cObject *skip;
  public:
    LoopCuttingVisitor(cVisitor *wrapped, cObject *skip)
      : wrapped(wrapped), skip(skip) { }

    virtual bool visit(cObject *obj) override {
        return obj == skip ? true : wrapped->visit(obj);
    }
};

cWatchBase::~cWatchBase()
{
    dropAndDelete(proxyDesc);
}

void cWatchBase::forEachChildOf(cObject *obj, cVisitor *visitor)
{
    LoopCuttingVisitor lcv(visitor, this);
    obj->forEachChild(&lcv);
}

// ----

std::string cWatchBase::str() const
{
    cClassDescriptor *desc = getDescriptor();
    return desc ? desc->getValueAsString(toAnyPtr(this)) : "";
}

}