//==========================================================================
//  CSTLWATCH.H - part of
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

#ifndef __OMNETPP_CSTLWATCH_H
#define __OMNETPP_CSTLWATCH_H

#include <vector>
#include <list>
#include <set>
#include <map>
#include <string>
#include <iostream>
#include <sstream>
#include "cownedobject.h"
#include "cwatch.h"
#include "stringutil.h"

namespace omnetpp {

//
// Internal class
//
class SIM_API cStlContainerWatcherBase : public cWatchBase
{
  private:
    mutable cClassDescriptor *desc = nullptr;
    // Note that this is a lie - this watcher class states that it itself is
    // the STL container class. But it's not, and if you tried to use the
    // descriptor registered for this false class name, to describe an actual
    // STL container object, bad things would happen. This is what makes adding
    // support for WATCHing (and describing) nested STL containers, because
    // the inner elements (still STL containers) would also be described using
    // this same descriptor, which is not correct. Another thing is that the
    // STL containers are templates, so the real descriptors for them would have
    // to be templates too, instantiated and registered on (compile-time) demand.
    std::string classname;
  public:
    cStlContainerWatcherBase(const char *name, const std::string& classname) : cWatchBase(name), classname(classname) {}
    virtual std::string str() const override;
    virtual bool supportsAssignment() const override {return false;}
    virtual const char *getClassName() const override {return classname.c_str();}
    // if the elements are pointers, the pointee type should be returned (no asterisk at the end)
    virtual const char *getElemTypeName() const = 0;
    virtual int size() const = 0;
    // NOTE: if the elements are cObject [subclass] pointers, this is not called, instead, cObject::str is used
    virtual std::string at(int i) const = 0;
    virtual any_ptr elementAt(int i) const {return any_ptr(nullptr);}
    virtual cClassDescriptor *getDescriptor() const override;
};


// --- type trait: is_std_pair<T> ---
template <class T> struct is_std_pair : std::false_type {};
template <class A, class B> struct is_std_pair<std::pair<A,B>> : std::true_type {};

// --- type trait: is_printable<T> --- true if operator<<(ostream&, T) exists ---
template <class T, class = void>
struct is_printable : std::false_type {};
template <class T>
struct is_printable<T, std::void_t<decltype(std::declval<std::ostream&>() << std::declval<const T&>())>> : std::true_type {};

template <class T>
void printValue(std::ostream& out, const T& value)
{
    if constexpr (std::is_pointer_v<T>) {
        if (value != nullptr) {
            if constexpr (is_printable<std::remove_pointer_t<T>>::value)
                out << *value;
            else
                out << (void*)value;
        }
        else
            out << "<nullptr>";
    }
    else {
        if constexpr (is_printable<T>::value)
            out << value;
        else
            out << "<not printable>";
    }
}


//
// Internal class
//
template<class T>
class cStdVectorWatcher : public cStlContainerWatcherBase
{
  protected:
    std::vector<T>& v;
  public:
    cStdVectorWatcher(const char *name, std::vector<T>& var) :
        cStlContainerWatcherBase(name, std::string("std::vector<")+opp_typename(typeid(T))+">"), v(var) {
    }
    virtual const char *getElemTypeName() const override {
        if constexpr (std::is_pointer_v<T>)
            return opp_typename(typeid(std::remove_pointer_t<T>));
        else
            return opp_typename(typeid(T));
    }
    virtual int size() const override {return v.size();}
    virtual std::string at(int i) const override {
        std::stringstream out;
        printValue(out, v[i]);
        return out.str();
    }
    virtual any_ptr elementAt(int i) const override {
        if constexpr (std::is_pointer_v<T>) {
            if constexpr (std::is_base_of_v<cObject,std::remove_pointer_t<T>>)
                // upcasting is necessary, as any_ptr can't do it internally
                return any_ptr((cObject *)v[i]);
            else
                return any_ptr(v[i]);
        }
        else {
            if constexpr (std::is_base_of_v<cObject,T>)
                // upcasting is necessary, as any_ptr can't do it internally
                return any_ptr((cObject *)&v[i]);
            else
                return any_ptr(&v[i]);
        }
    }
};

template <class T>
void createStdVectorWatcher(const char *varname, std::vector<T>& v)
{
    new cStdVectorWatcher<T>(varname, v);
}


// --- helper: reference to the "value" part of an iterator (pair->second for maps; *it otherwise) ---
template <class I>
decltype(auto) value_ref(const I& it) {
    if constexpr (is_std_pair<typename I::value_type>::value) {
        return (it->second);
    } else {
        return (*it);
    }
}


//
// Internal class
//
template <class T, typename I>
class cIteratorBasedContainerWatcherBase : public cStlContainerWatcherBase
{
    using cStlContainerWatcherBase::cStlContainerWatcherBase;

  protected:
    mutable I it;
    mutable int itPos = -1;

    void seekItTo(int i) const {
        // some STL containers (e.g. list, set, map) don't support random access iterator and iteration is slow,
        // so we have to use a trick, knowing that Qtenv will call this function with i=0, i=1, etc...
        if (i==0) {
            // always getting a fresh iterator for the first element, to defend a bit against iterator invalidation
            it=begin(); itPos=0;
        } else if (i==itPos) {
            return;
        } else if (i==itPos+1 && it!=end()) {
            ++it; ++itPos;
        } else {
            // always starting from the beginning if the element we're looking for is not the
            // current or the next one, again, to defend a bit against iterator invalidation
            it=begin();
            for (int k=0; k<i && it!=end(); k++) ++it;
            itPos=i;
        }
    }

    virtual I begin() const = 0;
    virtual I end() const = 0;

    virtual std::string atIt() const {
        // Even though this method is overridden in cStdMapWatcher,
        // the code here still has to be compilable in the context
        // of subclasses - just in case their overrides delegates
        // back to here, I suppose. The `value_ref` usage here is
        // only necessary to make that the case.
        std::stringstream out;
        printValue(out, value_ref(it));
        return out.str();
    }

    std::string at(int i) const override {
        seekItTo(i);
        if (it==end()) {
            return std::string("out of bounds");
        }
        return atIt();
    }

    any_ptr elementAt(int i) const override {
        seekItTo(i);
        if (it==end()) {
            return any_ptr(nullptr);
        }
        if constexpr (std::is_pointer_v<T>) {
            if constexpr (std::is_base_of_v<cObject,std::remove_pointer_t<T>>)
                // upcasting is necessary, as any_ptr can't do it internally
                return any_ptr((cObject *)value_ref(it));
            else
                return any_ptr(value_ref(it));
        } else {
            if constexpr (std::is_base_of_v<cObject,T>)
                // upcasting is necessary, as any_ptr can't do it internally
                return any_ptr((cObject *)&value_ref(it));
            else
                return any_ptr(&value_ref(it));
        }
    }

  public:
    const char *getElemTypeName() const override {
        if constexpr (std::is_pointer_v<T>) {
            return opp_typename(typeid(std::remove_pointer_t<T>));
        }
        else {
            return opp_typename(typeid(T));
        }
    }
};

//
// Internal class
//
// Note that I is not a real template parameter with an added degree of freedom
// (it should always be left as the default value/type) - it's just an alias for
// the iterator type of the container T, to make the code more concise.
template<class T, typename I = typename std::list<T>::iterator>
class cStdListWatcher : public cIteratorBasedContainerWatcherBase<T, I>
{
  protected:
    std::list<T>& v;

    int size() const override {return v.size();}
    I begin() const override {return v.begin();}
    I end() const override {return v.end();}

  public:
    cStdListWatcher(const char *name, std::list<T>& var) :
        cIteratorBasedContainerWatcherBase<T, I>(name, std::string("std::list<")+opp_typename(typeid(T))+">"),
        v(var)
    { }
};

template <class T>
void createStdListWatcher(const char *varname, std::list<T>& v)
{
    new cStdListWatcher<T>(varname, v);
}


//
// Internal class
//
// Note that I is not a real template parameter with an added degree of freedom
// (it should always be left as the default value/type) - it's just an alias for
// the iterator type of the container T, to make the code more concise.
template<class T, typename I = typename std::set<T>::iterator>
class cStdSetWatcher : public cIteratorBasedContainerWatcherBase<T, I>
{
  protected:
    std::set<T>& v;

    int size() const override {return v.size();}
    I begin() const override {return v.begin();}
    I end() const override {return v.end();}

  public:
    cStdSetWatcher(const char *name, std::set<T>& var) :
        cIteratorBasedContainerWatcherBase<T, I>(name, std::string("std::set<")+opp_typename(typeid(T))+">"),
        v(var)
    { }
};

template <class T>
void createStdSetWatcher(const char *varname, std::set<T>& v)
{
    new cStdSetWatcher<T>(varname, v);
}


//
// Internal class
//
// Note that I is not a real template parameter with an added degree of freedom
// (it should always be left as the default value/type) - it's just an alias for
// the iterator type of the container T, to make the code more concise.
template<class KeyT, class ValueT, class CmpT, class I = typename std::map<KeyT,ValueT,CmpT>::iterator>
class cStdMapWatcher : public cIteratorBasedContainerWatcherBase<ValueT, I>
{
  protected:
    std::map<KeyT,ValueT,CmpT>& m;

    int size() const override {return m.size();}
    I begin() const override {return m.begin();}
    I end() const override {return m.end();}

  public:
    cStdMapWatcher(const char *name, std::map<KeyT,ValueT,CmpT>& var) :
        cIteratorBasedContainerWatcherBase<ValueT, I>(name, std::string("std::map<")+opp_typename(typeid(KeyT))+","+opp_typename(typeid(ValueT))+">"),
        m(var)
    { }

    virtual std::string atIt() const {
        std::stringstream out;
        out << this->it->first << " => ";
        printValue(out, this->it->second);
        return out.str();
    }
};

template <class KeyT, class ValueT, class CmpT>
void createStdMapWatcher(const char *varname, std::map<KeyT,ValueT,CmpT>& m)
{
    new cStdMapWatcher<KeyT,ValueT,CmpT>(varname, m);
}


/**
 * @ingroup WatchMacros
 * @{
 */

/**
 * @brief Makes std::vectors inspectable in Qtenv. See also WATCH_PTRVECTOR().
 *
 * @hideinitializer
 */
#define WATCH_VECTOR(variable)     omnetpp::createStdVectorWatcher(#variable,(variable))

/**
 * @brief Makes std::vectors storing pointers inspectable in Qtenv. See also WATCH_VECTOR().
 *
 * @hideinitializer
 */
#define WATCH_PTRVECTOR(variable)  omnetpp::createStdVectorWatcher(#variable,(variable))

/**
 * @brief Makes std::lists inspectable in Qtenv. See also WATCH_PTRLIST().
 *
 * @hideinitializer
 */
#define WATCH_LIST(variable)       omnetpp::createStdListWatcher(#variable,(variable))

/**
 * @brief Makes std::lists storing pointers inspectable in Qtenv. See also WATCH_LIST().
 *
 * @hideinitializer
 */
#define WATCH_PTRLIST(variable)    omnetpp::createStdListWatcher(#variable,(variable))

/**
 * @brief Makes std::sets inspectable in Qtenv. See also WATCH_PTRSET().
 *
 * @hideinitializer
 */
#define WATCH_SET(variable)        omnetpp::createStdSetWatcher(#variable,(variable))

/**
 * @brief Makes std::sets storing pointers inspectable in Qtenv. See also WATCH_SET().
 *
 * @hideinitializer
 */
#define WATCH_PTRSET(variable)     omnetpp::createStdSetWatcher(#variable,(variable))

/**
 * @brief Makes std::maps inspectable in Qtenv. See also WATCH_PTRMAP().
 *
 * @hideinitializer
 */
#define WATCH_MAP(m)               omnetpp::createStdMapWatcher(#m,(m))

/**
 * @brief Makes std::maps storing pointers inspectable in Qtenv. See also WATCH_MAP().
 *
 * @hideinitializer
 */
#define WATCH_PTRMAP(m)            omnetpp::createStdMapWatcher(#m,(m))

/** @} */

}  // namespace omnetpp


#endif

