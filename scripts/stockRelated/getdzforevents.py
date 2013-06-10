#!/usr/bin/python
import re
import os
import subprocess

rootdir='/home/gagan/thesis/newstockquotes'
newdir = '/home/gagan/thesis/eventdzs'

list = []

def getIntforSymbol(symbol):
    s = symbol[1:-1]
    i = list.index(s)
    return(i)


def getdz(line):
    space = []

    symbol = 0
    high = 0
    low = 0
    volume = 0

    s = re.findall("\[(.*?)\]", line)
    if not s:
        return
    for i in range(len(s)):
        p = s[i].split(',')
        if(p[0]=="symbol"):
            symbol = getIntforSymbol(p[1])
            continue

        if(p[0]=="high"):
            high = p[1]
            continue
    
        if(p[0]=="low"):
            low = p[1]
            continue    
        
        if(p[0]=="volume"):
            volume = p[1]
            continue
            
    space.append(symbol)
    space.append(symbol)
    space.append(high)
    space.append(high)
    space.append(low)
    space.append(low)
    space.append(volume)
    space.append(volume)
    return(space)

for subdir, dirs, files in os.walk(rootdir):
    for file in files:
        list.append(file)
    list.sort()


for subdir, dirs, files in os.walk(rootdir):
    for file in files:
        f=open(os.path.join(rootdir,file), 'r')
        lines=f.readlines()
        f.close()
        f=open(os.path.join(newdir,file), 'a')
        count = 0
        for line in lines:
            space = getdz(line)
#            nline = str(count) + ": " + ' '.join(str(v) for v in space) + '\n'
            command = "java test/MultipleSubscriptionGenerator " +  ' '.join(str(v) for v in space)
            nline = str(count) + ": " + str(subprocess.Popen(command, stdout=subprocess.PIPE, shell=True).stdout.read()) + '\n'
            f.write(nline)
            count=count+1

        f.close()    

filelist = [ f for f in os.listdir(newdir) if f.endswith('~') ]
for f in filelist:
    os.remove(os.path.join(newdir,f))
