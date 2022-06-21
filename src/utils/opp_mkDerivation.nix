{ 
  pname, version, src ? ./.,                 # direct parameters
  stdenv, lib, fetchurl, symlinkJoin, lndir, # build environment
  binutils, perl, flex, bison, lld,          # dependencies
  python3,
  MODE ? "release",                          # build parameters
}:
let
  omnetpp-outputs = stdenv.mkDerivation rec {
    inherit pname version src;

    outputs = [ "bin" "dev" "out" ]; # doc, samples, gui, gui3d, ide

    enableParallelBuilding = true;
    strictDeps = true;
    dontStrip = true;
    # hardeningDisable = all;

    buildInputs = [ ];

    # tools required for build only (not needed in derivations)
    nativeBuildInputs = [  ];

    # tools required for build only (needed in derivations)
    propagatedNativeBuildInputs = [
      perl bison flex binutils lld lndir
      (python3.withPackages(ps: with ps; [ numpy pandas matplotlib scipy seaborn posix_ipc ]))
    ];

    configureFlags = [ "WITH_QTENV=no" "WITH_OSG=no" "WITH_OSGEARTH=no"
    #  "LDFLAGS=-Wl,-rpath,${placeholder "bin"}/lib"
    ];

    # we have to patch all shebangs to use NIX versions of the interpreters
    prePatch = ''
      patchShebangs src/nedxml
      patchShebangs src/utils
    '';

    preConfigure = ''
      if [ ! -f configure.user ]; then
        cp configure.user.dist configure.user
      fi
      source setenv
      rm -rf samples
    '';

    buildPhase = ''
      make -j16 MODE=${MODE}
    '';

    installPhase = ''
      runHook preInstall

      mkdir -p ${placeholder "bin"}/bin ${placeholder "bin"}/lib
      (cd bin && mv  opp_run* opp_msgtool opp_msgc opp_nedtool opp_charttool opp_eventlogtool opp_fingerprinttest opp_scavetool opp_test ${placeholder "bin"}/bin )
      (cd lib && mv *.so ${placeholder "bin"}/lib )
      mv python ${placeholder "bin"}

      mkdir -p ${placeholder "dev"}/bin ${placeholder "dev"}/lib
      mv Makefile.inc Version configure.user setenv include src ${placeholder "dev"}
      (cd bin && mv opp_configfilepath opp_featuretool opp_makemake opp_shlib_postprocess ${placeholder "dev"}/bin )
      (cd lib && mv *.a ${placeholder "dev"}/lib )

      mkdir -p ${placeholder "out"}

      runHook postInstall
      '';


    preFixup = ''
      (
        # patch rpath on BIN executables
        for file in $(find ${placeholder "bin"} -type f -executable); do
          if patchelf --print-rpath $file; then
            patchelf --set-rpath '${placeholder "bin"}/lib' $file
          fi
        done
      )
      '';

    meta = with lib; {
      outputsToInstall = "bin dev";
      homepage= "https://omnetpp.org";
      description = "OMNeT++ Discrete Event Simulator runtime";
      longDescription = "OMNeT++ is an extensible, modular, component-based C++ simulation library and framework, primarily for building network simulators.";
      changelog = "https://github.com/omnetpp/omnetpp/blob/omnetpp-${version}/WHATSNEW";
      license = licenses.free;
      maintainers = [ "rudi@omnetpp.org" ];
      platforms = [ "x86_64-linux" "aarch64-linux" "x86_64-darwin" "aarch64-darwin" ];
    };
  };
in
#   omnetpp-outputs

  symlinkJoin {
    name = "${pname}-${version}";
    paths = with omnetpp-outputs; [ bin dev ]; 
    postBuild = "";  # TODO optimize the symlink forest (include, src, images, samples, python could be linked as a single directory)
  }
