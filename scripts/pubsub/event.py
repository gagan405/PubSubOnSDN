import socket
import sys
import struct
import threading
import re
import time
from datetime import datetime

MCAST_PORT = 5007
ips = []

def strToBinary(n):
    s = bin(n)
    s = s[2:]
    if(len(s) < 8):
        s = "0" * (8 - len(s)) + s
    return(s)

    
def getIPfromDZ(s):
    if((ord(s[len(s)-1]) != 48) and (ord(s[len(s)-1]) != 49)):
        s = s[:-1]
    
    if(s == "*"):
        return('225.128.0.0')

    if(len(s) > 23):
        st = s[:23]
                
    if(len(s) < 23):
        st = s + "0" * (23 - len(s))
    if(len(s) == 23):
        st = s
    
    st = "111000011" + st #225.128 <-- fixed prefix
    addr_long = int(st,2)
    hex(addr_long)
    return(socket.inet_ntoa(struct.pack(">L", addr_long)))
    
def sendEvent(fileName, dzList):
    f = open(fileName, 'r')
    lines = f.readlines()
    f.close()
    idx = 0
    count = 0
    msg = ""
#    print("I am here")
    for line in lines:
        if(idx%2 == 0):
            segment = line.split(":")
            msg += segment[1] + " "
            idx +=1
#            print("1st print ", msg)
            continue
        else:
            idx += 1    
            dz = line[2:-3]
        for d in dzList:
	    if(not d):
		continue
#	    print(d)
#	    print(dz)
#	    print("2nd print ", msg)
            if(dz.find(d) == 0):
		msg.replace('\n','')
		msg.strip('\r\n')
#		print("3rd print ", msg)
                msg = dz + " " + str(datetime.now()) + " " + msg
                sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
                sock.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_TTL, 2)
#                print(msg + "sent to " + getIPfromDZ(dz))
#		for i in range(0,10):
	        sock.sendto(msg, (getIPfromDZ(dz), MCAST_PORT))
		count += 1
#                time.sleep(1.0/10.0)
		break
	msg = ""
	print("Number of events sent : " + str(count))

name = "advertisements" #subscription file
filename = sys.argv[1] #event file

f = open(name, 'r')
lines = f.readlines()
f.close()

idx = 0
subs = []
advt = []

for line in lines:    
    subs = line.split(" ")
    subs = list(set(subs))
    subs.sort()
    try:
        subs.remove('\n')
    except ValueError as e:
        pass
#        s = removeUnnecessary(subs)
    advt += subs

print(advt)
sendEvent(filename,advt)

