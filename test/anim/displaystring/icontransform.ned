module IconTransform
{
    @display("bgb=800,610");
    submodules:
        // Row 1: Rotation only
        rot0: Dummy {
            @display("p=80,60;i=device/antennatower_l;it=0");
        }
        rot45: Dummy {
            @display("p=180,60;i=device/antennatower_l;it=45");
        }
        rot120: Dummy {
            @display("p=280,60;i=device/antennatower_l;it=120");
        }
        rot390: Dummy {
            @display("p=380,60;i=device/antennatower_l;it=390");
        }
        rotneg30: Dummy {
            @display("p=480,60;i=device/antennatower_l;it=-30");
        }

        // Row 2: Flip
        noflip: Dummy {
            @display("p=80,170;i=device/laptop");
        }
        flipH: Dummy {
            @display("p=180,170;i=device/laptop;it=,h");
        }
        flipV: Dummy {
            @display("p=280,170;i=device/laptop;it=,v");
        }
        flipB: Dummy {
            @display("p=380,170;i=device/laptop;it=,b");
        }
        flipHRot45: Dummy {
            @display("p=480,170;i=device/laptop;it=45,h");
        }

        // Row 3: Scale (uniform)
        scale05: Dummy {
            @display("p=80,280;i=block/app;it=,,0.5");
        }
        scale10: Dummy {
            @display("p=180,280;i=block/app;it=,,1.0");
        }
        scale15: Dummy {
            @display("p=280,280;i=block/app;it=,,1.5");
        }
        scale20: Dummy {
            @display("p=380,280;i=block/app;it=,,2.0");
        }

        // Row 4: Non-uniform scale
        scaleX2Y1: Dummy {
            @display("p=80,400;i=block/sink;it=,,2.0,1.0");
        }
        scaleX1Y2: Dummy {
            @display("p=230,400;i=block/sink;it=,,1.0,2.0");
        }
        scaleX15Y05: Dummy {
            @display("p=380,400;i=block/sink;it=,,1.5,0.5");
        }

        // Row 5: Combined transforms
        flipHscale15rot90: Dummy {
            @display("p=80,520;i=block/sink;it=90,h,1.5;tt=Order: scale flip rot (rot is last!)");
        }
        flipVscaleY22rot45: Dummy {
            @display("p=230,520;i=block/sink;it=45,v,1.0,2.0;tt=Order: scale flip rot (rot is last!)");
        }
}
