import socket
import sys
import struct
import time
import re

MCAST_GRP = '225.37.0.0'
MCAST_PORT = 5007
ips = []

def strToBinary(n):
    s = bin(n)
    s = s[2:]
    if(len(s) < 8):
        s = "0" * (8 - len(s)) + s
    return(s)

def getNextBytes(s):
    if((s == "") or (s == " ") or (s == "\n")):
        return
    length = len(s)
    if(length > 40):
        print("dz expression too long")
        return
        
    temp = "00000001" + strToBinary(length)
    msg = struct.pack('!H', int(temp, 2))
   
    s = "0" * (8 + (40 - length)) + s
    
    msg += struct.pack('!H', int(s[0:16], 2))
    msg += struct.pack('!H', int(s[16:32], 2))
    msg += struct.pack('!H', int(s[32:48], 2))
    
    return(msg)
    
def removeUnnecessary(subs):
    newsubs = []
    covered = False
    for s in subs:
        covered = False
        for t in subs:
            if(len(t) >= len(s)):
                continue
            else:
                if(s.find(t) == 0):
                    covered = True
                    break
        if(covered is not True):
            newsubs += [s]
    return(newsubs)
    

name = sys.argv[1]

f = open(name, 'r')
lines = f.readlines()
f.close()

idx = 0
subs = []
advt = []
symbol = ""

for line in lines:    
    if(idx % 2 == 0):
        idx = idx + 1
        s = re.findall("\[(.*?)\]", line)
        if(not s):
            break
        p = s[0].split(',')
        if(p[0]=="symbol"):
            symbol = p[2][1:-1]
            continue
    else:    
        subs = line.split(" ")
        subs = list(set(subs))
        subs.sort()
        try:
            subs.remove('\n')
        except ValueError as e:
            pass
        s = removeUnnecessary(subs)
        advt.append((symbol, subs))
        idx = idx + 1


advertisementSpace = []
for adv in advt:
    advertisementSpace += adv[1]
    advertisementSpace = list(set(advertisementSpace))
    advertisementSpace.sort()
    
for sub in advertisementSpace:
    msg = b""
    msg = getNextBytes(sub)
    if(msg is not None):
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
        sock.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_TTL, 2)
        sock.sendto(msg, (MCAST_GRP, MCAST_PORT))
    time.sleep(1.0/10.0) #sleep 100 ms

