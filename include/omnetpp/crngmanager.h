//==========================================================================
//   CRNGMANAGER.H  - part of
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

#ifndef __OMNETPP_CRNGMANAGER_H
#define __OMNETPP_CRNGMANAGER_H

#include "cownedobject.h"

namespace omnetpp {

class cConfiguration;
class cConfigurationEx;
class cRNG;

/**
 * @brief Abstract interface for managing random number generators (RNGs)
 * for simulation components (modules and channels).
 *
 * The RNG manager is responsible for setting up and providing access to
 * RNG instances used during the simulation. It is consulted by cComponent's
 * getRNG(k) to obtain the actual cRNG instance for a given local RNG index.
 * The interface allows for RNG instances to be shared among components.
 *
 * The RNG manager is pluggable: a custom implementation can be installed
 * into cSimulation via setRngManager(). The default implementation is
 * cRngManager.
 *
 * @see cRngManager, cRNG, cComponent::getRNG()
 * @ingroup SimSupport
 */
class SIM_API cIRngManager : public cNoncopyableOwnedObject
{
  public:
    /** @name Constructor, destructor. */
    //@{
    /**
     * Constructor.
     */
    cIRngManager() {}

    /**
     * Destructor.
     */
    virtual ~cIRngManager();
    //@}

    /** @name Setting up and accessing RNGs. */
    //@{
    /**
     * Initializes the RNG manager from the given configuration. Called during
     * simulation setup, before the network is built.
     */
    virtual void configure(cConfiguration *cfg) = 0;

    /**
     * Sets up RNG access for the given component. Called during component
     * initialization.
     */
    virtual void configureRngs(cComponent *component) = 0;

    /**
     * Returns the number of RNGs available for the given component.
     */
    virtual int getNumRngs(const cComponent *component) const = 0;

    /**
     * Returns the RNG instance for the given component and local RNG index k.
     */
    virtual cRNG *getRng(const cComponent *component, int k) = 0;

    /**
     * Returns a hash value computed from the usage state of all RNGs
     * (e.g. the number of random numbers drawn from each). Intended for
     * use as a fingerprint ingredient.
     */
    virtual uint32_t getHash() const = 0;
    //@}

    /** @name Access to the global RNGs. */
    //@{

    /** Returns the total number of global RNG instances. */
    virtual int getTotalNumRngs() const = 0;

    /**
     * Returns the global RNG instance with the given index (0-based).
     * Throws an error if rngId is out of range.
     */
    virtual cRNG *getGlobalRng(int rngId) = 0;
    //@}
};


/**
 * @brief The default implementation of cIRngManager.
 *
 * cRngManager implements a two-level RNG access scheme. It creates a set of
 * global (physical) RNG instances at simulation setup time, and each
 * component (module or channel) accesses them through a configurable
 * local-to-global index mapping. This mapping enables techniques such as
 * variance reduction without requiring changes to model code.
 *
 * Relevant configuration options:
 *  - num-rngs: The number of global (physical) RNG instances to create.
 *  - rng-class: The C++ class of the RNG to use (default: cMersenneTwister).
 *  - seed-set: Selects the set of automatic seeds (default: \${runnumber}).
 *  - <module-path>.rng-N = M: Maps local RNG index N to global RNG index M
 *    for matching modules/channels. The value may be an expression.
 *  - seed-K-mt, seed-K-lcg32: Manual seed for global RNG K.
 *
 * @see cIRngManager, cRNG, cComponent::getRNG()
 * @ingroup SimSupport
 */
class SIM_API cRngManager : public cIRngManager
{
  private:
    cConfigurationEx *cfg = nullptr;
    int numRNGs = 0;
    cRNG **rngs = nullptr;

  public:
    /** Constructor */
    cRngManager() {}

    /** Destructor */
    virtual ~cRngManager();

    /** @name Redefined cIRngManager methods. */
    //@{
    virtual void configure(cConfiguration *cfg) override;
    virtual void configureRngs(cComponent *component) override;
    virtual int getNumRngs(const cComponent *component) const override;
    virtual cRNG *getRng(const cComponent *component, int k) override;
    virtual uint32_t getHash() const  override;
    virtual int getTotalNumRngs() const override;
    virtual cRNG *getGlobalRng(int rngId) override;
    //@}
};

}  // namespace omnetpp

#endif


