#!/usr/bin/python
import re
import os

rootdir='/home/gagan/stockquotes'
newdir='/home/gagan/newstockquotes'

def modify(line):
    s = re.findall("\[(.*?)\]", line)
    if not s:
        return
    del s[0]
    del s[6:12]
    for i in range(1,5):
        p = s[i].split(',')
        p[1] = str(int(float(p[1])+0.5))
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
