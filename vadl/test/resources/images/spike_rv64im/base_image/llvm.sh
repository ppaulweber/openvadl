set -e
set -x
/src/llvm-final/build/bin/clang --target=${TARGET} -S -c /src/inputs/$INPUT -o /output/$INPUT.s && diff -y --color --ignore-all-space /output/$INPUT.s /assertions/$INPUT.s