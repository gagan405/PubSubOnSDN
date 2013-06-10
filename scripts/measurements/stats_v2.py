import sys
import re
import os

dim1_l = -1
dim1_u = -1

dim2_l = -1
dim2_u = -1


numberOfEvents = 0
avgDelay = 0.0
totalDelay = 0.0
attrFalsePositiveRate = 0.0
dzFalsePositiveRate = 0.0

def checkDz(line,  dzList):
    p = line.split(" ")
    d = p[0]
    
    for dz in dzList:
        dz = dz.strip('\r\n')
        if(d.find(dz) == 0):
            return(False)
        elif(dz.find(d) == 0):
            return(False)    
        
    return(True)

def checkAttr(line):
    global dim1_l, dim1_u, dim2_l, dim2_u
    p = line.split(" ")
    
    d1_l = int(p[3])
    d2_l = int(p[5])    
    
    if((d1_l >= dim1_l) and (d1_l <= dim1_u) and (d2_l >= dim2_l) and (d2_l <= dim2_u)):
        return(False)
    else:    
        return(True)

def getDelay(line):
    global totalDelay
    p = line.split(" ")
    sendingTime = int(p[2][len(p[2])-6:])
    receiveTime = int(p[9][len(p[9])-6:])
    if(receiveTime < sendingTime):
        receiveTime =  1000000 + receiveTime
    delay = receiveTime - sendingTime
    totalDelay += delay
    return(delay)

def getDz(line):
    dz = []
    dz += line.split(" ")
    return(dz)

def getrange(line):
    global dim1_l, dim1_u, dim2_l, dim2_u
    s = line.split(" ")

    if not s:
        return

    dim1_l = int(s[0])
    dim1_u = int(s[1])
    dim2_l = int(s[2])
    dim2_u = int(s[3])

    return;

#name = sys.argv[1] #received event file
subFile = sys.argv[1] # subscription file

afcount = 0
dfcount = 0

rlist = [ f for f in os.listdir(os.getcwd()) if f.startswith("ReceivedEvent") ]
    
f = open(subFile, 'r')
sub = f.readlines()
f.close()

for name in rlist:
        
    delay = "event_delays_" + name
    dzFalsePositive = "dz_false_positive_" + name
    attrFalsePositive = "attr_false_positive_" + name
    
    f = open(name, 'r')
    lines = f.readlines()
    f.close()
    
    #dzList = getDz(sub[1])
    #getrange(sub[0])
    
    d = open(delay, 'a')
    df = open(dzFalsePositive, 'a')
    af = open(attrFalsePositive,  'a')
    idx = 0
   
    while(True):
        if(len(lines) == 0):
            break
        line = lines[idx].replace('\n', '') + lines[idx+1].replace('\n', '')
        numberOfEvents += 1
        aFalsePositive = True
        dFalsePositive = True
    #    print(line)
        d.write(str(getDelay(line)) + '\n')
    
        sub_idx = 0
        while(sub_idx < len(sub)-1):
            dzList = getDz(sub[sub_idx+1])
            getrange(sub[sub_idx])
            aFalsePositive = aFalsePositive & checkAttr(line)
            if(dFalsePositive):
                dFalsePositive = dFalsePositive & checkDz(line, dzList)
            if(not aFalsePositive):
                break
            sub_idx += 2
            
        if(aFalsePositive):
            afcount += 1
            af.write(line + '\n')
        if(dFalsePositive):
            dfcount += 1
            df.write(line + '\n')
    #    print(line)
        idx +=2
        if(idx == len(lines)):
            break
            
    
    d.close()
    df.close()
    af.close()

filelist = [ f for f in os.listdir(os.getcwd()) if f.endswith('~') ]
for f in filelist:
    os.remove(os.path.join(os.getcwd(),f))
print("Total number of events received : ", numberOfEvents)
print("Average Delay : ",  totalDelay/numberOfEvents)
print("Average attribute false +ve : ",  afcount/numberOfEvents)
print("Average dz false +ve : ",  dfcount/numberOfEvents)
