#!/usr/bin/python
import re
import os

rootdir='/home/gagan/thesis/sub'
newdir='/home/gagan/thesis/newSub'

def modify(line):
    s = re.findall("\[(.*?)\]", line)
    if not s:
        return
    del s[0]
    for i in range(0,len(s)):
        p = s[i].split(',')
        if(p[0]=="symbol"):
            continue
        if(p[0]=="volume"):
            continue
        p[2] = str(int(float(p[2])+0.5))
        s[i] = ','.join(p)
    newLine = '[' + "],[".join(s) + ']\n'
    return newLine

for subdir, dirs, files in os.walk(rootdir):
    for file in files:
        f=open(os.path.join(rootdir,file), 'r')
        lines=f.readlines()
        f.close()
        f=open(os.path.join(newdir,file), 'a')
        for line in lines:
            newline=modify(line)
            f.write(newline)
        f.close()

filelist = [ f for f in os.listdir(newdir) if f.endswith('~') ]
for f in filelist:
    os.remove(os.path.join(newdir,f))
