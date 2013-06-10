#!/usr/bin/python
import re
import os
import subprocess

list = []
dim = 3
def getdz(line):
    global dim

    space = []
    p = line.split(' ')
    
    for i in range(0, dim):
        space.append(p[i])            
        space.append(p[i])

    return(space)

f=open("zipf_evnts", 'r')
lines=f.readlines()
f.close()

f=open("dzForEvents_z", 'a')
count = 0

for line in lines:
#    print(line)
    space = getdz(line)
    nline = str(count) + ":" + ' '.join(str(v) for v in space) + '\n'
#    print(nline)
    command = "java test/MultipleSubscriptionGenerator " +  ' '.join(str(v) for v in space)
    print(command)
    nline += str(subprocess.Popen(command, stdout=subprocess.PIPE, shell=True).stdout.read()) + '\n'
    f.write(nline)
    count=count+1

f.close()    
