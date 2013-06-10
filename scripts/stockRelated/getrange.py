#!/usr/bin/python
import re
import os

rootdir='/home/gagan/thesis/newstockquotes'

high_l = -1
high_u = -1

low_l = -1
low_u = -1

volume_l = -1
volume_u = -1

def getrange(line):
    global high_l, high_u, low_l, low_u, volume_l, volume_u
    s = re.findall("\[(.*?)\]", line)
    if not s:
        return
    p = s[2].split(',')
    if(high_l == -1):
        high_l = int(p[1])
    if(high_u == -1):
        high_u = int(p[1])
    elif(high_l > int(p[1])):
        high_l = int(p[1])
    elif(high_u < int(p[1])):
        high_u = int(p[1])
#    print(p)

    p = s[3].split(',')
    if(low_l == -1):
        low_l = int(p[1])
    if(low_u == -1):
        low_u = int(p[1])
    elif(low_l > int(p[1])):
        low_l = int(p[1])
    elif(low_u < int(p[1])):
        low_u = int(p[1])
#    print(p)

    p = s[5].split(',')
    if(volume_l == -1):
        volume_l = int(p[1])
    if(volume_u == -1):
        volume_u = int(p[1])
    elif(volume_l > int(p[1])):
        volume_l = int(p[1])
    elif(volume_u < int(p[1])):
        volume_u = int(p[1])
#    print(p)    

for subdir, dirs, files in os.walk(rootdir):
    for file in files:
        f=open(os.path.join(rootdir,file), 'r')
        lines=f.readlines()
        f.close()
        for line in lines:
#            print(line)
            newline=getrange(line)

print("Range for 'high' : ", high_l, high_u)
print("Range for 'low' : ", low_l, low_u)
print("Range for 'volume' : ", volume_l, volume_u)
