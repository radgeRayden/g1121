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

    stdio.printf "%s\n" cs"henlo world"
    0

do
    let main
    locals;
