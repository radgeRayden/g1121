import .globals
import .main

compile-object
    default-target-triple
    compiler-file-kind-object
    module-dir .. "/../build/game.o"
    do
        let main = (static-typify main.main i32 (mutable@ rawstring))
        locals;
    'O2

let src argc argv = (script-launch-args)
for i in (range argc)
    if ((string (argv @ i)) == "-run")
        main.main 0 0
        break;
