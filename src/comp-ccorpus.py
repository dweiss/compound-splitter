import sys,re

words = {}
for line in open('ccorpus.txt','r'):
    w = line.split()[0]
    words[w] = True

print("Read input.")

for line in open('combined.sorted','r'):
    w, r = line.split()
    if words.has_key(w):
        print w + "\t" + r
