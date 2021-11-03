import platform
from doit.tools import LongRunning
from doit.tools import run_once

OS = platform.system()
is_windows = OS.startswith("MINGW")
is_linux = OS == "Linux"

def task_launch():
    """launch the game"""
    cmd = "scopes ./src/boot.sc -run"
    return {
            'actions': [LongRunning(cmd)],
            # 'file_dep': runtime_targets,
            'uptodate': [False]
        }
