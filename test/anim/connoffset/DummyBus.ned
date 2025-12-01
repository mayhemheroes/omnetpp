//
// Simple module for testing connection offset rendering (bus variant with vector gates)
//
simple DummyBus
{
    gates:
        // Individual inout gates as vectors
        inout io1[4];
        inout io2[4];
        inout io3[4];

        // Individual one-way gates as vectors (2 pairs)
        input in1[4];
        output out1[4];
        input in2[4];
        output out2[4];

        // Inout gate array (now 2D: [4 taps][3 gates each])
        inout ioArray[];

        // One-way gate arrays (now 2D: [4 taps][2 gates each])
        input inArray[];
        output outArray[];
}
