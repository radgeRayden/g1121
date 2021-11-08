let sdl = (import .FFI.sdl)

global window : (mutable@ sdl.Window)

fn get-size ()
    local width : i32
    local height : i32
    sdl.GetWindowSize window &width &height

    _ width height

fn get-native-info ()
    local info : sdl.SysWMinfo
    sdl.SDL_VERSION &info.version

    # assert
    (sdl.GetWindowWMInfo window &info)

    let info = info.info

    # FIXME: use the window subsystem enum properly
    static-match operating-system
    case 'linux
        _ info.x11.display info.x11.window
    case 'windows
        _ info.win.hinstance info.win.window
    default
        error "OS not supported"

fn init ()
    sdl.Init
        sdl.SDL_INIT_VIDEO

    window =
        sdl.CreateWindow
            "my very nice game"
            sdl.SDL_WINDOWPOS_UNDEFINED
            sdl.SDL_WINDOWPOS_UNDEFINED
            640
            480
            sdl.SDL_WINDOW_RESIZABLE

do
    let init
        get-size
        get-native-info

    let handle = window
    locals;
