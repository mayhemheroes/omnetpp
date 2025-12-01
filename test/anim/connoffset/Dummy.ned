//
// Simple module for testing connection offset rendering
//
simple Dummy
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
}
