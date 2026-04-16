// Tests that arrowcoords() receives the correct (transformed) bounding box
// for icons with non-uniform scaling and rotation. Each group has the same
// icon scaled 3x horizontally and rotated to a different angle, connected
// to a plain small module placed in various relative directions.
// The connection line should start at the edge of the *rotated* bounding box,
// not the original unrotated icon rect.
module ConnTransform
{
    @display("bgb=1100,900");
    submodules:
        // 0 deg: wide horizontal bar, neighbors in all directions
        r0: Dummy { @display("p=200,100;i=block/process;it=0,,3.0,1.0"); }
        r0right: Dummy { @display("p=400,100;is=s"); }
        r0below: Dummy { @display("p=200,200;is=s"); }
        r0diag:  Dummy { @display("p=400,200;is=s"); }
        r0left:  Dummy { @display("p=50,100;is=s"); }
        r0above: Dummy { @display("p=200,30;is=s"); }

        // 30 deg: tilted bar
        r30: Dummy { @display("p=650,100;i=block/process;it=30,,3.0,1.0"); }
        r30right: Dummy { @display("p=850,100;is=s"); }
        r30below: Dummy { @display("p=650,230;is=s"); }
        r30diag:  Dummy { @display("p=850,230;is=s"); }
        r30left:  Dummy { @display("p=470,100;is=s"); }
        r30above: Dummy { @display("p=650,10;is=s"); }

        // 45 deg: diagonal bar
        r45: Dummy { @display("p=200,370;i=block/process;it=45,,3.0,1.0"); }
        r45right: Dummy { @display("p=400,370;is=s"); }
        r45below: Dummy { @display("p=200,500;is=s"); }
        r45diag:  Dummy { @display("p=400,500;is=s"); }
        r45left:  Dummy { @display("p=60,370;is=s"); }
        r45above: Dummy { @display("p=200,250;is=s"); }

        // 90 deg: tall vertical bar
        r90: Dummy { @display("p=650,400;i=block/process;it=90,,3.0,1.0"); }
        r90right: Dummy { @display("p=780,400;is=s"); }
        r90below: Dummy { @display("p=650,560;is=s"); }
        r90diag:  Dummy { @display("p=780,560;is=s"); }
        r90left:  Dummy { @display("p=520,400;is=s"); }
        r90above: Dummy { @display("p=650,250;is=s"); }

        // 135 deg: diagonal bar, other direction
        r135: Dummy { @display("p=200,700;i=block/process;it=135,,3.0,1.0"); }
        r135right: Dummy { @display("p=400,700;is=s"); }
        r135below: Dummy { @display("p=200,830;is=s"); }
        r135diag:  Dummy { @display("p=400,830;is=s"); }
        r135left:  Dummy { @display("p=60,700;is=s"); }
        r135above: Dummy { @display("p=200,580;is=s"); }

        // 60 deg: steeply tilted
        r60: Dummy { @display("p=650,720;i=block/process;it=60,,3.0,1.0"); }
        r60right: Dummy { @display("p=800,720;is=s"); }
        r60below: Dummy { @display("p=650,870;is=s"); }
        r60diag:  Dummy { @display("p=800,870;is=s"); }
        r60left:  Dummy { @display("p=510,720;is=s"); }
        r60above: Dummy { @display("p=650,580;is=s"); }

    connections allowunconnected:
        // 0 deg
        r0right.out++ --> r0.in++;
        r0below.out++ --> r0.in++;
        r0diag.out++ --> r0.in++;
        r0left.out++ --> r0.in++;
        r0above.out++ --> r0.in++;

        // 30 deg
        r30right.out++ --> r30.in++;
        r30below.out++ --> r30.in++;
        r30diag.out++ --> r30.in++;
        r30left.out++ --> r30.in++;
        r30above.out++ --> r30.in++;

        // 45 deg
        r45right.out++ --> r45.in++;
        r45below.out++ --> r45.in++;
        r45diag.out++ --> r45.in++;
        r45left.out++ --> r45.in++;
        r45above.out++ --> r45.in++;

        // 90 deg
        r90right.out++ --> r90.in++;
        r90below.out++ --> r90.in++;
        r90diag.out++ --> r90.in++;
        r90left.out++ --> r90.in++;
        r90above.out++ --> r90.in++;

        // 135 deg
        r135right.out++ --> r135.in++;
        r135below.out++ --> r135.in++;
        r135diag.out++ --> r135.in++;
        r135left.out++ --> r135.in++;
        r135above.out++ --> r135.in++;

        // 60 deg
        r60right.out++ --> r60.in++;
        r60below.out++ --> r60.in++;
        r60diag.out++ --> r60.in++;
        r60left.out++ --> r60.in++;
        r60above.out++ --> r60.in++;
}
