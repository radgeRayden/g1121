let stdio = (import C.stdio)
let sdl = (import .FFI.sdl)

import .runtime
import .window
import .gfx
import .events

fn main (argc argv)
    # order is important here.
    window.init;
    events.init;
    gfx.init;

    window.show;

    events.set-callback 'quit
        fn (ev)
            stdio.printf "Quitting, bye!\n"
            events.signal-application-exit;

    :: main-loop
    loop ()
        local event : sdl.Event
        while (sdl.PollEvent &event)
            events.dispatch event

            if (events.really-quit? event)
                merge main-loop

        gfx.present;
    main-loop ::

    0

do
    let main
    locals;
