//
// Network for testing connection offset rendering
//
network ConnOffsetTest
{
    submodules:
        node1: Dummy {
            @display("p=100,100");
        }
        node2: Dummy {
            @display("p=300,100");
        }
        bus: DummyBus {
            @display("p=400,400;b=700,50");
        }
        tap1: Dummy {
            @display("p=50,600");
        }
        tap2: Dummy {
            @display("p=200,600;b=10,100");
        }
        tap3: Dummy {
            @display("p=300,600");
        }
        tap4: Dummy {
            @display("p=500,600;b=100,10");
        }
        tap5: Dummy {
            @display("p=900,600");
        }
        compound1: Compound {
            @display("p=100,200;is=s");
        }
        compound2: Compound {
            @display("p=300,300;is=l");
        }

    connections:
        // Connect individual inout gates between node1 and node2
        node1.io1 <--> node2.io1;
        node1.io2 <--> node2.io2;
        node1.io3 <--> node2.io3;

        // Connect individual one-way gates
        node1.out1 --> node2.in1;
        node2.out1 --> node1.in1;
        node1.out2 --> node2.in2;
        node2.out2 --> node1.in2;

        // Connect inout gate arrays
        for i=0..2 {
            node1.ioArray++ <--> node2.ioArray++;
        }

        // Connect one-way gate arrays
        for i=0..1 {
            node1.outArray++ --> node2.inArray++;
            node2.outArray++ --> node1.inArray++;
        }

        // Connect individual inout gates between compound1 and compound2
        compound1.io1 <--> compound2.io1;
        compound1.io2 <--> compound2.io2;
        compound1.io3 <--> compound2.io3;

        // Connect individual one-way gates
        compound1.out1 --> compound2.in1;
        compound2.out1 --> compound1.in1;
        compound1.out2 --> compound2.in2;
        compound2.out2 --> compound1.in2;

        // Connect inout gate arrays
        for i=0..2 {
            compound1.ioArray++ <--> compound2.ioArray++;
        }

        // Connect one-way gate arrays
        for i=0..1 {
            compound1.outArray++ --> compound2.inArray++;
            compound2.outArray++ --> compound1.inArray++;
        }

        // Connect individual inout gates between bus and tap1
        bus.io1++ <--> tap1.io1;
        bus.io2++ <--> tap1.io2;
        bus.io3++ <--> tap1.io3;

        // Connect individual one-way gates
        bus.out1++ --> tap1.in1;
        tap1.out1 --> bus.in1++;
        bus.out2++ --> tap1.in2;
        tap1.out2 --> bus.in2++;

        // Connect inout gate arrays
        for i=0..2 {
            bus.ioArray++ <--> tap1.ioArray++;
        }

        // Connect one-way gate arrays
        for i=0..1 {
            bus.outArray++ --> tap1.inArray++;
            tap1.outArray++ --> bus.inArray++;
        }

        // Connect individual inout gates between bus and tap2
        bus.io1++ <--> tap2.io1;
        bus.io2++ <--> tap2.io2;
        bus.io3++ <--> tap2.io3;

        // Connect individual one-way gates
        bus.out1++ --> tap2.in1;
        tap2.out1 --> bus.in1++;
        bus.out2++ --> tap2.in2;
        tap2.out2 --> bus.in2++;

        // Connect inout gate arrays
        for i=0..2 {
            bus.ioArray++ <--> tap2.ioArray++;
        }

        // Connect one-way gate arrays
        for i=0..1 {
            bus.outArray++ --> tap2.inArray++;
            tap2.outArray++ --> bus.inArray++;
        }

        // Connect individual inout gates between bus and tap3
        bus.io1++ <--> tap3.io1;
        bus.io2++ <--> tap3.io2;
        bus.io3++ <--> tap3.io3;

        // Connect individual one-way gates
        bus.out1++ --> tap3.in1;
        tap3.out1 --> bus.in1++;
        bus.out2++ --> tap3.in2;
        tap3.out2 --> bus.in2++;

        // Connect inout gate arrays
        for i=0..2 {
            bus.ioArray++ <--> tap3.ioArray++;
        }

        // Connect one-way gate arrays
        for i=0..1 {
            bus.outArray++ --> tap3.inArray++;
            tap3.outArray++ --> bus.inArray++;
        }

        // Connect individual inout gates between bus and tap4
        bus.io1++ <--> tap4.io1;
        bus.io2++ <--> tap4.io2;
        bus.io3++ <--> tap4.io3;

        // Connect individual one-way gates
        bus.out1++ --> tap4.in1;
        tap4.out1 --> bus.in1++;
        bus.out2++ --> tap4.in2;
        tap4.out2 --> bus.in2++;

        // Connect inout gate arrays
        for i=0..2 {
            bus.ioArray++ <--> tap4.ioArray++;
        }

        // Connect one-way gate arrays
        for i=0..1 {
            bus.outArray++ --> tap4.inArray++;
            tap4.outArray++ --> bus.inArray++;
        }

        // Connect individual inout gates between bus and tap5
        bus.io1++ <--> tap5.io1;
        bus.io2++ <--> tap5.io2;
        bus.io3++ <--> tap5.io3;

        // Connect individual one-way gates
        bus.out1++ --> tap5.in1;
        tap5.out1 --> bus.in1++;
        bus.out2++ --> tap5.in2;
        tap5.out2 --> bus.in2++;

        // Connect inout gate arrays
        for i=0..2 {
            bus.ioArray++ <--> tap5.ioArray[i];
        }

        // Connect one-way gate arrays
        for i=0..1 {
            bus.outArray++ --> tap5.inArray[i];
            tap5.outArray[i] --> bus.inArray++;
        }
}
