//
// Compound module for testing connection offset rendering with hierarchical connections
//
module Compound
{
    gates:
        // Individual inout gates
        inout io1;
        inout io2;
        inout io3;

        // Individual one-way gates (2 pairs)
        input in1;
        output out1;
        input in2;
        output out2;

        // Inout gate array
        inout ioArray[3];

        // One-way gate arrays
        input inArray[2];
        output outArray[2];

    submodules:
        dummy: Dummy;

    connections:
        // Connect individual inout gates
        io1 <--> dummy.io1;
        io2 <--> dummy.io2;
        io3 <--> dummy.io3;

        // Connect individual one-way gates
        in1 --> dummy.in1;
        dummy.out1 --> out1;
        in2 --> { @display("m=e"); } --> dummy.in2;
        dummy.out2 --> { @display("m=e"); } --> out2;

        // Connect inout gate arrays
        for i=0..2 {
            ioArray[i] <--> { @display("m=s"); } <--> dummy.ioArray[i];
        }

        // Connect one-way gate arrays
        for i=0..1 {
            inArray[i] --> dummy.inArray[i];
            dummy.outArray[i] --> outArray[i];
        }
}
