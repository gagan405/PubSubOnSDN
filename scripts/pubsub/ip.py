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

def ipToBinaryString(ip):
    return (''.join([ bin(int(x))[2:].rjust(8,'0') for x in ip.split('.')]))


def getNextBytes(s):
    if((s == "") or (s == " ") or (s == "\n")):
        return
    length = len(s)
    if(length > 40):
        print("dz expression too long")
        return
        
    temp = "00000100" + strToBinary(length)
    msg = struct.pack('!H', int(temp, 2))
   
    s = "0" * (8 + (40 - length)) + s
    
    msg += struct.pack('!H', int(s[0:16], 2))
    msg += struct.pack('!H', int(s[16:32], 2))
    msg += struct.pack('!H', int(s[32:48], 2))
    
    return(msg)

msg = b""
msg = getNextBytes(ipToBinaryString(sys.argv[1]))
if(msg is not None):
    sock = socket.socket(socket.AF_INET, socket.SOCK_DGRAM, socket.IPPROTO_UDP)
    sock.setsockopt(socket.IPPROTO_IP, socket.IP_MULTICAST_TTL, 2)
    sock.sendto(msg, (MCAST_GRP, MCAST_PORT))


