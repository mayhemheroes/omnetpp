#include <omnetpp.h>

using namespace omnetpp;

class Dummy : public cSimpleModule
{
  protected:
    virtual void initialize() override;
    virtual void handleMessage(cMessage *msg) override;
};

Define_Module(Dummy);

void Dummy::initialize()
{
    // Nothing to do
}

void Dummy::handleMessage(cMessage *msg)
{
    // Nothing to do
    delete msg;
}
