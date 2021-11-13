using import struct
from (import C.stdio) let printf

let wgpu = (import .FFI.wgpu)
import .window
import .events

inline &local (T ...)
    &
        local T
            ...

# SHADERS
# ============================================================
let vshader =
    fn ()
        using import glsl
        using import glm

        out vcolor : vec4
            location = 0

        local vertices =
            arrayof vec3
                vec3 -0.5 -0.5 0.0
                vec3  0.5 -0.5 0.0
                vec3  0.0  0.5 0.0

        local colors =
            arrayof vec3
                vec3 0.0 1.0 0.0
                vec3 0.0 0.0 1.0
                vec3 1.0 0.0 0.0

        gl_Position = (vec4 (vertices @ gl_VertexIndex) 1)
        vcolor = (vec4 (colors @ gl_VertexIndex) 1)

let fshader =
    fn ()
        using import glsl
        using import glm

        in vcolor : vec4
            location = 0
        out fcolor : vec4
            location = 0

        fcolor = vcolor

let vshader-SPIRV fshader-SPIRV =
    static-compile-spirv 0x10000 'vertex (static-typify vshader)
    static-compile-spirv 0x10000 'fragment (static-typify fshader)

struct GfxState plain
    surface : wgpu.Surface
    adapter : wgpu.Adapter
    device  : wgpu.Device
    swapchain : wgpu.SwapChain
    queue : wgpu.Queue
    default-pipeline : wgpu.RenderPipeline

global istate : GfxState

inline shader-module-from-SPIRV (code)
    local desc : wgpu.ShaderModuleSPIRVDescriptor
        chain =
            wgpu.ChainedStruct
                sType = wgpu.SType.ShaderModuleSPIRVDescriptor
        codeSize = ((countof code) // 4)
        code = (code as rawstring as (@ u32))

    let module =
        wgpu.DeviceCreateShaderModule
            istate.device
            &local wgpu.ShaderModuleDescriptor
                nextInChain = (&desc as (mutable@ wgpu.ChainedStruct))
    module

fn make-default-pipeline ()
    let pip-layout =
        wgpu.DeviceCreatePipelineLayout istate.device
            &local wgpu.PipelineLayoutDescriptor
                bindGroupLayoutCount = 0
                bindGroupLayouts = null

    wgpu.DeviceCreateRenderPipeline istate.device
        &local wgpu.RenderPipelineDescriptor
            layout = pip-layout
            vertex =
                wgpu.VertexState
                    module = (shader-module-from-SPIRV vshader-SPIRV)
                    entryPoint = "main"
            primitive =
                wgpu.PrimitiveState
                    topology = wgpu.PrimitiveTopology.TriangleList
                    frontFace = wgpu.FrontFace.CCW
                    cullMode = wgpu.CullMode.None
            multisample =
                wgpu.MultisampleState
                    count = 1
                    mask = (~ 0:u32)
                    alphaToCoverageEnabled = false
            fragment =
                &local wgpu.FragmentState
                    module = (shader-module-from-SPIRV fshader-SPIRV)
                    entryPoint = "main"
                    targetCount = 1
                    targets =
                        &local wgpu.ColorTargetState
                            format = (wgpu.SurfaceGetPreferredFormat
                                istate.surface istate.adapter)
                            blend =
                                &local wgpu.BlendState
                                    color =
                                        typeinit
                                            srcFactor = wgpu.BlendFactor.One
                                            dstFactor = wgpu.BlendFactor.Zero
                                            operation = wgpu.BlendOperation.Add
                                    alpha =
                                        typeinit
                                            srcFactor = wgpu.BlendFactor.One
                                            dstFactor = wgpu.BlendFactor.Zero
                                            operation = wgpu.BlendOperation.Add
                            writeMask = wgpu.ColorWriteMask.All


fn update-swapchain (width height)
    let format =(wgpu.SurfaceGetPreferredFormat istate.surface istate.adapter)
    printf "surface format: %d\n" format
    istate.swapchain =
        wgpu.DeviceCreateSwapChain istate.device istate.surface
            &local wgpu.SwapChainDescriptor
                label = "swapchain"
                usage = wgpu.TextureUsage.RenderAttachment
                format = (wgpu.SurfaceGetPreferredFormat istate.surface istate.adapter)
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
    istate.default-pipeline = (make-default-pipeline)

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

    wgpu.RenderPassEncoderSetPipeline rp istate.default-pipeline
    wgpu.RenderPassEncoderDraw rp 3 1 0 0

    wgpu.RenderPassEncoderEndPass rp

    local cmd-buffer =
        wgpu.CommandEncoderFinish cmd-encoder
            &local wgpu.CommandBufferDescriptor
                label = "command buffer"

    wgpu.QueueSubmit istate.queue 1 &cmd-buffer
    wgpu.SwapChainPresent istate.swapchain
    ;

# ============================================================
@@ 'on events.window-size-changed
fn (width height)
    update-swapchain width height

do
    let init present
    locals;
