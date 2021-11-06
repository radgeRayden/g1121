using import .ffi-helpers

let header =
    include
        "SDL2/SDL.h"

let sdl-extern = (filter-scope header.extern "^SDL_")
let sdl-typedef = (filter-scope header.typedef "^SDL_")
let sdl-define = (filter-scope header.define "^(?=SDL_)")
let sdl-const = (filter-scope header.const "^(?=SDL_)")

let sdl = (.. sdl-extern sdl-typedef sdl-define sdl-const)
run-stage;

let sdl-macros =
    do
        inline SDL_WINDOWPOS_UNDEFINED_DISPLAY (X)
            sdl.SDL_WINDOWPOS_UNDEFINED_MASK | X

        inline SDL_WINDOWPOS_ISUNDEFINED (X)
            (X & 0xFFFF0000) == sdl.SDL_WINDOWPOS_UNDEFINED_MASK

        inline SDL_WINDOWPOS_CENTERED_DISPLAY (X)
            sdl.SDL_WINDOWPOS_CENTERED_MASK | X

        inline SDL_WINDOWPOS_ISCENTERED (X)
            (X & 0xFFFF0000) == sdl.SDL_WINDOWPOS_CENTERED_MASK

        let SDL_WINDOWPOS_CENTERED = (SDL_WINDOWPOS_CENTERED_DISPLAY 0)
        let SDL_WINDOWPOS_UNDEFINED = (SDL_WINDOWPOS_UNDEFINED_DISPLAY 0)

        inline SDL_VERSION (version-struct)
            imply version-struct sdl.version
            version-struct.major = sdl.SDL_MAJOR_VERSION
            version-struct.minor = sdl.SDL_MINOR_VERSION
            version-struct.patch = sdl.SDL_PATCHLEVEL
            ;

        inline SDL_VERSIONNUM (major minor patch)
            (major * 1000) + (minor * 100) + patch

        inline SDL_COMPILEDVERSION ()
            SDL_VERSIONNUM sdl.SDL_MAJOR_VERSION sdl.SDL_MINOR_VERSION sdl.SDL_PATCHLEVEL

        inline SDL_VERSION_ATLEAST (major minor patch)
            (SDL_COMPILEDVERSION) >= (SDL_VERSIONNUM major minor patch)
        locals;

sdl .. sdl-macros
