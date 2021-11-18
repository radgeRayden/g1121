import filter-scope

let header =
    include
        options (.. "-I" module-dir "/../../native/stb")
        """"#include "stb_image.h"
            #include "stb_image_write.h"
filter-scope header.extern "^stbi_"
