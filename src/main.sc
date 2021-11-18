let stdio = (import C.stdio)
import sdl

import .runtime
import .window
import .gfx
import .events

@@ 'on events.quit
fn ()
    stdio.printf "Quitting, bye!\n"
    true # yes, really quit

@@ 'on events.key-pressed
fn (keysym repeat?)
    if (keysym.sym == sdl.SDLK_ESCAPE)
        events.request-quit;
    ;

fn main (argc argv)
    # order is important here.
    window.init;
    events.init;
    gfx.init;

    window.show;

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
