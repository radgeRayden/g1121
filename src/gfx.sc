using import struct
from (import C.stdio) let printf

let wgpu = (import .FFI.wgpu)
import .window
import .events

inline &local (T ...)
    &
        local T
            ...

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

fn create-wgpu-surface ()
    static-match operating-system
    case 'linux
        let x11-display x11-window = (window.get-native-info)
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
        let hinstance hwnd = (window.get-native-info)
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

fn init ()
    istate.surface = (create-wgpu-surface)

    # FIXME: check for status code!
    wgpu.InstanceRequestAdapter null
        &local wgpu.RequestAdapterOptions
            compatibleSurface = istate.surface
            powerPreference = wgpu.PowerPreference.HighPerformance
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

    update-swapchain (window.get-size)

    istate.queue = (wgpu.DeviceGetQueue istate.device)

fn present ()
    let width height = (window.get-size)
    let swapchain-image = (wgpu.SwapChainGetCurrentTextureView istate.swapchain)

    let cmd-encoder =
        wgpu.DeviceCreateCommandEncoder istate.device
            &local wgpu.CommandEncoderDescriptor
                label = "command encoder"

    let rp =
        wgpu.CommandEncoderBeginRenderPass cmd-encoder
            &local wgpu.RenderPassDescriptor
                label = "output render pass"
                colorAttachmentCount = 1
                colorAttachments =
                    &local wgpu.RenderPassColorAttachment
                        view = swapchain-image
                        clearColor = (typeinit 0.017 0.017 0.017 1.0)

    wgpu.RenderPassEncoderEndPass rp

    local cmd-buffer =
        wgpu.CommandEncoderFinish cmd-encoder
            &local wgpu.CommandBufferDescriptor
                label = "command buffer"

    wgpu.QueueSubmit istate.queue 1 &cmd-buffer
    wgpu.SwapChainPresent istate.swapchain
    ;

@@ 'on events.window-size-changed
fn (width height)
    update-swapchain width height

do
    let init present
    locals;