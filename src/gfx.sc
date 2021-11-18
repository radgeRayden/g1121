using import struct
from (import C.stdio) let printf

import wgpu
import stbi
import .window
import .events

inline... &local (T : type, ...)
    &
        local T
            ...
case (value)
    &
        local dummy-name = value

# SHADERS
# ============================================================
let vshader =
    fn ()
        using import glsl
        using import glm

        # 0 ----- 3
        # | \     |
        # |   \   |
        # |     \ |
        # 1 ----- 2
        local vertices =
            arrayof vec3
                vec3  1.0 -1.0 0.0 # 2
                vec3 -1.0  1.0 0.0 # 0
                vec3 -1.0 -1.0 0.0 # 1
                vec3 -1.0  1.0 0.0 # 0
                vec3  1.0 -1.0 0.0 # 2
                vec3  1.0  1.0 0.0 # 3

        # NOTE: inverted UVs to flip images
        local texcoords =
            arrayof vec2
                vec2 1.0 1.0 # 2
                vec2 0.0 0.0 # 0
                vec2 0.0 1.0 # 1
                vec2 0.0 0.0 # 0
                vec2 1.0 1.0 # 2
                vec2 1.0 0.0 # 3

        out vtexcoord : vec2
            location = 0
        out vcolor : vec4
            location = 1

        gl_Position = (vec4 (vertices @ gl_VertexIndex) 1)
        # vcolor = (vec4 (colors @ gl_VertexIndex) 1)
        vcolor = (vec4 1)
        vtexcoord = texcoords @ gl_VertexIndex

let fshader =
    fn ()
        using import glsl
        using import glm

        in vtexcoord : vec2
            location = 0
        in vcolor : vec4
            location = 1
        out fcolor : vec4
            location = 0

        uniform tex : texture2D
            set = 0
            binding = 0
        uniform samp : sampler
            set = 0
            binding = 1

        fcolor = vcolor * (texture (sampler2D tex samp) vtexcoord)

let vshader-SPIRV fshader-SPIRV =
    static-compile-spirv 0x10000 'vertex (static-typify vshader)
    static-compile-spirv 0x10000 'fragment (static-typify fshader)

struct GfxState plain
    surface : wgpu.Surface
    adapter : wgpu.Adapter
    device  : wgpu.Device
    swapchain : wgpu.SwapChain
    queue : wgpu.Queue

    # we're probably gonna remove these
    default-pipeline : wgpu.RenderPipeline
    default-bgroup-layout : wgpu.BindGroupLayout
    default-bgroup : wgpu.BindGroup

global istate : GfxState
global test-texture : wgpu.Texture

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

fn make-default-bindgroup-layout ()
    wgpu.DeviceCreateBindGroupLayout istate.device
        &local wgpu.BindGroupLayoutDescriptor
            entryCount = 2
            entries =
                &local
                    arrayof wgpu.BindGroupLayoutEntry
                        typeinit
                            binding = 0
                            visibility = wgpu.ShaderStage.Fragment
                            texture =
                                typeinit
                                    sampleType = wgpu.TextureSampleType.Float
                                    viewDimension = wgpu.TextureViewDimension.2D
                                    multisampled = false
                        typeinit
                            binding = 1
                            visibility = wgpu.ShaderStage.Fragment
                            sampler =
                                typeinit
                                    type = wgpu.SamplerBindingType.Filtering

fn make-default-pipeline ()
    let pip-layout =
        wgpu.DeviceCreatePipelineLayout istate.device
            &local wgpu.PipelineLayoutDescriptor
                bindGroupLayoutCount = 1
                bindGroupLayouts = &istate.default-bgroup-layout

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

fn make-default-bindgroup ()
    wgpu.DeviceCreateBindGroup istate.device
        &local wgpu.BindGroupDescriptor
            label = "default bindgroup"
            layout = istate.default-bgroup-layout
            entryCount = 2
            entries =
                &local
                    arrayof wgpu.BindGroupEntry
                        typeinit
                            binding = 0
                            textureView =
                                wgpu.TextureCreateView test-texture
                                    &local wgpu.TextureViewDescriptor
                                        format = wgpu.TextureFormat.RGBA8UnormSrgb
                                        dimension = wgpu.TextureViewDimension.2D
                                        baseMipLevel = 0
                                        mipLevelCount = 1
                                        baseArrayLayer = 0
                                        arrayLayerCount = 1
                                        aspect = wgpu.TextureAspect.All
                        typeinit
                            binding = 1
                            sampler =
                                wgpu.DeviceCreateSampler istate.device
                                    &local wgpu.SamplerDescriptor
                                        addressModeU = wgpu.AddressMode.Repeat
                                        addressModeV = wgpu.AddressMode.Repeat
                                        addressModeW = wgpu.AddressMode.Repeat
                                        magFilter = wgpu.FilterMode.Linear
                                        minFilter = wgpu.FilterMode.Linear
                                        mipmapFilter = wgpu.FilterMode.Linear

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

fn make-test-texture ()
    local width : i32
    local height : i32
    local channel-count : i32
    let data = (stbi.load "assets/surfing_pikachu.png" &width &height &channel-count 4)

    test-texture =
        wgpu.DeviceCreateTexture istate.device
            &local wgpu.TextureDescriptor
                label = "test texture"
                usage = (wgpu.TextureUsage.TextureBinding | wgpu.TextureUsage.CopyDst)
                dimension = wgpu.TextureDimension.2D
                size = (wgpu.Extent3D (width as u32) (height as u32) 1)
                format = wgpu.TextureFormat.RGBA8UnormSrgb
                mipLevelCount = 1
                sampleCount = 1

    using import Array
    using import itertools
    using import glm

    wgpu.QueueWriteTexture istate.queue
        &local wgpu.ImageCopyTexture
            texture = test-texture
            mipLevel = 0
            origin = (wgpu.Origin3D)
            aspect = wgpu.TextureAspect.All
        data as voidstar
        width * height * 4
        &local wgpu.TextureDataLayout
            offset = 0
            bytesPerRow = ((width as u32) * 4)
            rowsPerImage = (height as u32)
        &local wgpu.Extent3D (width as u32) (height as u32) 1
    ;

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
    istate.default-bgroup-layout = (make-default-bindgroup-layout)
    istate.default-pipeline = (make-default-pipeline)
    make-test-texture;
    istate.default-bgroup = (make-default-bindgroup)

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
    wgpu.RenderPassEncoderSetBindGroup rp 0 istate.default-bgroup 0 null
    wgpu.RenderPassEncoderDraw rp 6 1 0 0

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
