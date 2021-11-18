using import Map
using import FunctionChain
from (import C.stdio) let printf

import sdl

let EventCallback = (@ (function void sdl.Event))
let WindowEventCallback = (@ (function void sdl.WindowEvent))

global event-callbacks : (Map sdl.EventType EventCallback)
global window-event-callbacks : (Map sdl.WindowEventID WindowEventCallback)

fn dispatch (ev)
    fn dummy-handler (ev)
        ;

    if (ev.type == sdl.SDL_WINDOWEVENT)
        let fun =
            'getdefault window-event-callbacks ev.window.event
                (imply dummy-handler WindowEventCallback)
        fun ev.window
    else
        let fun =
            'getdefault event-callbacks ev.type
                (imply dummy-handler EventCallback)
        fun ev

inline set-callback (id fun)
    let idT = (typeof id)

    static-match idT
    case sdl.EventType
        'set event-callbacks id (imply fun EventCallback)
    case sdl.WindowEventID
        'set window-event-callbacks id (imply fun WindowEventCallback)
    default
        error "unexpected event id type"
        unreachable;

vvv bind callbacks
do
    fnchain quit
    fnchain window-size-changed
    fnchain key-pressed
    fnchain key-released
    locals;

# To be able to handle a quit callback with the ability to back off and keep running,
# we need an event that means "I really mean it, please quit" that is meant to be triggered
# from inside such a callback.
global application-exit-event : sdl.EventType

fn signal-application-exit ()
    local ev : sdl.Event
    ev.type = application-exit-event
    sdl.PushEvent &ev
    ;

fn really-quit? (ev)
    ev.type == application-exit-event

fn request-quit ()
    local ev : sdl.Event
    ev.type = sdl.SDL_QUIT
    sdl.PushEvent &ev
    ;

fn init ()
    application-exit-event = (sdl.RegisterEvents 1)

    set-callback sdl.SDL_QUIT
        fn (ev)
            let result = (callbacks.quit)
            if result
                signal-application-exit;

    set-callback sdl.SDL_WINDOWEVENT_SIZE_CHANGED
        fn (ev)
            callbacks.window-size-changed ev.data1 ev.data2

    set-callback sdl.SDL_KEYDOWN
        fn (ev)
            callbacks.key-pressed ev.key.keysym ev.key.repeat

..
    callbacks
    do
        let init dispatch really-quit? request-quit
        locals;
