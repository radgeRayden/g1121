using import Map

let sdl = (import .FFI.sdl)

vvv bind events
do
    let audio-device-added = sdl.SDL_AUDIODEVICEADDED
    let audio-device-removed = sdl.SDL_AUDIODEVICEREMOVED
    let controller-axis-motion = sdl.SDL_CONTROLLERAXISMOTION
    let controller-button-down = sdl.SDL_CONTROLLERBUTTONDOWN
    let controller-button-up = sdl.SDL_CONTROLLERBUTTONUP
    let controller-device-added = sdl.SDL_CONTROLLERDEVICEADDED
    let controller-device-removed = sdl.SDL_CONTROLLERDEVICEREMOVED
    let controller-device-remapped = sdl.SDL_CONTROLLERDEVICEREMAPPED
    let dollar-gesture = sdl.SDL_DOLLARGESTURE
    let dollar-record = sdl.SDL_DOLLARRECORD
    let file-dropped = sdl.SDL_DROPFILE
    let text-dropped = sdl.SDL_DROPTEXT
    let drop-begin = sdl.SDL_DROPBEGIN
    let drop-complete = sdl.SDL_DROPCOMPLETE
    let finger-motion = sdl.SDL_FINGERMOTION
    let finger-down = sdl.SDL_FINGERDOWN
    let finger-up = sdl.SDL_FINGERUP
    let key-down = sdl.SDL_KEYDOWN
    let key-up = sdl.SDL_KEYUP
    let joy-axis-motion = sdl.SDL_JOYAXISMOTION
    let joy-ball-motion = sdl.SDL_JOYBALLMOTION
    let joy-hat-motion = sdl.SDL_JOYHATMOTION
    let joy-button-down = sdl.SDL_JOYBUTTONDOWN
    let joy-button-up = sdl.SDL_JOYBUTTONUP
    let joy-device-added = sdl.SDL_JOYDEVICEADDED
    let joy-device-removed = sdl.SDL_JOYDEVICEREMOVED
    let mouse-moved = sdl.SDL_MOUSEMOTION
    let mouse-button-down = sdl.SDL_MOUSEBUTTONDOWN
    let mouse-button-up = sdl.SDL_MOUSEBUTTONUP
    let mouse-wheel = sdl.SDL_MOUSEWHEEL
    let multi-gesture = sdl.SDL_MULTIGESTURE
    let quit = sdl.SDL_QUIT
    let syswm-event = sdl.SDL_SYSWMEVENT
    let text-editing = sdl.SDL_TEXTEDITING
    let text-input = sdl.SDL_TEXTINPUT
    let user-event = sdl.SDL_USEREVENT

    let window-shown = sdl.SDL_WINDOWEVENT_SHOWN
    let window-hidden = sdl.SDL_WINDOWEVENT_HIDDEN
    let window-exposed = sdl.SDL_WINDOWEVENT_EXPOSED
    let window-moved = sdl.SDL_WINDOWEVENT_MOVED
    let window-resized = sdl.SDL_WINDOWEVENT_RESIZED
    let window-size-changed = sdl.SDL_WINDOWEVENT_SIZE_CHANGED
    let window-minimized = sdl.SDL_WINDOWEVENT_MINIMIZED
    let window-maximized = sdl.SDL_WINDOWEVENT_MAXIMIZED
    let window-restored = sdl.SDL_WINDOWEVENT_RESTORED
    let window-enter = sdl.SDL_WINDOWEVENT_ENTER
    let window-leave = sdl.SDL_WINDOWEVENT_LEAVE
    let window-focus-gained = sdl.SDL_WINDOWEVENT_FOCUS_GAINED
    let window-focus-lost = sdl.SDL_WINDOWEVENT_FOCUS_LOST
    let window-close = sdl.SDL_WINDOWEVENT_CLOSE
    let window-take-focus = sdl.SDL_WINDOWEVENT_TAKE_FOCUS
    let window-hit-test = sdl.SDL_WINDOWEVENT_HIT_TEST

    locals;

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

inline set-callback (evname fun)
    let id = (getattr events evname)
    let idT = (typeof id)

    static-match idT
    case sdl.EventType
        'set event-callbacks id (imply fun EventCallback)
    case sdl.WindowEventID
        'set window-event-callbacks id (imply fun WindowEventCallback)
    default
        error "unexpected event id type"
        unreachable;

do
    let dispatch set-callback
    locals;
