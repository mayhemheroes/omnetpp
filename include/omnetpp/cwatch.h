//==========================================================================
//  CWATCH.H - part of
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

#ifndef __OMNETPP_CWATCH_H
#define __OMNETPP_CWATCH_H

#include <iostream>
#include <sstream>
#include <functional>
#include "cownedobject.h"
#include "cclassdescriptor.h"
#include "cdynamicdescriptor.h"

namespace omnetpp {

class cWatchBase;

/**
 * Internal helper for cWatchBase (for objects and object pointers).
 * Necessary only to translate the object argument of instance-related
 * methods from the cWatchBase (or subclass) pointer to a pointer to
 * the actually watched object.
 */
class cWatchProxyDescriptor : public cClassDescriptor {
  protected:
    cWatchBase *watch;
    cClassDescriptor *targetDesc;

  public:
    cWatchProxyDescriptor(cWatchBase *watch, cClassDescriptor *targetDesc) :
      cClassDescriptor(targetDesc ? targetDesc->getName() : "none"),
      watch(watch), targetDesc(targetDesc) { }

    void setTargetDescriptor(cClassDescriptor *desc) { targetDesc = desc; setName(desc ? desc->getName() : "none"); }

    virtual const char **getPropertyNames() const override;
    virtual const char *getProperty(const char *propertyname) const override;
    virtual int getFieldCount() const override;
    virtual const char *getFieldName(int field) const override;
    virtual unsigned int getFieldTypeFlags(int field) const override;
    virtual const char *getFieldTypeString(int field) const override;
    virtual const char *getFieldStructName(int field) const override;
    virtual const char **getFieldPropertyNames(int field) const override;
    virtual const char *getFieldProperty(int field, const char *propertyname) const override;
    virtual std::string getValueAsString(any_ptr object) const override;
    virtual void setValueAsString(any_ptr object, const char *value) const override;
    virtual std::string getFieldValueAsString(any_ptr object, int field, int i) const override;
    virtual void setFieldValueAsString(any_ptr object, int field, int i, const char *value) const override;
    virtual cValue getFieldValue(any_ptr object, int field, int i) const override;
    virtual void setFieldValue(any_ptr object, int field, int i, const cValue& value) const override;
    virtual any_ptr getFieldStructValuePointer(any_ptr object, int field, int i) const override;
    virtual void setFieldStructValuePointer(any_ptr object, int field, int i, any_ptr ptr) const override;
    virtual int getFieldArraySize(any_ptr object, int field) const override;
    virtual void setFieldArraySize(any_ptr object, int field, int size) const override;
    virtual std::string getFieldArrayIndexString(any_ptr object, int field, int arrayIndex) const override;
};

/**
 * @brief Helper class to make primitive types and non-cOwnedObject objects
 * inspectable in Qtenv. To be used only via the WATCH, WATCH_PTR,
 * WATCH_OBJ, WATCH_VECTOR etc macros.
 *
 * @ingroup Internals
 */
class SIM_API cWatchBase : public cNoncopyableOwnedObject
{
  protected:
    mutable cWatchProxyDescriptor *proxyDesc = nullptr;

  protected:
    void forEachChildOf(cObject *obj, cVisitor *visitor);

  public:
    /** @name Constructors, destructor, assignment */
    //@{
    /**
     * Initialize the shell to hold the given variable.
     */
    cWatchBase(const char *name) : cNoncopyableOwnedObject(name) {}
    ~cWatchBase();
    //@}

    virtual std::string str() const override;

    /** @name New methods */
    //@{
    /**
     * Returns a pointer to the watched variable or object as an any_ptr.
     */
    virtual any_ptr getValuePointer() const = 0;
    //@}
};


/**
 * @brief Template Watch class that delegates to the type's cClassDescriptor
 * for string conversion and inspection.
 * @ingroup Internals
 */
template<typename T>
class cWatch : public cWatchBase
{
  private:
    const T& r;

  public:
    cWatch(const char *name, const T& x) : cWatchBase(name), r(x) {}
    virtual const char *getClassName() const override {return getDescriptor()->getFullName();}

    virtual void forEachChild(cVisitor *visitor) override {
        if constexpr (std::is_base_of_v<cObject, T>)
            forEachChildOf(const_cast<T*>(&r), visitor);
    }

    virtual any_ptr getValuePointer() const override {
        if constexpr (std::is_base_of_v<cObject, T>)
            return toAnyPtr(&r);
        else
            return any_ptr(&r);
    }

    virtual cClassDescriptor *getDescriptor() const override {
        if (proxyDesc == nullptr) {
            cClassDescriptor *targetDesc;
            if constexpr (std::is_base_of_v<cObject, T>)
                targetDesc = r.getDescriptor();
            else
                targetDesc = omnetpp::ensureDescriptor<T>();
            auto *nonconst_this = const_cast<cWatch*>(this);
            proxyDesc = new cWatchProxyDescriptor(nonconst_this, targetDesc);
            nonconst_this->take(proxyDesc);
        }
        return proxyDesc;
    }
};

/**
 * @brief Template Watch class that delegates to the type's cClassDescriptor
 * for string conversion and inspection.
 * @ingroup Internals
 */
template<typename T>
class cPointerWatch : public cWatchBase
{
  private:
    T *& p;
    std::string declTypeName;

  public:
    cPointerWatch(const char *name, T *& x) : cWatchBase(name), p(x),
        declTypeName(std::string(opp_typename(typeid(T))) + " *") {}

    virtual const char *getClassName() const override {return declTypeName.c_str();}

    virtual void forEachChild(cVisitor *visitor) override {
        if constexpr (std::is_base_of_v<cObject, T>)
            if (p) forEachChildOf(p, visitor);
    }

    virtual any_ptr getValuePointer() const override {
        if constexpr (std::is_base_of_v<cObject, T>)
            return toAnyPtr(p);
        else
            return any_ptr(p);
    }

    virtual cClassDescriptor *getDescriptor() const override {
        if (proxyDesc == nullptr) {
            cClassDescriptor *targetDesc;
            if constexpr (std::is_base_of_v<cObject, T>)
                targetDesc = p ? p->getDescriptor() : nullptr;
            else
                targetDesc = omnetpp::ensureDescriptor<T>();
            auto *nonconst_this = const_cast<cPointerWatch*>(this);
            proxyDesc = new cWatchProxyDescriptor(nonconst_this, targetDesc);
            nonconst_this->take(proxyDesc);
        }
        else if constexpr (std::is_base_of_v<cObject, T>) {
            // watched pointer may now point to a different object, update descriptor
            proxyDesc->setTargetDescriptor(p ? p->getDescriptor() : nullptr);
        }
        return proxyDesc;
    }
};


template<typename T>
inline cWatchBase *createWatch(const char *varname, T& d) {
    return new cWatch<T>(varname, d);
}

template<typename T>
inline cWatchBase *createWatch(const char *varname, T *& p) {
    return new cPointerWatch<T>(varname, p);
}

/**
 * @ingroup WatchMacros
 * @{
 */

/**
 * @brief Makes variables inspectable in Qtenv. String representation is
 * produced using str() for cObject descendants, and the stream write operator
 * (operator<<) for other types. For compound types, fields are queried using
 * the associated class descriptor of the type (see cClassDescriptor).
 *
 * @hideinitializer
 */
#define WATCH(variable)  omnetpp::createWatch(#variable,(variable))

// Obsolete aliases to WATCH()
#define WATCH_RW(variable)         WATCH(variable)
#define WATCH_OBJ(variable)        WATCH(variable)
#define WATCH_PTR(variable)        WATCH(variable)
#define WATCH_VECTOR(variable)     WATCH(variable)
#define WATCH_PTRVECTOR(variable)  WATCH(variable)
#define WATCH_LIST(variable)       WATCH(variable)
#define WATCH_PTRLIST(variable)    WATCH(variable)
#define WATCH_SET(variable)        WATCH(variable)
#define WATCH_PTRSET(variable)     WATCH(variable)
#define WATCH_MAP(variable)        WATCH(variable)
#define WATCH_PTRMAP(variable)     WATCH(variable)

/**
 * @brief Makes the result of a formula or calculation inspectable in Qtenv
 * without requiring a separate variable. The expression is evaluated as often
 * as needed, providing real-time monitoring of derived metrics. Unlike WATCH
 * which monitors single variables, WATCH_EXPR can display the result of
 * operations combining multiple variables, function calls, or any valid
 * expression.
 *
 * The macro works by creating a lambda function. Note that local variables will
 * be captured by value (i.e. their current values will be used.) See also
 * WATCH_LAMBDA() which gives you more flexibility.
 *
 * Example: WATCH_EXPR("totalPks", numTransmitted + queue.length() + numDropped)
 *
 * @hideinitializer
 */
#define WATCH_EXPR(name, expression)  //TODO omnetpp::createComputedExpressionWatch(name, std::function([=]() {return (expression);}))

/**
 * @brief Makes the result of a lambda function inspectable in Qtenv. This is a
 * more flexible (but also more verbose) version of WATCH_EXPR().
 *
 * Example: WATCH_LAMBDA("totalPks", [this]() { return numTransmitted + queue.length() + numDropped; })
 *
 * @hideinitializer
 */
#define WATCH_LAMBDA(name, lambdaFunction) //TODO omnetpp::createComputedExpressionWatch(name, std::function(lambdaFunction))

/** @} */

}  // namespace omnetpp


#endif


