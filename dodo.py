import platform
from doit.tools import LongRunning
from doit.tools import run_once

operating_system = platform.system()
is_windows = operating_system.startswith("MINGW")
is_linux = operating_system == "Linux"

def task_bin():
    """build the game binary"""
    return {
        'actions': ["scopes ./src/boot.sc", f"gcc -o bin/game ./build/game.o"],
        'targets': ["./bin/game"],
        'uptodate': [False]
    }

def task_launch():
    """launch the game"""
    cmd = "./bin/game"
    return {
        'actions': [LongRunning(cmd)],
        'file_dep': ["./bin/game"],
        'uptodate': [False]
    }
