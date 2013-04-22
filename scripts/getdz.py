#!/usr/bin/python
import re
import os
import subprocess

dir = '/home/gagan/thesis/newstockquotes'
rootdir='/home/gagan/thesis/newSub'
newdir = '/home/gagan/thesis/spaces'

list = []

def getIntforSymbol(symbol):
    s = symbol[1:-1]
    i = list.index(s)
    return(i)


def getdz(line):
    space = []

    symbol_l = 0
    symbol_h = 99
    
    high_l = 0
    high_h = 740

    low_l = 0
    low_h = 639

    volume_l = 0
    volume_h = 580453376

    s = re.findall("\[(.*?)\]", line)
    if not s:
        return
    for i in range(len(s)):
        p = s[i].split(',')
        if(p[0]=="symbol"):
            symbol_l = getIntforSymbol(p[2])
            symbol_h = getIntforSymbol(p[2])
            continue

        if(p[0]=="high"):
            if(p[1]=='>'):
                high_l = p[2]
            if(p[1]=='<'):
                high_h = p[2]
            continue
    
        if(p[0]=="low"):
            if(p[1]=='>'):
                low_l = p[2]
            if(p[1]=='<'):
                low_h = p[2]
            continue    
        
        if(p[0]=="volume"):
            if(p[1]=='>'):
                volume_l = p[2]
            if(p[1]=='<'):
                volume_h = p[2]
            continue
            
    space.append(symbol_l)
    space.append(symbol_h)
    space.append(high_l)
    space.append(high_h)
    space.append(low_l)
    space.append(low_h)
    space.append(volume_l)
    space.append(volume_h)
    return(space)

for subdir, dirs, files in os.walk(dir):
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
#            print(line)
            space = getdz(line)
            nline = str(count) + ":" + ' '.join(str(v) for v in space) + '\n'
            command = "java test/MultipleSubscriptionGenerator " +  ' '.join(str(v) for v in space)
            print(command)
            nline += str(subprocess.Popen(command, stdout=subprocess.PIPE, shell=True).stdout.read()) + '\n'
            f.write(nline)
            count=count+1

        f.close()    
