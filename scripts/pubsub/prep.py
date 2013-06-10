import socket
import sys
import struct
import time
import re
   
def removeUnnecessary(subs):
    newsubs = []
    covered = False
    for s in subs:
        if(not s):
            continue
        covered = False
        s = s.strip('\r\n')
        for t in subs:
            if(not t):
                continue
            t = t.strip('\r\n')
            
            if(len(t) >= len(s)):
                continue
            else:
                if(s.find(t) == 0):
                    covered = True
                    break
        if(covered is not True):
            newsubs += [s]
    return(newsubs)
    

name = sys.argv[1] #subscription file name
adv_name = sys.argv[2]

f = open(name, 'r')
lines = f.readlines()
f.close()

idx = 0
subs = []
advt = []

for line in lines:    
    if(idx % 2 == 0):
        idx = idx + 1
        continue
    else:    
        subs = line.split(" ")
        subs = list(set(subs))
        subs.sort()
        try:
            subs.remove('\n')
        except ValueError as e:
            pass
#        s = removeUnnecessary(subs)
        advt += subs
        idx = idx + 1

print(advt)
advt = removeUnnecessary(advt)
advt = list(set(advt))
advt.sort()

print(advt)

f = open(adv_name, 'a')

for a in advt:
    f.write(a + " ")
f.close()

