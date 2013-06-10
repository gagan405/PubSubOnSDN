import sys
import re
import os
import math

filelist = [ f for f in os.listdir(os.getcwd()) if f.startswith("100") and not f.endswith("_")]
avgdelay = 0.0
count = 0
globalNum = 0
globalDel = 0
d = []

def average(s): 
    return sum(s) * 1.0 / len(s)

for name in filelist:
    
    avgDelay = 0.0
    totalDelay = 0

    f = open(name, 'r')
    lines = f.readlines()
    f.close()
    numberOfEvents = 0	
    for line in lines:
#        line = line.replace('\n', '')
        if(line.startswith("999")):
            continue
        line = line.strip('\n\r')
        numberOfEvents += 1
        d.append(int(line))
        totalDelay += int(line)
#    print("Delay for ", name, " : ",  totalDelay/numberOfEvents)
    globalNum += numberOfEvents
    globalDel += totalDelay

print()
print("Average delay : " , globalDel/globalNum)    
avg = average(d)
[*variance] = map(lambda x: (x - avg)**2, d)
#print(variance)
sd = math.sqrt(average(variance))
print("SD : ", sd)
print(globalNum)
