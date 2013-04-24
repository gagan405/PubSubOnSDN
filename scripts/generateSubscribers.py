#!/usr/bin/python
import sys
import re
import os
import random

rootdir='/home/gagan/thesis/newSub'
newdir='/home/gagan/thesis/Subscribers'
dzDir='/home/gagan/thesis/spaces'

def distributeSubscriptions(nSub, nSubscriptions):
    global rootdir, newdir, dzDir

    if(nSub == 0):
        return

    for subdir, dirs, files in os.walk(rootdir):
        count = 0
        for file in files:
            f=open(os.path.join(rootdir,file), 'r')
            lines=f.readlines()
            f.close()
            
            f=open(os.path.join(dzDir,file), 'r')
            dzlines=f.readlines()
            f.close()

            while(count < nSub):
                nLines = 0
                filename= "sub_" + str(count)
                f=open(os.path.join(newdir,filename), 'a')

                for line_idx in random.sample(range(0,len(lines)), nSubscriptions):
                    f.write(lines[line_idx])
                    f.write(dzlines[(2*line_idx)+1][2:-2] + '\n')
                f.close()
                count = count + 1
            
    filelist = [ f for f in os.listdir(newdir) if f.endswith('~') ]
    for f in filelist:
        os.remove(os.path.join(newdir,f))

if __name__ == "__main__":

    nSubscribers = int(sys.argv[1])
    nSubscriptionsPerSub = 5

    if(int(sys.argv[2]) != 0):
        nSubscriptionsPerSub = int(sys.argv[2])

    distributeSubscriptions(nSubscribers, nSubscriptionsPerSub)
