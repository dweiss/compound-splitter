import sys,re

words = {}
for line in open('jwordsplitter.dict','r'):
    w = line.split()[0]
    words[w] = True

for line in open('combined.sorted','r'):
    w, r = line.split()
    if not words.has_key(w):
        print w + "\t" + r
