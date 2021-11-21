import platform
from doit.tools import LongRunning
from doit.tools import run_once

class UnsupportedPlatform(Exception):
    pass

operating_system = platform.system()
is_windows = operating_system.startswith("MINGW")
is_linux = operating_system == "Linux"

make_flavor = ""
make = ""
cc = ""
cxx = ""
lflags_common = "-lpthread -lm"
exename = ""
libname = ""

if is_windows:
    make_flavor = "MinGW"
    make = "mingw32-make"
    cc = "x86_64-w64-mingw32-gcc"
    cxx = "x86_64-w64-mingw32-g++"
    lflags_common = lflags_common + " -lgdi32 -lwinmm -lole32 -luuid"
    exename = "game.exe"
    libname = "libgame.dll"
elif is_linux:
    make_flavor = "Unix"
    make = "make"
    cc = "gcc"
    cxx = "g++"
    lflags_common = lflags_common + " -ldl -lX11"
    exename = "game"
    libname = "game"
else:
    raise UnsupportedPlatform

def task_wgpu():
    """build webgpu native"""
    wgpu = "./native/wgpu"
    return {
        'actions': [f"{make} -C {wgpu} lib-native", f"cp {wgpu}/target/debug/libwgpu_native.so ./bin/"],
        'targets': [f"./bin/libwgpu_native.so"],
        'uptodate': [True]
    }

runtime_libs = [
    "wgpu_native",
    f"{libname}"
]

libgame_sources = [
    "./native/hash.c",
    "./native/stb.c",
]

def libgame_linux():
    print("here")
    sources = " ".join(libgame_sources)
    lflags = f"-Wl,-E {lflags_common}"
    return {
        'basename': f"lib{libname}.so",
        'actions': [f"gcc -o ./bin/lib{libname}.so -fPIC {sources} -shared {lflags}"],
        'targets': [f"/bin/lib{libname}.so"],
        'file_dep': libgame_sources
    }

def task_libgame():
    if is_windows:
        yield libgame_windows()
    elif is_linux:
        yield libgame_linux()
    else:
        raise UnsupportedPlatform

def task_bin():
    """build the game binary"""
    llibs = "".join(f" -l{lib}" for lib in runtime_libs)
    return {
        'actions': ["scopes ./src/boot.sc", f"gcc -o bin/{exename} ./build/game.o -Wl,-rpath='$ORIGIN' -L./bin {llibs} -lSDL2 {lflags_common}"],
        'targets': [f"./bin/{exename}"],
        'file_dep': [f"./bin/lib{lib}.so" for lib in runtime_libs],
        'uptodate': [False]
    }

def task_launch():
    """launch the game"""
    cmd = f"./bin/{exename}"
    return {
        'actions': [LongRunning(cmd)],
        'file_dep': [f"./bin/{exename}"],
        'uptodate': [False]
    }
