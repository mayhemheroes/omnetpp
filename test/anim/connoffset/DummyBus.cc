#include <omnetpp.h>

using namespace omnetpp;

class DummyBus : public cSimpleModule
{
  protected:
    virtual void initialize() override;
    virtual void handleMessage(cMessage *msg) override;
};

Define_Module(DummyBus);

void DummyBus::initialize()
{
    // Nothing to do
}

void DummyBus::handleMessage(cMessage *msg)
{
    // Nothing to do
    delete msg;
}
