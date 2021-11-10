using import Map

let sdl = (import .FFI.sdl)

let EventCallback = (@ (function void sdl.Event))
let WindowEventCallback = (@ (function void sdl.WindowEvent))

global event-callbacks : (Map sdl.EventType EventCallback)
global window-event-callbacks : (Map sdl.WindowEventID WindowEventCallback)

fn init ()
    event-callbacks = (typeinit)
    window-event-callbacks = (typeinit)

fn dispatch (ev)
    if (ev.type == sdl.SDL_WINDOWEVENT)
        try (('get window-event-callbacks ev.window.event) ev.window)
        else ()
    else
        try (('get event-callbacks ev.type) ev)
        else ()

fn... set-callback (evtype : sdl.EventType, fun : EventCallback)
    'set event-callbacks evtype fun
case (evtype : sdl.WindowEventID, fun : WindowEventCallback)
    'set window-event-callbacks evtype fun

do
    let init dispatch set-callback
    locals;
