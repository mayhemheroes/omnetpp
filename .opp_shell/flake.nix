{
  description = "OMNeT++ development environment (NixOS)";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-25.11";
    flake-utils.url = "github:numtide/flake-utils";
  };

  outputs = { self, nixpkgs, flake-utils }:
    flake-utils.lib.eachDefaultSystem (system:
      let
        pkgs = import nixpkgs { inherit system; };

        # Dev tools
        devTools = with pkgs; [
          # Common utilities for the shell
          bashInteractive
          which

          # Build Tools
          ccache
          gcc
          lld
          lldb
          gnumake
          autoconf
          bison
          flex
          perl
          pkg-config
          xdg-utils
          swig

          # additional tools used in the opp project
          graphviz
          doxygen
        ];

        fhsTools = with pkgs; [
          clang
          clang-tools
          uv
        ];

        nativeTools = with pkgs; [
        ];

        ideDependencies = with pkgs; [
          # IDE dependencies
          gtk3
          glib-networking
          libsecret
          cairo
          freetype
          fontconfig
          xorg.libXtst
          xorg.libX11
          xorg.libXrender
          adw-gtk3
          gsettings-desktop-schemas
          webkitgtk_4_1
        ];

        # System libraries required by binary files (omnetpp or python wheels)
        runtimeLibs = with pkgs; [
          # libstdc++
          stdenv.cc.cc.lib

          # Common core libraries
          zlib
          glib
          libGL
          libGL.dev
          libffi
          openssl
          icu
          libxml2
          elfutils
          elfutils.dev

          # UI & Graphics
          qt6.qtbase
          qt6.qtwayland
          qt6.qtsvg
          openscenegraph

          # inet
          ffmpeg-headless
          ffmpeg-headless.dev
          z3
          z3.dev
        ];

        fhsRuntimeLibs = with pkgs; [
          python3
        ];

        nativeRuntimeLibs = with pkgs;[  
          (python3.withPackages (ps: with ps; [
            packaging
            matplotlib
            numpy
            pandas
            scipy
            ipython
          ]))
        ];

        fhsEnv = pkgs.buildFHSEnv {
          name = "opp_shell";
          targetPkgs = pkgs: (ideDependencies ++ devTools ++ fhsTools ++ runtimeLibs ++ fhsRuntimeLibs);

          profile = ''
            export name="opp_shell" # important because it is used in install.sh to skip dependency installation
            # primarily link against FHS mapped system libraries to emulate a generic Linux environment
            export LD_LIBRARY_PATH=/lib:/lib64:/usr/lib:/usr/lib64:$LD_LIBRARY_PATH

            export OMP_NUM_THREADS=$(nproc)
            export CC=clang
            export CXX=clang++
            # Disable Nix hardening specifically for fortify to avoid the warning in debug builds (-O0)
            export NIX_HARDENING_ENABLE="stackprotector,format,relro,bindnow,pic"

            if [ -z "$__omnetpp_root" ]; then
              __omnetpp_root=.
            fi

            # initialization must be run from the omnetpp root directory
            cd $__omnetpp_root

            # Initialize venv using uv
            if [ ! -d ".venv" ]; then
              echo "Initializing virtual environment with uv..."
              uv venv .venv
              source .venv/bin/activate
              if [ -f "python/requirements.txt" ]; then
                echo "Syncing dependencies..."
                uv pip install -r python/requirements.txt
              fi
            else
              source .venv/bin/activate
            fi

            if [ -f ./setenv ]; then
              source ./setenv
            fi
            
            # restore the original working directory
            cd - >/dev/null
          '';
        };

        nativeEnv = (pkgs.mkShell.override { stdenv = pkgs.clangStdenv; }) {
          name = "opp_shell_native"; # important because it is used in install.sh to skip dependency installation
          hardeningDisable = [ "fortify" ];

          nativeBuildInputs = devTools ++ nativeTools;
          buildInputs = runtimeLibs ++ nativeRuntimeLibs;

          # NIX_LD handles unpatched executables (like the IDE)
          # this requires programs.nix-ld.enable = true; in the global nixos config
          # alternatively ideDependencies could be added to the LD_LIBRARY_PATH in the shellHook
          NIX_LD = pkgs.lib.fileContents "${pkgs.stdenv.cc}/nix-support/dynamic-linker";
          NIX_LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath (ideDependencies);

          shellHook = ''
            export name="opp_shell_native" # important because it is used in install.sh to skip dependency installation

            export OMP_NUM_THREADS=$(nproc)
            export MKL_DEBUG_CPU_TYPE=5

            # Ensure project-built binaries find their runtime dependencies
            export LD_LIBRARY_PATH="${pkgs.lib.makeLibraryPath (runtimeLibs ++ nativeRuntimeLibs)}:$LD_LIBRARY_PATH"

            # Disable Nix hardening specifically for fortify to avoid the warning in debug builds (-O0)
            export NIX_HARDENING_ENABLE="stackprotector,format,relro,bindnow,pic"

            if [ -z "$__omnetpp_root" ]; then
              __omnetpp_root=.
            fi

            echo "__omnetpp_root: $__omnetpp_root"
            if [ -f $__omnetpp_root/setenv ]; then
              source $__omnetpp_root/setenv
            fi
          '';
        };


      in
      {
        packages = {
          default = fhsEnv;
          native = nativeEnv;
        };

        devShells = {
          default = fhsEnv.env;
          native = nativeEnv;
        };
      });
}