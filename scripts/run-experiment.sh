if [ $# -ne 1 ]; then
    echo "Usage: $0 <experiment name>"
    exit 1
fi

nthreads=
payload=
server=
readsper=
writesper=

cd analysis
python3 executor.py run $1
