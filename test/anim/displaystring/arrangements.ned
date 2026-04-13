module Arrangements
{
    submodules:
        col1[6]: Dummy { @display("is=s;p=100,60,c"); }
        col2[8]: Dummy { @display("is=s;p=200,60,c,,5"); }
        col3[8]: Dummy { @display("is=s;p=400,60,c,30,5,80"); }
        row1[8]: Dummy { @display("is=s;p=100,400,r"); }
        row2[8]: Dummy { @display("is=s;p=100,500,r,,5"); }
        row3[8]: Dummy { @display("is=s;p=100,650,r,30,5,80"); }
        matrix[10]: Dummy { @display("is=s;p=550,600,m,4"); }
        ring[7]: Dummy { @display("is=s;p=650,60,ri"); }
        ringpos: Dummy { @display("i=misc/node_vs,red;p=650,60"); }
        cring[7]: Dummy { @display("is=s;p=700,400,cr"); }
        cringpos: Dummy { @display("i=misc/node_vs,red;p=700,400"); }
}


