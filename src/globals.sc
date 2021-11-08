# This module replaces some core constructs that aren't suitable for AOT compilation. It also
# provides a couple facilities that I want to use in the codebase without importing anything,
# although I try to keep that usage to a minimum.

# To avoid cluttering the package namespace, lets import all C functions
  we make use of at this stage.
let C =
    .
        include
            """"void abort();
                int printf(const char *restrict format, ...);
        extern

# we redefine assert to avoid depending on the scopes runtime.
spice _aot-assert (args...)
    inline check-assertion (result anchor msg)
        if (not result)
            C.printf "%s assertion failed: %s \n"
                anchor as rawstring
                msg
            C.abort;
    let argc = ('argcount args)
    verify-count argc 2 2
    let expr msg =
        'getarg args 0
        'getarg args 1
    let msgT = ('typeof msg)
    if ((msgT != string) and (msgT != rawstring))
        error "string expected as second argument"
    let anchor = ('anchor args)
    let anchor-text = (repr anchor)
    'tag `(check-assertion expr [anchor-text] (msg as rawstring)) anchor

sugar aot-assert (args...)
    let args = (args... as list)
    let cond msg body = (decons args 2)
    let anchor = ('anchor cond)
    let msg = (convert-assert-args args cond msg)
    list ('tag `_aot-assert anchor) cond msg

# convert to C string
inline prefix:cs (str)
    str as rawstring

set-globals!
    ..
        do
            let prefix:cs
            let assert = aot-assert
            locals;
        (globals)
;
