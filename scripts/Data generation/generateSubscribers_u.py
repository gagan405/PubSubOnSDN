#!/usr/bin/python
import sys
import re
import os
import random

def distributeSubscriptions(nSub, nSubscriptions):

    if(nSub == 0):
        return

    count = 0
    
    f=open("uniform_subs", 'r')
    lines=f.readlines()
    f.close()
            
    f=open("dzForSubs_u", 'r')
    dzlines=f.readlines()
    f.close()

    while(count < nSub):
        nLines = 0
        filename= "sub_" + str(count)
        f=open(filename, 'a')
        for line_idx in random.sample(range(0,len(lines)), nSubscriptions):
            f.write(lines[line_idx])
            f.write(dzlines[(2*line_idx)+1][2:-2] + '\n')
        count = count + 1
        f.close()    

if __name__ == "__main__":

    nSubscribers = int(sys.argv[1])
    nSubscriptionsPerSub = 5

    if(int(sys.argv[2]) != 0):
        nSubscriptionsPerSub = int(sys.argv[2])

    distributeSubscriptions(nSubscribers, nSubscriptionsPerSub)
