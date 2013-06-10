import socket
import sys
import struct
import time

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
        
    temp = "00000011" + strToBinary(length)
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
for line in lines:
    if(idx % 2 == 0):
        idx = idx + 1
        continue
    subs += line.split(" ")
    idx = idx + 1

subs = list(set(subs))
subs.sort()
subs.remove('\n')
print(subs)
s = removeUnnecessary(subs)
print(s)

for sub in s:
    msg = b""
    msg = getNextBytes(sub)
    if(msg is not None):
        sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
        sock.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_TTL, 2)
        sock.sendto(msg, (MCAST_GRP, MCAST_PORT))
    time.sleep(1.0/2.0) #sleep 500 ms

