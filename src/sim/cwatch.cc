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

/**
 * Internal helper for cWatchBase (for objects and object pointers).
 * Necessary only to translate the object argument of instance-related
 * methods from the cWatchBase (or subclass) pointer to a pointer to
 * the actually watched object.
 */
class cWatchProxyDescriptor : public cClassDescriptor {
  protected:
    cWatchBase *watch;

  protected:
    cClassDescriptor *getWatchedDescriptor() const {
    	//TODO this is quite costly, cache!
        any_ptr value = watch->getValuePointer();
        cClassDescriptor *desc;
        if (value.contains<cObject>()) {
            cObject *obj = fromAnyPtr<cObject>(value);
            if (obj == nullptr)
                return nullptr;
            desc = obj->getDescriptor();
        }
        else
            desc = cClassDescriptor::getDescriptorFor(value.typeName());
        return desc;
    }

  public:
    cWatchProxyDescriptor(cWatchBase *watch) : cClassDescriptor("cWatchBase"), watch(watch) {
    }

    virtual const char **getPropertyNames() const override {
        static const char *empty[] = {nullptr};
        auto d = getWatchedDescriptor();
        return d ? d->getPropertyNames() : empty;
    }

    virtual const char *getProperty(const char *propertyname) const override {
        auto d = getWatchedDescriptor();
        return d ? d->getProperty(propertyname) : nullptr;
    }

    virtual int getFieldCount() const override {
        auto d = getWatchedDescriptor();
        return d ? d->getFieldCount() : 0;
    }

    virtual const char *getFieldName(int field) const override {
        auto d = getWatchedDescriptor();
        return d ? d->getFieldName(field) : nullptr;
    }

    virtual unsigned int getFieldTypeFlags(int field) const override {
        auto d = getWatchedDescriptor();
        return d ? d->getFieldTypeFlags(field) : 0;
    }

    virtual const char *getFieldTypeString(int field) const override {
        auto d = getWatchedDescriptor();
        return d ? d->getFieldTypeString(field) : nullptr;
    }

    virtual const char *getFieldStructName(int field) const override {
        auto d = getWatchedDescriptor();
        return d ? d->getFieldStructName(field) : nullptr;
    }

    virtual const char **getFieldPropertyNames(int field) const override {
        static const char *empty[] = {nullptr};
        auto d = getWatchedDescriptor();
        return d ? d->getFieldPropertyNames(field) : empty;
    }

    virtual const char *getFieldProperty(int field, const char *propertyname) const override {
        auto d = getWatchedDescriptor();
        return d ? d->getFieldProperty(field, propertyname) : nullptr;
    }

    virtual std::string getFieldValueAsString(any_ptr object, int field, int i) const override {
        ASSERT(fromAnyPtr<cObject>(object) == watch);
        auto d = getWatchedDescriptor();
        return d ? d->getFieldValueAsString(watch->getValuePointer(), field, i) : "n/a";
    }

    virtual void setFieldValueAsString(any_ptr object, int field, int i, const char *value) const override {
        ASSERT(fromAnyPtr<cObject>(object) == watch);
        if (auto d = getWatchedDescriptor()) d->setFieldValueAsString(watch->getValuePointer(), field, i, value);
    }

    virtual cValue getFieldValue(any_ptr object, int field, int i) const override {
        ASSERT(fromAnyPtr<cObject>(object) == watch);
        auto d = getWatchedDescriptor(); return d ? d->getFieldValue(watch->getValuePointer(), field, i) : cValue();
    }

    virtual void setFieldValue(any_ptr object, int field, int i, const cValue& value) const override {
        ASSERT(fromAnyPtr<cObject>(object) == watch);
        if (auto d = getWatchedDescriptor()) d->setFieldValue(watch->getValuePointer(), field, i, value);
    }

    virtual any_ptr getFieldStructValuePointer(any_ptr object, int field, int i) const override {
        ASSERT(fromAnyPtr<cObject>(object) == watch);
        auto d = getWatchedDescriptor(); return d ? d->getFieldStructValuePointer(watch->getValuePointer(), field, i) : any_ptr(nullptr);
    }

    virtual void setFieldStructValuePointer(any_ptr object, int field, int i, any_ptr ptr) const override {
        ASSERT(fromAnyPtr<cObject>(object) == watch);
        if (auto d = getWatchedDescriptor()) d->setFieldStructValuePointer(watch->getValuePointer(), field, i, ptr);
    }

    virtual int getFieldArraySize(any_ptr object, int field) const override {
        ASSERT(fromAnyPtr<cObject>(object) == static_cast<cObject *>(watch));
        auto d = getWatchedDescriptor(); return d ? d->getFieldArraySize(watch->getValuePointer(), field) : 0;
    }

    virtual void setFieldArraySize(any_ptr object, int field, int size) const override {
        ASSERT(fromAnyPtr<cObject>(object) == static_cast<cObject *>(watch));
        if (auto d = getWatchedDescriptor()) d->setFieldArraySize(watch->getValuePointer(), field, size);
    }
};

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

// ----

cClassDescriptor *cWatchBase::getDescriptor() const
{
    if (!proxyDesc) {
        proxyDesc = new cWatchProxyDescriptor(const_cast<cWatchBase*>(this));
        const_cast<cWatchBase*>(this)->take(proxyDesc);
    }
    return proxyDesc;
}

cWatchBase::~cWatchBase()
{
    dropAndDelete(proxyDesc);
}

// ----

cWatch_cObject::cWatch_cObject(const char *name, const char *typeName, cObject& ref)
    : cWatchBase(name), r(ref), typeName(typeName)
{
}

void cWatch_cObject::forEachChild(cVisitor *visitor)
{
    LoopCuttingVisitor lcv(visitor, this);
    r.forEachChild(&lcv);
}

cWatch_cObjectPtr::cWatch_cObjectPtr(const char *name, const char *typeName, cObject *&ptr)
    : cWatchBase(name), rp(ptr), typeName(typeName)
{
}

void cWatch_cObjectPtr::forEachChild(cVisitor *visitor)
{
    LoopCuttingVisitor lcv(visitor, this);
    if (rp)
        rp->forEachChild(&lcv);
}

// ----

std::string cWatch_stdstring::str() const
{
    return opp_quotestr(r);
}

void cWatch_stdstring::assign(const char *s)
{
    if (s[0] == '"' && s[strlen(s)-1] == '"') {
        r = opp_parsequotedstr(s);
    }
    else {
        r = s;
    }
}

}  // namespace omnetpp

