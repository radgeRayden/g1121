import .runtime

let stdio = ((include "stdio.h") . extern)
let sdl2 = (import .FFI.sdl2)

fn main (argc argv)
    sdl2.Init
        sdl2.SDL_INIT_VIDEO

    let window =
        sdl2.CreateWindow
            "my very nice game"
            sdl2.SDL_WINDOWPOS_UNDEFINED
            sdl2.SDL_WINDOWPOS_UNDEFINED
            640
            480
            0

    stdio.printf "%s\n" cs"henlo world"
    0

do
    let main
    locals;
