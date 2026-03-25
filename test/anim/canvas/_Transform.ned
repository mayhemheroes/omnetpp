module Transform
{
    parameters:
        @display("bgb=800,500");

        @figure[g](type=group; transform=translate(100,100));
        @figure[g.origin](type=oval; pos=0,0; size=6,6);
        @figure[g.r0](type=rectangle; pos=100,100; size=100,100; fillColor=green);
        @figure[g.r1](type=rectangle; pos=100,100; size=100,100; transform=rotate(45); fillColor=blue);
        @figure[g.r2](type=rectangle; pos=100,100; size=100,100; transform=rotate(45,150,150); fillColor=red);


        @figure[g1](type=group; transform=scale(2));
        @figure[g1.r1](type=rectangle; size=20,20; zoomLineWidth=true; lineColor=blue; pos=10,10; anchor=c; transform=translate(200,20),rotate(20));
        @figure[gr1](type=rectangle; size=16,16; zoomLineWidth=true; lineColor=blue; pos=10,10; anchor=c; transform=translate(200,20),rotate(20),scale(2));
        @figure[gr1.label](type=label; pos=24,0; text="two aligned blue squares inside each other");


        @figure[g2](type=group; transform=translate(200,100));
        @figure[g2.r2](type=rectangle; lineColor=red; size=20,20; zoomLineWidth=true; pos=10,10; anchor=c; transform=scale(2));
        @figure[gr2](type=rectangle; lineColor=red; size=16,16; zoomLineWidth=true; pos=10,10; anchor=c; transform=scale(2),translate(200,100));
        @figure[gr2.label](type=label; pos=24,0; text="two aligned red squares inside each other");


        @figure[g3](type=group; transform=scale(0.7),translate(-30,-30),rotate(-15));
        @figure[g3.r3](type=rectangle; lineColor=green; size=20,20; zoomLineWidth=true; pos=10,10; anchor=c; transform=scale(2),translate(100,100));
        @figure[gr3](type=rectangle; lineColor=green; size=16,16; zoomLineWidth=true; pos=10,10; anchor=c; transform=scale(2),translate(100,100),scale(0.7),translate(-30,-30),rotate(-15));
        @figure[gr3.label](type=label; pos=30,0; text="two aligned green squares inside each other");


        @figure[g4a](type=group; transform=translate(10,3),scale(2,0.4),rotate(-1),rotate(-3),translate(200,30),rotate(50,30,200),skewy(-1.1917,400));
        @figure[g4a.r4a](type=rectangle; lineColor=yellow; size=36,36; zoomLineWidth=true; pos=10,10; anchor=c; transform=scale(2),rotate(20,30,40),translate(100,100),scale(0.7),translate(-30,-30),skewx(0.577),rotate(-15),rotate(3));

        @figure[g4b](type=group; transform=translate(-30,-30),skewx(0.577),rotate(-15),rotate(3),translate(10,3),scale(2,0.4),rotate(-1),rotate(-3),translate(200,30),rotate(50,30,200),skewy(-1.1917,400));
        @figure[g4b.r4b](type=rectangle; lineColor=yellow; size=32,32; zoomLineWidth=true; pos=10,10; anchor=c; transform=scale(2),rotate(20,30,40),translate(100,100),scale(0.7));

        @figure[g4c](type=group; transform=translate(100,100),scale(0.7),translate(-30,-30),skewx(0.577),rotate(-15),rotate(3),translate(10,3),scale(2,0.4),rotate(-1),rotate(-3),translate(200,30),rotate(50,30,200),skewy(-1.1917,400));
        @figure[g4c.r4c](type=rectangle; lineColor=yellow; size=28,28; zoomLineWidth=true; pos=10,10; anchor=c; transform=scale(2),rotate(20,30,40));

        @figure[g4d](type=group; transform=translate(200,30),rotate(50,30,200),skewy(-1.1917,400));
        @figure[g4d.r4d](type=rectangle; lineColor=yellow; size=24,24; zoomLineWidth=true; pos=10,10; anchor=c; transform=scale(2),rotate(20,30,40),translate(100,100),scale(0.7),translate(-30,-30),skewx(0.577),rotate(-15),rotate(3),translate(10,3),scale(2,0.4),rotate(-1),rotate(-3));

        @figure[g4e](type=group; transform=rotate(-1),rotate(-3),translate(200,30),rotate(50,30,200),skewy(-1.1917,400));
        @figure[g4e.g4e2](type=group; transform=rotate(-15),rotate(3),translate(10,3),scale(2,0.4));
        @figure[g4e.g4e2.r4e](type=rectangle; lineColor=yellow; size=20,20; zoomLineWidth=true; pos=10,10; anchor=c; transform=scale(2),rotate(20,30,40),translate(100,100),scale(0.7),translate(-30,-30),skewx(0.577));

        @figure[g4f](type=group; transform=rotate(-3),translate(200,30),rotate(50,30,200),skewy(-1.1917,400));
        @figure[g4f.g4f2](type=group; transform=rotate(-1));
        @figure[g4f.g4f2.g4f3](type=group; transform=rotate(-15),rotate(3),translate(10,3),scale(2,0.4));
        @figure[g4f.g4f2.g4f3.g4f4](type=group; transform=scale(0.7),translate(-30,-30),skewx(0.577));
        @figure[g4f.g4f2.g4f3.g4f4.r4f](type=rectangle; lineColor=yellow; size=16,16; zoomLineWidth=true; pos=10,10; anchor=c; transform=scale(2),rotate(20,30,40),translate(100,100));

        @figure[gr4](type=rectangle; lineColor=yellow; size=40,40; zoomLineWidth=true; pos=10,10; anchor=c; transform=scale(2),rotate(20,30,40),translate(100,100),scale(0.7),translate(-30,-30),skewx(0.577),rotate(-15),rotate(3),translate(10,3),scale(2,0.4),rotate(-1),rotate(-3),translate(200,30),rotate(50,30,200),skewy(-1.1917,400));
        @figure[gr4.label](type=label; pos=36,0; text="seven aligned yellow parallelograms inside each other");
}
