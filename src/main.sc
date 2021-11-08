import .runtime

let sdl = (import .FFI.sdl)
let wgpu = (import .FFI.wgpu)

inline &local (T ...)
    &
        local T
            ...

fn get-native-window-info (window)
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

fn create-wgpu-surface (window)
    static-match operating-system
    case 'linux
        let x11-display x11-window = (get-native-window-info window)
        wgpu.InstanceCreateSurface null
            &local wgpu.SurfaceDescriptor
                nextInChain =
                    as
                        &local wgpu.SurfaceDescriptorFromXlib
                            chain =
                                wgpu.ChainedStruct
                                    sType = wgpu.SType.SurfaceDescriptorFromXlib
                            display = (x11-display as voidstar)
                            window = (x11-window as u32)
                        mutable@ wgpu.ChainedStruct
    case 'windows
        let hinstance hwnd = (get-native-window-info window)
        wgpu.InstanceCreateSurface null
            &local wgpu.SurfaceDescriptor
                nextInChain =
                    as
                        &local wgpu.SurfaceDescriptorFromWindowsHWND
                            chain =
                                wgpu.ChainedStruct
                                    sType = wgpu.SType.SurfaceDescriptorFromWindowsHWND
                            hinstance = hinstance
                            hwnd = hwnd
                        mutable@ wgpu.ChainedStruct
    default
        error "OS not supported"

fn main (argc argv)
    sdl.Init
        sdl.SDL_INIT_VIDEO

    let window =
        sdl.CreateWindow
            "my very nice game"
            sdl.SDL_WINDOWPOS_UNDEFINED
            sdl.SDL_WINDOWPOS_UNDEFINED
            640
            480
            0

    let surface = (create-wgpu-surface window)

    local running = true
    while running
        local event : sdl.Event
        while (sdl.PollEvent &event)
            switch event.type
            case sdl.SDL_QUIT
                running = false
            default
                ;

    0

do
    let main
    locals;
