{
  description = "OMNeT++ Discrete Event Simulator";

  inputs = {
    nixpkgs.url = "nixpkgs/nixpkgs-unstable";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem(system:
    let
      pname = "omnetpp";
      version = "6.0.0.${nixpkgs.lib.substring 0 8 self.lastModifiedDate}.${self.shortRev or "dirty"}";
      pkgs = import nixpkgs { inherit system; };
    in rec {
      # set different defaults for creating packages.
      oppCallPackage = pkgs.newScope (pkgs // {
          stdenv = pkgs.llvmPackages_14.stdenv;  # use clang14 instead of the standard g++ compiler
          lld = pkgs.lld_14;
          python3 = pkgs.python310;
      });

      packages = rec {
        ${pname} = oppCallPackage ./src/utils/opp_mkDerivation.nix { 
          inherit pname version; 
          src = self;
        };

        default = packages.${pname};
      };

      devShells = rec {
        "${pname}-dev" = pkgs.mkShell { 
          buildInputs = [ 
            pkgs.hello
            self.packages.${system}.default
          ]; 
        };

        default = devShells."${pname}-dev";
      };

    });
}