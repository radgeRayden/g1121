import .runtime

let stdio = ((include "stdio.h") . extern)
let sdl = (import .FFI.sdl)

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

    local running = true
    while running
        local event : sdl.Event
        while (sdl.PollEvent &event)
            switch event.type
            case sdl.SDL_QUIT
                running = false
            default
                ;

    stdio.printf "%s\n" cs"henlo world"
    0

do
    let main
    locals;
