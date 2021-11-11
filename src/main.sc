let stdio = (import C.stdio)
let sdl = (import .FFI.sdl)

import .runtime
import .window
import .gfx
import .callbacks

fn main (argc argv)
    # order is important here.
    window.init;
    callbacks.init;
    gfx.init;

    window.show;

    callbacks.set-callback 'quit
        fn (ev)
            stdio.printf "Quitting, bye!\n"
            callbacks.signal-application-exit;

    local running = true
    while running
        local event : sdl.Event
        while (sdl.PollEvent &event)
            callbacks.dispatch (deref event)

            if (event.type == (callbacks.get-exit-event-type))
                running = false

        gfx.present;

    0

do
    let main
    locals;
