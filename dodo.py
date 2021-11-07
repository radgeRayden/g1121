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

if is_windows:
    make_flavor = "MinGW"
    make = "mingw32-make"
    cc = "x86_64-w64-mingw32-gcc"
    cxx = "x86_64-w64-mingw32-g++"
    lflags_common = lflags_common + " -lgdi32 -lwinmm -lole32 -luuid"
    exename = "game.exe"
elif "Linux" in operating_system:
    make_flavor = "Unix"
    make = "make"
    cc = "gcc"
    cxx = "g++"
    lflags_common = lflags_common + " -ldl -lX11 -lasound"
    exename = "game"
else:
    raise UnsupportedPlatform

def task_bin():
    """build the game binary"""
    return {
        'actions': ["scopes ./src/boot.sc", f"gcc -o bin/{exename} ./build/game.o -lSDL2"],
        'targets': ["./bin/{exename}"],
        'uptodate': [False]
    }

def task_launch():
    """launch the game"""
    cmd = "./bin/{exename}"
    return {
        'actions': [LongRunning(cmd)],
        'file_dep': ["./bin/{exename}"],
        'uptodate': [False]
    }
