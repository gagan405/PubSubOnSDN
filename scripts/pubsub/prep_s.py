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

f1 = open(name + "_5", 'a')
f2 = open(name + "_10", 'a')
f3 = open(name + "_15", 'a')
f4 = open(name + "_23" , 'a')

temp1 = []
temp2 = []
temp3 = []
temp4 = []


for a in advt:
    if(a):
        if(len(a)> 5 ):
	    t = a[:5]
	else:
	    t = a
	temp1.append(t)
	
	if(len(a) > 10):
	    t2 = a[:10]
	else:
	    t2 = a
        temp2.append(t2)
	
	if(len(a) > 15):
            t3 = a[:15]
        else:
            t3 = a
        temp3.append(t3)
	
	if(len(a) > 23):
            t4 = a[:23]
        else:
            t4 = a
        temp4.append(t4)


s5 = removeUnnecessary(temp1)
s5 = list(set(s5))
s5.sort()


s10 = removeUnnecessary(temp2)
s10 = list(set(s10))
s10.sort()

s15 = removeUnnecessary(temp3)
s15 = list(set(s15))
s15.sort()

s23 = removeUnnecessary(temp4)
s23 = list(set(s23))
s23.sort()

for s in s5:
    if(s):
	f1.write(s + " ")

for s in s10:
    if(s):
        f2.write(s + " ")

for s in s15:
    if(s):
        f3.write(s + " ")

for s in s23:
    if(s):
        f4.write(s + " ")

f1.close()
f2.close()
f3.close()
f4.close()

