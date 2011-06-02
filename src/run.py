import sys,re

words = {}
for line in open('combined.sorted','r'):
    w, rank = line.split()
    if int(rank) < 10:
        break
    if len(w) > 1:
        words[w] = int(rank)

print("Read input.")

stack = []
def decompound(s):
    if len(s) == 0:
        print "  " + str(stack)
        return

    for i in range(1, len(s) + 1):
        t = s[0:i]
        if t in words:
            stack.append([t, words[t]])
            decompound(s[i:])
            stack.pop()

for line in sys.stdin:
    line = line.strip()
    print line
    decompound(line)
