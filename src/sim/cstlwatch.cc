//==========================================================================
//  CSTLWATCH.H - part of
//                     OMNeT++/OMNEST
//            Discrete System Simulation in C++
//
//
//  WATCH_VECTOR, WATCH_MAP etc macros
//
//==========================================================================

/*--------------------------------------------------------------*
  Copyright (C) 1992-2017 Andras Varga
  Copyright (C) 2006-2017 OpenSim Ltd.

  This file is distributed WITHOUT ANY WARRANTY. See the file
  `license' for details on this and other legal matters.
*--------------------------------------------------------------*/

#include <cstdio>
#include "omnetpp/cstlwatch.h"
#include "omnetpp/cclassdescriptor.h"
#include "omnetpp/globals.h"
#include "omnetpp/checkandcast.h"

using namespace omnetpp::internal;

namespace omnetpp {

//
// Internal
//
class SIM_API cStlContainerWatcherDescriptor : public cClassDescriptor  // noncopyable
{
  private:
    std::string vectorTypeName;  // type name of the inspected type, e.g. "std::vector<foo::Bar>"
    std::string elementTypeName;  // type name of vector elements, e.g. "foo::Bar"
    bool elementsAreCompound;
    bool elementsAreCObjects;
  public:
    cStlContainerWatcherDescriptor(const char *vecTypeName, const char *elemTypeName, bool elementsAreCompound, bool elementsAreCObjects);
    virtual ~cStlContainerWatcherDescriptor() {}

    virtual const char **getPropertyNames() const override;
    virtual const char *getProperty(const char *propertyname) const override;
    virtual int getFieldCount() const override;
    virtual const char *getFieldName(int field) const override;
    virtual unsigned int getFieldTypeFlags(int field) const override;
    virtual const char *getFieldTypeString(int field) const override;
    virtual const char **getFieldPropertyNames(int field) const override;
    virtual const char *getFieldProperty(int field, const char *propertyname) const override;
    virtual int getFieldArraySize(any_ptr object, int field) const override;
    virtual void setFieldArraySize(any_ptr object, int field, int size) const override;

    virtual std::string getFieldValueAsString(any_ptr object, int field, int i) const override;
    virtual void setFieldValueAsString(any_ptr object, int field, int i, const char *value) const override;
    virtual cValue getFieldValue(any_ptr object, int field, int i) const override;
    virtual void setFieldValue(any_ptr object, int field, int i, const cValue& value) const override;

    virtual const char *getFieldStructName(int field) const override;
    virtual any_ptr getFieldStructValuePointer(any_ptr object, int field, int i) const override;
    virtual void setFieldStructValuePointer(any_ptr object, int field, int i, any_ptr ptr) const override;
};

cStlContainerWatcherDescriptor::cStlContainerWatcherDescriptor(const char *vecType, const char *elemType, bool elementsAreCompound, bool elementsAreCObjects) :
    cClassDescriptor(vecType, "omnetpp::cWatchBase"), vectorTypeName(vecType), elementTypeName(elemType), elementsAreCompound(elementsAreCompound), elementsAreCObjects(elementsAreCObjects)
{
}

const char **cStlContainerWatcherDescriptor::getPropertyNames() const
{
    return getBaseClassDescriptor()->getPropertyNames();
}

const char *cStlContainerWatcherDescriptor::getProperty(const char *propertyname) const
{
    return getBaseClassDescriptor()->getProperty(propertyname);
}

int cStlContainerWatcherDescriptor::getFieldCount() const
{
    return 1;
}

unsigned int cStlContainerWatcherDescriptor::getFieldTypeFlags(int field) const
{
    return FD_ISARRAY | (elementsAreCompound ? FD_ISCOMPOUND : 0) | (elementsAreCObjects ? FD_ISCOBJECT : 0);
}

const char *cStlContainerWatcherDescriptor::getFieldName(int field) const
{
    return "elements";
}

const char *cStlContainerWatcherDescriptor::getFieldTypeString(int field) const
{
    return elementTypeName.c_str();
}

const char **cStlContainerWatcherDescriptor::getFieldPropertyNames(int field) const
{
    static const char **names = { nullptr };
    return names;
}

const char *cStlContainerWatcherDescriptor::getFieldProperty(int field, const char *propertyname) const
{
    return nullptr;
}

int cStlContainerWatcherDescriptor::getFieldArraySize(any_ptr object, int field) const
{
    cStlContainerWatcherBase *pp = check_and_cast<cStlContainerWatcherBase*>(fromAnyPtr<cObject>(object));
    return pp->size();
}

void cStlContainerWatcherDescriptor::setFieldArraySize(any_ptr object, int field, int size) const
{
    throw cRuntimeError("Cannot set size of array field");  // not supported
}

std::string cStlContainerWatcherDescriptor::getFieldValueAsString(any_ptr object, int field, int i) const
{
    cStlContainerWatcherBase *pp = check_and_cast<cStlContainerWatcherBase*>(fromAnyPtr<cObject>(object));
    return pp->at(i);
}

void cStlContainerWatcherDescriptor::setFieldValueAsString(any_ptr object, int field, int i, const char *value) const
{
    throw cRuntimeError("Cannot set field value");  // not supported
}

cValue cStlContainerWatcherDescriptor::getFieldValue(any_ptr object, int field, int i) const
{
    throw cRuntimeError("Cannot return field value as cValue");  // not supported
}

void cStlContainerWatcherDescriptor::setFieldValue(any_ptr object, int field, int i, const cValue& value) const
{
    throw cRuntimeError("Cannot set field value");  // not supported
}

const char *cStlContainerWatcherDescriptor::getFieldStructName(int field) const
{
    return elementTypeName.c_str();
}

any_ptr cStlContainerWatcherDescriptor::getFieldStructValuePointer(any_ptr object, int field, int i) const
{
    cStlContainerWatcherBase *pp = check_and_cast<cStlContainerWatcherBase*>(fromAnyPtr<cObject>(object));
    return pp->elementAt(i);
}

void cStlContainerWatcherDescriptor::setFieldStructValuePointer(any_ptr object, int field, int i, any_ptr ptr) const
{
    throw cRuntimeError("Cannot set field value");  // not supported
}

//--------------------------------

std::string cStlContainerWatcherBase::str() const
{
    if (size() == 0)
        return std::string("empty");
    std::stringstream out;
    out << "size=" << size();
    return out.str();
}

cClassDescriptor *cStlContainerWatcherBase::getDescriptor() const
{
    if (!desc) {
        // try to find existing descriptor for this particular type (e.g. "std::vector<double>");
        // if there isn't, create and register a new one
        desc = (cClassDescriptor *)classDescriptors.getInstance()->lookup(getClassName());
        cClassDescriptor *elemDesc = cClassDescriptor::getDescriptorFor(getElemTypeName());
        if (!desc) {
            // FIXME: This wrongly passes false for `elementsAreCObjects` if there is no
            // descriptor for the element type directly, only for a superclass of it.
            // The issue is that there's no way to lookup base classes "at runtime"
            // (after template instantiation), and without an element instance...
            desc = new cStlContainerWatcherDescriptor(getClassName(), getElemTypeName(), elemDesc != nullptr, elemDesc && elemDesc->extendsCObject());
            classDescriptors.getInstance()->add(desc);
        }
    }
    return desc;
}

}  // namespace omnetpp

