using import struct

import .runtime

let stdio = ((include "stdio.h") . extern)
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

struct GfxState plain
    surface : wgpu.Surface
    adapter : wgpu.Adapter
    device  : wgpu.Device
    swapchain : wgpu.SwapChain
    queue : wgpu.Queue

global istate : GfxState

fn update-swapchain (width height)
    istate.swapchain =
        wgpu.DeviceCreateSwapChain istate.device istate.surface
            &local wgpu.SwapChainDescriptor
                label = "swapchain"
                usage = wgpu.TextureUsage.RenderAttachment
                format = wgpu.TextureFormat.BGRA8UnormSrgb
                width = (width as u32)
                height = (height as u32)
                presentMode = wgpu.PresentMode.Fifo

fn init-wgpu (window)
    istate.surface = (create-wgpu-surface window)

    # FIXME: check for status code!
    wgpu.InstanceRequestAdapter null
        &local wgpu.RequestAdapterOptions
            compatibleSurface = istate.surface
        fn (status result msg userdata)
            istate.adapter = result
            ;
        null
    wgpu.AdapterRequestDevice istate.adapter
        &local wgpu.DeviceDescriptor
            requiredLimits =
                &local wgpu.RequiredLimits
        fn (status result msg userdata)
            istate.device = result
        null

    local width : i32
    local height : i32
    sdl.GetWindowSize window &width &height
    update-swapchain width height

    istate.queue = (wgpu.DeviceGetQueue istate.device)

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

    init-wgpu window

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
