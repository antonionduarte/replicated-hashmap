#!/usr/bin/python

import sys

loglines = []
for fname in sys.argv[1:]:
    with open(fname, "r") as f:
        loglines += f.readlines()
times = [int(l.split()[0]) for l in loglines]
mintime = min(times)

for l in loglines:
    p = l.split()
    t = int(p[0])
    t = t - mintime
    t = float(t) / 1_000_000_000
    print(f"{t:0.6f} {' '.join(p[1:])}")
